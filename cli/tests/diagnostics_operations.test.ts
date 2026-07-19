import { diagnosticsApplicationService } from '../src/services/diagnostics';
import { HealthResponse } from '../src/api/health';
import { ValidationError } from '../src/errors/errors';
import * as validator from '../src/validators/diagnostics';
import { ExecutionContext } from '../src/models/context';
import { HttpTransport } from '../src/http/transport';
import { httpClientService } from '../src/http/clientService';
import { configService } from '../src/config/service';
import { commandEventPublisher, CommandEvent } from '../src/events/commandEvents';
import { run } from '../src/cli';
import { Console } from 'console';

interface ServiceWithPipeline {
  pipeline: {
    transport: HttpTransport;
  };
}

describe('Diagnostics Operations & Report Generators', () => {

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

  describe('Validator checks & event notification sequences', () => {
    it('should throw validation error for invalid diagnostic type', () => {
      expect(() => validator.validateDiagnosticType('invalid')).toThrow(ValidationError);
    });

    it('should publish DiagnosticsValidationFailed event when pre-flight validation fails', async () => {
      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      await expect(
        diagnosticsApplicationService.runEnvironmentChecks({ env: undefined } as unknown as ExecutionContext),
      ).rejects.toThrow(ValidationError);

      expect(events.some((e) => e.type === 'DiagnosticsValidationFailed')).toBe(true);
      unsubscribe();
    });
  });

  describe('Diagnostics Operations subcommands', () => {
    it('should execute diagnostics environment successfully', async () => {
      const exitCode = await run(['node', 'memora', 'diagnostics', 'environment', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { allPassed: boolean };
      expect(output.allPassed).toBe(true);
    });

    it('should execute diagnostics report successfully', async () => {
      const mockResp: HealthResponse = { status: 'UP', version: '1.0.0' };
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'diagnostics', 'report', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as {
        allPassed: boolean;
        health: { allPassed: boolean };
        configuration: { allPassed: boolean };
        connectivity: { allPassed: boolean };
        environment: { allPassed: boolean };
      };
      expect(output.allPassed).toBe(true);
      expect(output.health.allPassed).toBe(true);
      expect(output.configuration.allPassed).toBe(true);
      expect(output.connectivity.allPassed).toBe(true);
      expect(output.environment.allPassed).toBe(true);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should handle validation failures inside check suites', async () => {
      const spy = jest.spyOn(validator, 'validateDiagnosticType').mockImplementation(() => {
        throw new ValidationError('Mocked type validation failure');
      });

      const fallbackCtx = { env: {} } as unknown as ExecutionContext;
      await expect(diagnosticsApplicationService.runHealthChecks(fallbackCtx)).rejects.toThrow('Mocked type validation failure');
      await expect(diagnosticsApplicationService.runConfigChecks(fallbackCtx)).rejects.toThrow('Mocked type validation failure');
      await expect(diagnosticsApplicationService.runConnectivityChecks(fallbackCtx)).rejects.toThrow('Mocked type validation failure');
      await expect(diagnosticsApplicationService.runEnvironmentChecks(fallbackCtx)).rejects.toThrow('Mocked type validation failure');
      await expect(diagnosticsApplicationService.generateReport(fallbackCtx)).rejects.toThrow('Mocked type validation failure');

      spy.mockRestore();
    });
  });
});
