import { contextApplicationService } from '../src/services/context';
import { ContextResponse } from '../src/api/context';
import { ProjectResponse } from '../src/api/project';
import { ValidationError } from '../src/errors/errors';
import { HttpTransport } from '../src/http/transport';
import { httpClientService } from '../src/http/clientService';
import { configService } from '../src/config/service';
import { commandEventPublisher, CommandEvent } from '../src/events/commandEvents';
import { ExecutionContext } from '../src/models/context';
import { run } from '../src/cli';
import { Console } from 'console';

interface ServiceWithPipeline {
  pipeline: {
    transport: HttpTransport;
  };
}

describe('Context Operations & Mappings', () => {
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

  describe('Input and response validations', () => {
    it('should throw validation error for invalid project ID format', async () => {
      await expect(
        contextApplicationService.getContext('invalid project id!', { env: {} } as unknown as ExecutionContext),
      ).rejects.toThrow(ValidationError);
    });

    it('should throw validation error for invalid API responses missing content', async () => {
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: { projectId: 'p-1' } as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      await expect(
        contextApplicationService.getContext('proj123', { env: {} } as unknown as ExecutionContext),
      ).rejects.toThrow(ValidationError);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });

  describe('Context Operations subcommands', () => {
    it('should execute context refresh command successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-refresh', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockContextResp: ContextResponse = {
        projectId: 'proj-refresh',
        content: 'refreshed content',
        updatedAt: '2026-07-17',
      };

      const mockTransport: HttpTransport = {
        send: async <T>(builder: unknown) => {
          const b = builder as { path: string };
          if (b.path.includes('/projects')) {
            return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
          }
          return { status: 200, headers: {}, data: mockContextResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      const exitCode = await run(['node', 'memora', 'context', 'refresh', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as ContextResponse;
      expect(output.projectId).toBe('proj-refresh');
      expect(output.content).toBe('refreshed content');

      expect(events.some((e) => e.type === 'ContextRefreshed')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute context delete command successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-delete', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockDeleteResp = {
        projectId: 'proj-delete',
        deleted: true,
      };

      const mockTransport: HttpTransport = {
        send: async <T>(builder: unknown) => {
          const b = builder as { path: string };
          if (b.path.includes('/projects')) {
            return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
          }
          return { status: 200, headers: {}, data: mockDeleteResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      const exitCode = await run(['node', 'memora', 'context', 'delete', 'proj-delete', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { projectId: string; success: boolean };
      expect(output.projectId).toBe('proj-delete');
      expect(output.success).toBe(true);

      expect(events.some((e) => e.type === 'ContextDeleted')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
