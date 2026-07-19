import { knowledgeApplicationService } from '../src/services/knowledge';
import { KnowledgeDocument, QueryKnowledgeResponse } from '../src/api/knowledge';
import { ProjectResponse } from '../src/api/project';
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

describe('Knowledge Management Subsystem', () => {
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

  describe('KnowledgeQuery model & service layer mapping', () => {
    it('should construct immutable KnowledgeQuery objects', () => {
      const query = knowledgeApplicationService.createQuery('search-term', 'proj-1', 5);
      expect(query.queryText).toBe('search-term');
      expect(query.projectId).toBe('proj-1');
      expect(query.limit).toBe(5);

      expect(() => {
        (query as { queryText: string }).queryText = 'mutated';
      }).toThrow();
    });

    it('should execute knowledge search query correctly', async () => {
      const mockResp: QueryKnowledgeResponse = {
        documents: [
          { id: 'doc-1', title: 'Doc Title', content: 'Doc Content', score: 0.99 },
        ],
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const query = knowledgeApplicationService.createQuery('test', 'p-1');
      const searchResult = await knowledgeApplicationService.searchKnowledge(query, mockCtx);
      expect(searchResult.documents.length).toBe(1);
      expect(searchResult.documents[0].id).toBe('doc-1');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });

  describe('Knowledge CLI commands execution', () => {
    it('should execute memora knowledge search successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-search', name: 'proj1', rootPath: process.cwd() },
      ];
      const mockSearchResp: QueryKnowledgeResponse = {
        documents: [
          { id: 'k-doc', title: 'Knowledge Title', content: 'Doc Info', score: 0.95 },
        ],
      };

      const mockTransport: HttpTransport = {
        send: async <T>(builder: unknown) => {
          const b = builder as { path: string };
          if (b.path.includes('/projects')) {
            return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
          }
          return { status: 200, headers: {}, data: mockSearchResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'knowledge', 'search', 'query-term', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as QueryKnowledgeResponse;
      expect(output.documents.length).toBe(1);
      expect(output.documents[0].id).toBe('k-doc');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute memora knowledge show successfully', async () => {
      const mockDoc: KnowledgeDocument = {
        id: 'doc-show',
        title: 'Document Details',
        content: 'Long file context text',
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockDoc as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'knowledge', 'show', 'doc-show', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as KnowledgeDocument;
      expect(output.id).toBe('doc-show');
      expect(output.title).toBe('Document Details');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute memora knowledge explain successfully', async () => {
      const mockExplanation = {
        id: 'doc-explain',
        explanation: 'Detailed symbol usage description',
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockExplanation as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'knowledge', 'explain', 'doc-explain', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as typeof mockExplanation;
      expect(output.id).toBe('doc-explain');
      expect(output.explanation).toBe('Detailed symbol usage description');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
