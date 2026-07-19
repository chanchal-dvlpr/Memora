import { diagnosticsApplicationService } from '../src/services/diagnostics';
import { HealthResponse } from '../src/api/health';
import { ExecutionContext } from '../src/models/context';
import { HttpTransport } from '../src/http/transport';
import { httpClientService } from '../src/http/clientService';
import { configService } from '../src/config/service';
import { run } from '../src/cli';
import { Console } from 'console';

interface ServiceWithPipeline {
  pipeline: {
    transport: HttpTransport;
  };
}

describe('Diagnostics Management Subsystem', () => {
  const mockCtx = {
    workingDir: process.cwd(),
    requestId: 'req-123',
    correlationId: 'corr-123',
    env: {},
    logger: {
      trace: () => {},
      debug: () => {},
      info: () => {},
      warn: () => {},
      error: () => {},
    },
  } as unknown as ExecutionContext;

  let originalLog: typeof console.log;
  let logSpy: jest.SpyInstance;

  beforeAll(async () => {
    originalLog = new Console(process.stdout).log;
    await configService.load();
  });

  afterAll(() => {
    console.log = originalLog;
  });

  beforeEach(() => {
    console.log = originalLog;
    logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    logSpy.mockRestore();
  });

  describe('DiagnosticCheck immutability & service layer mapping', () => {
    it('should construct immutable DiagnosticCheck objects', async () => {
      const result = await diagnosticsApplicationService.runConfigChecks(mockCtx);
      expect(result.checks.length).toBe(1);
      const check = result.checks[0];

      expect(() => {
        (check as { status: string }).status = 'mutated';
      }).toThrow();
    });

    it('should return health check PASSED when backend is UP', async () => {
      const mockResp: HealthResponse = { status: 'UP', version: '1.0.0' };
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const result = await diagnosticsApplicationService.runHealthChecks(mockCtx);
      expect(result.allPassed).toBe(true);
      expect(result.checks[0].status).toBe('PASSED');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should return health check FAILED when backend is unreachable', async () => {
      const mockTransport: HttpTransport = {
        send: async () => {
          throw new Error('Socket timeout');
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const result = await diagnosticsApplicationService.runHealthChecks(mockCtx);
      expect(result.allPassed).toBe(false);
      expect(result.checks[0].status).toBe('FAILED');
      expect(result.checks[0].error).toContain('Socket timeout');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });

  describe('Diagnostics CLI commands execution', () => {
    it('should execute memora diagnostics health successfully', async () => {
      const mockResp: HealthResponse = { status: 'UP', version: '1.0.0' };
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'diagnostics', 'health', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { allPassed: boolean };
      expect(output.allPassed).toBe(true);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute memora diagnostics config successfully', async () => {
      const exitCode = await run(['node', 'memora', 'diagnostics', 'config', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { allPassed: boolean };
      expect(output.allPassed).toBe(true);
    });

    it('should execute memora diagnostics connectivity successfully', async () => {
      const mockResp: HealthResponse = { status: 'UP' };
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'diagnostics', 'connectivity', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { allPassed: boolean };
      expect(output.allPassed).toBe(true);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
