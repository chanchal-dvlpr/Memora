import { knowledgeApplicationService } from '../src/services/knowledge';
import { QueryKnowledgeResponse } from '../src/api/knowledge';
import { ProjectResponse } from '../src/api/project';
import { ValidationError } from '../src/errors/errors';
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

describe('Knowledge Operations & Mappings', () => {
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
    it('should throw validation error for invalid knowledge ID format', async () => {
      await expect(
        knowledgeApplicationService.showKnowledge('invalid doc id!', { env: {} } as unknown as ExecutionContext),
      ).rejects.toThrow(ValidationError);
    });

    it('should throw validation error for invalid project ID format on refresh', async () => {
      await expect(
        knowledgeApplicationService.refreshKnowledge('invalid project id!', { env: {} } as unknown as ExecutionContext),
      ).rejects.toThrow(ValidationError);
    });
  });

  describe('Knowledge Operations subcommands', () => {
    it('should execute knowledge refresh command successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-refresh', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockQueryResp: QueryKnowledgeResponse = {
        documents: [
          { id: 'doc-1', title: 'Refreshed Title', content: 'Doc text' },
        ],
      };

      const mockTransport: HttpTransport = {
        send: async <T>(builder: unknown) => {
          const b = builder as { path: string };
          if (b.path.includes('/projects')) {
            return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
          }
          return { status: 200, headers: {}, data: mockQueryResp as unknown as T };
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

      const exitCode = await run(['node', 'memora', 'knowledge', 'refresh', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as QueryKnowledgeResponse;
      expect(output.documents.length).toBe(1);
      expect(output.documents[0].title).toBe('Refreshed Title');

      expect(events.some((e) => e.type === 'KnowledgeRefreshed')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute knowledge delete command successfully', async () => {
      const mockDeleteResp = {
        id: 'doc-del',
        deleted: true,
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
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

      const exitCode = await run(['node', 'memora', 'knowledge', 'delete', 'doc-del', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as { id: string; success: boolean };
      expect(output.id).toBe('doc-del');
      expect(output.success).toBe(true);

      expect(events.some((e) => e.type === 'KnowledgeDeleted')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
