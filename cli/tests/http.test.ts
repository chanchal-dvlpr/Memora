import { RequestBuilder } from '../src/http/builder';
import { ResponseHandler, FetchResponse } from '../src/http/handler';
import { mapHttpError, mapNetworkError } from '../src/http/mapper';
import { CliHttpClient } from '../src/http/client';
import { ConfigService } from '../src/config/service';
import { ConfigLoader } from '../src/config/loader';
import { DefaultConfigSource } from '../src/config/source';
import { ExecutionContext } from '../src/models/context';
import { CommandError, ValidationError, InternalError } from '../src/errors/errors';

describe('HTTP Client Pipeline Subsystem', () => {
  let service: ConfigService;
  let client: CliHttpClient;
  const mockCtx = {
    workingDir: '/workspace',
    requestId: 'req-123',
    correlationId: 'corr-456',
    env: {},
  } as unknown as ExecutionContext;

  beforeEach(() => {
    service = new ConfigService(new ConfigLoader([new DefaultConfigSource()]));
    client = new CliHttpClient(service);
  });

  describe('RequestBuilder Immutability', () => {
    it('should create immutable chain builder configurations', () => {
      const b1 = RequestBuilder.get('/api/test');
      const b2 = b1.query({ foo: 'bar' });
      const b3 = b2.header('x-custom', 'val').timeout(5000);

      expect(b1.getQueryParams()).toEqual({});
      expect(b2.getQueryParams()).toEqual({ foo: 'bar' });
      expect(b2.getHeaders()).toEqual({});
      expect(b3.getHeaders()).toEqual({ 'x-custom': 'val' });
      expect(b3.getTimeoutMs()).toBe(5000);
    });
  });

  describe('Error Mapper Rules', () => {
    it('should map standard status code errors to correct CLI exceptions', () => {
      expect(mapHttpError(400, 'GET', '/url')).toBeInstanceOf(CommandError);
      expect(mapHttpError(422, 'POST', '/url')).toBeInstanceOf(ValidationError);
      expect(mapHttpError(500, 'GET', '/url')).toBeInstanceOf(InternalError);
    });

    it('should map network anomalies to CLI exceptions', () => {
      const dnsError = new Error('getaddrinfo ENOTFOUND server');
      const connError = new Error('connect ECONNREFUSED 127.0.0.1');

      expect(mapNetworkError(dnsError, '/url').message).toContain('DNS lookup failed');
      expect(mapNetworkError(connError, '/url').message).toContain('Connection refused');
    });
  });

  describe('Response Handler Parser', () => {
    it('should parse application/json successfully', async () => {
      const mockRes = {
        status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => JSON.stringify({ success: true }),
      };

      const result = await ResponseHandler.handle(
        mockRes as unknown as FetchResponse,
        'GET',
        '/url',
      );
      expect(result.data).toEqual({ success: true });
    });

    it('should fail on malformed JSON payload', async () => {
      const mockRes = {
        status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => '{ bad json }',
      };

      await expect(
        ResponseHandler.handle(mockRes as unknown as FetchResponse, 'GET', '/url'),
      ).rejects.toThrow();
    });
  });

  describe('CliHttpClient Execution', () => {
    it('should execute fetch successfully', async () => {
      await service.load();

      const mockFetchResponse = {
        status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => JSON.stringify({ status: 'UP' }),
      };

      const fetchSpy = jest
        .spyOn(global, 'fetch')
        .mockResolvedValue(mockFetchResponse as unknown as Response);

      const builder = RequestBuilder.get('/health');
      const res = await client.execute<{ status: string }>(builder, mockCtx);

      expect(fetchSpy).toHaveBeenCalled();
      expect(res.data.status).toBe('UP');

      fetchSpy.mockRestore();
    });

    it('should catch timeouts and abort request', async () => {
      await service.load();

      const abortError = new Error('The operation was aborted.');
      abortError.name = 'AbortError';

      const fetchSpy = jest.spyOn(global, 'fetch').mockRejectedValue(abortError);

      const builder = RequestBuilder.get('/slow').timeout(10);
      await expect(client.execute(builder, mockCtx)).rejects.toThrow('timed out after 10ms');

      fetchSpy.mockRestore();
    });
  });
});
