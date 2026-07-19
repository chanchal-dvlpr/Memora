import { contextApplicationService } from '../src/services/context';
import { ContextResponse } from '../src/api/context';
import { ProjectResponse } from '../src/api/project';
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

describe('Context Management Subsystem', () => {
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

  describe('ContextSession lifecycle & progress states', () => {
    it('should transition session states from IDLE to COMPLETED on success', async () => {
      const mockResp: ContextResponse = {
        projectId: 'p-1',
        content: 'ai context data',
        updatedAt: '2026-07-17',
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const session = contextApplicationService.createSession('p-1');
      expect(session.state).toBe('IDLE');
      expect(session.progress).toBe(0);

      const { result, session: completedSession } =
        await contextApplicationService.generateContext('p-1', mockCtx);
      expect(result.content).toBe('ai context data');
      expect(completedSession.state).toBe('COMPLETED');
      expect(completedSession.progress).toBe(100);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should transition session state to FAILED on errors', async () => {
      const mockTransport: HttpTransport = {
        send: async () => {
          throw new Error('generation error');
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      await expect(
        contextApplicationService.generateContext('p-1', mockCtx),
      ).rejects.toThrow();

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });

  describe('Context CLI commands execution', () => {
    it('should execute memora context show successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-show', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockContextResp: ContextResponse = {
        projectId: 'proj-show',
        content: 'pretty context info',
        updatedAt: '2026',
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

      const exitCode = await run(['node', 'memora', 'context', 'show', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as ContextResponse;
      expect(output.projectId).toBe('proj-show');
      expect(output.content).toBe('pretty context info');

      expect(events.some((e) => e.type === 'ContextViewed')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should export project context as markdown format', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-export', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockContextResp: ContextResponse = {
        projectId: 'proj-export',
        content: '# Title\n- Item 1',
        updatedAt: '2026',
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

      const exitCode = await run([
        'node',
        'memora',
        'context',
        'export',
        '--format',
        'markdown',
      ]);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      expect(logSpy.mock.calls[0][0]).toBe('# Title\n- Item 1');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
