import { ToolRegistry } from '../src/registry/tool';
import { ToolDispatcher } from '../src/tool/executor';
import { ToolCategory, ToolVisibility } from '../src/types/tool';
import { MessageRouter, MessageDispatcher } from '../src/protocol';
import { StructuredLogger } from '../src/logging/logger';
import { ToolValidationError, ToolNotFoundError } from '../src/errors';

describe('MCP Tool Framework Foundation', () => {
  let registry: ToolRegistry;
  let toolDispatcher: ToolDispatcher;

  const defaultMeta = {
    categories: [ToolCategory.SYSTEM],
    visibility: ToolVisibility.PUBLIC,
  };

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'test-req',
    sessionId: 'test-session',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger: new StructuredLogger('test', 'error'),
    params,
    metadata: new Map<string, unknown>(),
  });

  beforeEach(() => {
    registry = new ToolRegistry();
    toolDispatcher = new ToolDispatcher(registry);
  });

  describe('Registry Operations', () => {
    it('should register, check, and retrieve tools', () => {
      const toolDef = {
        name: 'test-tool',
        description: 'useful tool',
        inputSchema: { type: 'object' as const, properties: {} },
        handler: async () => ({ content: [{ type: 'text' as const, text: 'hi' }] }),
      };

      registry.registerTool(toolDef, defaultMeta);
      expect(registry.hasTool('test-tool')).toBe(true);
      expect(registry.getTool('test-tool')).toBe(toolDef);
      expect(registry.getMetadata('test-tool')).toBe(defaultMeta);
      expect(registry.listTools()).toHaveLength(1);
    });

    it('should prevent duplicate tool registration', () => {
      const toolDef = {
        name: 'dup',
        description: 'useful tool',
        inputSchema: { type: 'object' as const },
        handler: async () => ({ content: [] }),
      };

      registry.registerTool(toolDef, defaultMeta);
      expect(() => registry.registerTool(toolDef, defaultMeta)).toThrow();
    });

    it('should unregister tools', () => {
      const toolDef = {
        name: 'todelete',
        description: 'to delete',
        inputSchema: { type: 'object' as const },
        handler: async () => ({ content: [] }),
      };

      registry.registerTool(toolDef, defaultMeta);
      expect(registry.hasTool('todelete')).toBe(true);
      registry.unregisterTool('todelete');
      expect(registry.hasTool('todelete')).toBe(false);
    });
  });

  describe('Execution Pipeline and Validation', () => {
    it('should execute tool and return successful result', async () => {
      registry.registerTool(
        {
          name: 'add',
          description: 'adds two numbers',
          inputSchema: {
            type: 'object',
            properties: {
              a: { type: 'number' },
              b: { type: 'number' },
            },
            required: ['a', 'b'],
          },
          handler: async (params) => {
            const sum = (params.a as number) + (params.b as number);
            return { content: [{ type: 'text', text: `Sum is ${sum}` }] };
          },
        },
        defaultMeta
      );

      const result = await toolDispatcher.dispatchCall('add', { a: 5, b: 15 }, mockContext({ a: 5, b: 15 }));
      expect(result.content[0].text).toBe('Sum is 20');
      expect(result.isError).toBeUndefined();
    });

    it('should validate parameter presence and types', async () => {
      registry.registerTool(
        {
          name: 'params-check',
          description: 'checks params',
          inputSchema: {
            type: 'object',
            properties: {
              tags: { type: 'array' },
              name: { type: 'string' },
            },
            required: ['name'],
          },
          handler: async () => ({ content: [] }),
        },
        defaultMeta
      );

      // Missing required parameter
      await expect(toolDispatcher.dispatchCall('params-check', {}, mockContext({}))).rejects.toThrow(ToolValidationError);
      
      // Type mismatch
      await expect(toolDispatcher.dispatchCall('params-check', { name: 123 }, mockContext({ name: 123 }))).rejects.toThrow(ToolValidationError);
      await expect(toolDispatcher.dispatchCall('params-check', { name: 'john', tags: 'not-an-array' }, mockContext({ name: 'john', tags: 'not-an-array' }))).rejects.toThrow(ToolValidationError);
    });

    it('should throw method not found on unknown tool calls', async () => {
      await expect(toolDispatcher.dispatchCall('non-existent', {}, mockContext({}))).rejects.toThrow(ToolNotFoundError);
    });
  });

  describe('Placeholder Tools Verification', () => {
    // Helper to register mock placeholders
    const registerMockPlaceholders = (reg: ToolRegistry) => {
      const placeholders = ['status', 'doctor', 'projects', 'search', 'handoff'];
      for (const name of placeholders) {
        reg.registerTool(
          {
            name,
            description: `Placeholder for ${name} tool`,
            inputSchema: { type: 'object', properties: {} },
            handler: async () => ({
              content: [{ type: 'text', text: `This tool is not implemented yet. (Mock ${name})` }],
            }),
          },
          defaultMeta
        );
      }
    };

    it('should respond with static placeholder text on placeholder calls', async () => {
      registerMockPlaceholders(registry);

      const statusRes = await toolDispatcher.dispatchCall('status', {}, mockContext({}));
      expect(statusRes.content[0].text).toBe('This tool is not implemented yet. (Mock status)');

      const searchRes = await toolDispatcher.dispatchCall('search', {}, mockContext({}));
      expect(searchRes.content[0].text).toBe('This tool is not implemented yet. (Mock search)');
    });
  });

  describe('Dispatcher protocol routing', () => {
    let messageRouter: MessageRouter;
    let dispatcher: MessageDispatcher;

    beforeEach(() => {
      messageRouter = new MessageRouter();
      dispatcher = new MessageDispatcher(
        messageRouter,
        { name: 'test', version: '1.0.0' },
        new StructuredLogger('test', 'error'),
        registry
      );
    });

    it('should handle tools/list and tools/call protocol requests', async () => {
      registry.registerTool(
        {
          name: 'say-hello',
          description: 'greets user',
          inputSchema: { type: 'object', properties: {} },
          handler: async () => ({ content: [{ type: 'text', text: 'Hello!' }] }),
        },
        defaultMeta
      );

      // Perform Handshake first
      await dispatcher.dispatch('{"jsonrpc":"2.0","id":"init-1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test-client","version":"1.0.0"},"capabilities":{}}}');
      await dispatcher.dispatch('{"jsonrpc":"2.0","method":"initialized"}');

      // Call tools/list
      const listResRaw = await dispatcher.dispatch('{"jsonrpc":"2.0","id":"call-list","method":"tools/list"}');
      expect(listResRaw).toBeDefined();
      const listRes = JSON.parse(listResRaw!);
      expect(listRes.result.tools).toHaveLength(1);
      expect(listRes.result.tools[0].name).toBe('say-hello');

      // Call tools/call
      const callResRaw = await dispatcher.dispatch(
        JSON.stringify({
          jsonrpc: '2.0',
          id: 'call-tool',
          method: 'tools/call',
          params: { name: 'say-hello', arguments: {} },
        })
      );
      expect(callResRaw).toBeDefined();
      const callRes = JSON.parse(callResRaw!);
      expect(callRes.result.content[0].text).toBe('Hello!');
    });
  });
});
