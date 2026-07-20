import {
  MessageParser,
  MessageValidator,
  MessageSerializer,
  MessageRouter,
  MessageDispatcher,
} from '../src/protocol';
import {
  JsonRpcParseError,
  JsonRpcInvalidRequestError,
  JsonRpcMethodNotFoundError,
} from '../src/errors';
import { StructuredLogger } from '../src/logging/logger';
import { ToolRegistry } from '../src/registry/tool';

describe('MCP Core Protocol Pipeline', () => {
  describe('MessageParser', () => {
    it('should parse valid JSON', () => {
      const payload = '{"jsonrpc":"2.0","id":1,"method":"ping"}';
      const parsed = MessageParser.parse(payload);
      expect(parsed).toEqual({ jsonrpc: '2.0', id: 1, method: 'ping' });
    });

    it('should throw JsonRpcParseError on syntax error', () => {
      const payload = '{"jsonrpc":"2.0",id:1';
      expect(() => MessageParser.parse(payload)).toThrow(JsonRpcParseError);
    });

    it('should throw JsonRpcParseError on empty string', () => {
      expect(() => MessageParser.parse('')).toThrow(JsonRpcParseError);
    });
  });

  describe('MessageValidator', () => {
    it('should validate standard request objects', () => {
      const valid = { jsonrpc: '2.0', id: 'req-1', method: 'someMethod', params: {} };
      expect(() => MessageValidator.validate(valid)).not.toThrow();
    });

    it('should validate notification objects', () => {
      const valid = { jsonrpc: '2.0', method: 'notify', params: { data: 'update' } };
      expect(() => MessageValidator.validate(valid)).not.toThrow();
    });

    it('should throw JsonRpcInvalidRequestError if version is not 2.0', () => {
      const invalid = { jsonrpc: '1.0', id: 1, method: 'ping' };
      expect(() => MessageValidator.validate(invalid)).toThrow(JsonRpcInvalidRequestError);
    });

    it('should throw JsonRpcInvalidRequestError if method is not a string', () => {
      const invalid = { jsonrpc: '2.0', id: 1, method: 123 };
      expect(() => MessageValidator.validate(invalid)).toThrow(JsonRpcInvalidRequestError);
    });

    it('should throw JsonRpcInvalidRequestError if message is not an object', () => {
      expect(() => MessageValidator.validate('not-an-object')).toThrow(JsonRpcInvalidRequestError);
      expect(() => MessageValidator.validate(null)).toThrow(JsonRpcInvalidRequestError);
      expect(() => MessageValidator.validate([1, 2])).toThrow(JsonRpcInvalidRequestError);
    });
  });

  describe('MessageSerializer', () => {
    it('should serialize objects to strings', () => {
      const msg = { jsonrpc: '2.0' as const, id: 1, result: 'ok' };
      const serialized = MessageSerializer.serialize(msg);
      expect(serialized).toBe('{"jsonrpc":"2.0","id":1,"result":"ok"}');
    });

    it('should build success responses', () => {
      const response = MessageSerializer.success('req-2', { value: 42 });
      expect(response).toEqual({
        jsonrpc: '2.0',
        id: 'req-2',
        result: { value: 42 },
      });
    });

    it('should build error responses', () => {
      const response = MessageSerializer.error('req-3', -32601, 'Method not found');
      expect(response).toEqual({
        jsonrpc: '2.0',
        id: 'req-3',
        error: {
          code: -32601,
          message: 'Method not found',
        },
      });
    });
  });

  describe('MessageRouter', () => {
    let router: MessageRouter;

    beforeEach(() => {
      router = new MessageRouter();
    });

    it('should register and resolve method handlers', async () => {
      const handlerSpy = jest.fn().mockResolvedValue('success');
      router.register('test', handlerSpy);

      const result = await router.route('test', { key: 'val' });
      expect(handlerSpy).toHaveBeenCalledWith({ key: 'val' });
      expect(result).toBe('success');
    });

    it('should throw JsonRpcMethodNotFoundError if method is not registered', async () => {
      await expect(router.route('missing')).rejects.toThrow(JsonRpcMethodNotFoundError);
    });

    it('should prevent duplicate registration', () => {
      router.register('dup', async () => {});
      expect(() => router.register('dup', async () => {})).toThrow();
    });
  });

  describe('MessageDispatcher', () => {
    let router: MessageRouter;
    let dispatcher: MessageDispatcher;

    beforeEach(() => {
      router = new MessageRouter();
      dispatcher = new MessageDispatcher(
        router,
        { name: 'test', version: '1.0.0' },
        new StructuredLogger('test', 'error'),
        new ToolRegistry()
      );
    });

    it('should parse, validate, route, and serialize requests after handshake', async () => {
      router.register('sum', async (params) => {
        const p = params as { a: number; b: number };
        return p.a + p.b;
      });

      // Execute handshake
      await dispatcher.dispatch('{"jsonrpc":"2.0","id":"init-1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test-client","version":"1.0.0"},"capabilities":{}}}');
      await dispatcher.dispatch('{"jsonrpc":"2.0","method":"initialized"}');

      const payload = '{"jsonrpc":"2.0","id":"c-1","method":"sum","params":{"a":5,"b":10}}';
      const response = await dispatcher.dispatch(payload);

      expect(response).toBe('{"jsonrpc":"2.0","id":"c-1","result":15}');
    });

    it('should return Parse Error on syntax violations', async () => {
      const payload = '{"jsonrpc":"2.0",id:1';
      const response = await dispatcher.dispatch(payload);
      
      const parsed = JSON.parse(response!);
      expect(parsed.error.code).toBe(-32700);
      expect(parsed.id).toBeNull();
    });

    it('should return Method Not Found error response on missing method', async () => {
      // Execute handshake
      await dispatcher.dispatch('{"jsonrpc":"2.0","id":"init-1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test-client","version":"1.0.0"},"capabilities":{}}}');
      await dispatcher.dispatch('{"jsonrpc":"2.0","method":"initialized"}');

      const payload = '{"jsonrpc":"2.0","id":"c-2","method":"unknown"}';
      const response = await dispatcher.dispatch(payload);

      const parsed = JSON.parse(response!);
      expect(parsed.error.code).toBe(-32601);
      expect(parsed.id).toBe('c-2');
    });

    it('should return null and not throw error on notification failures', async () => {
      // Execute handshake
      await dispatcher.dispatch('{"jsonrpc":"2.0","id":"init-1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test-client","version":"1.0.0"},"capabilities":{}}}');
      await dispatcher.dispatch('{"jsonrpc":"2.0","method":"initialized"}');

      const payload = '{"jsonrpc":"2.0","method":"notify-fail"}';
      const response = await dispatcher.dispatch(payload);
      expect(response).toBeNull();
    });
  });
});
