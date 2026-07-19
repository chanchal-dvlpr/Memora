import { HttpClientService } from '../src/http/clientService';
import { ProjectApiClient } from '../src/api/project';
import { ContextApiClient } from '../src/api/context';
import { KnowledgeApiClient } from '../src/api/knowledge';
import { HealthApiClient } from '../src/api/health';
import { ConfigService } from '../src/config/service';
import { ConfigLoader } from '../src/config/loader';
import { DefaultConfigSource } from '../src/config/source';
import { ExecutionContext } from '../src/models/context';
import { HttpTransport } from '../src/http/transport';

describe('Resource API Clients Layer', () => {
  let service: ConfigService;
  let clientService: HttpClientService;
  const mockCtx = {
    workingDir: '/workspace',
    requestId: 'req-123',
    correlationId: 'corr-456',
    env: {},
  } as unknown as ExecutionContext;

  beforeEach(() => {
    service = new ConfigService(new ConfigLoader([new DefaultConfigSource()]));
  });

  it('should list and create projects via ProjectApiClient', async () => {
    await service.load();
    const mockProjects = [
      { id: '1', name: 'p1', path: '/p1' },
      { id: '2', name: 'p2', path: '/p2' },
    ];

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: mockProjects as unknown as T };
      },
    };

    clientService = new HttpClientService(service, mockTransport, []);
    const client = new ProjectApiClient(clientService);
    const result = await client.listProjects(mockCtx);

    expect(result).toEqual(mockProjects);
  });

  it('should query context details via ContextApiClient', async () => {
    await service.load();
    const mockContextData = { projectId: '123', content: 'test data', updatedAt: '2026-07' };

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: mockContextData as unknown as T };
      },
    };

    clientService = new HttpClientService(service, mockTransport, []);
    const client = new ContextApiClient(clientService);
    const result = await client.getContext({ projectId: '123' }, mockCtx);

    expect(result).toEqual(mockContextData);
  });

  it('should query knowledge elements via KnowledgeApiClient', async () => {
    await service.load();
    const mockResp = { documents: [{ id: 'k1', title: 't1', content: 'c1' }] };

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: mockResp as unknown as T };
      },
    };

    clientService = new HttpClientService(service, mockTransport, []);
    const client = new KnowledgeApiClient(clientService);
    const result = await client.queryKnowledge({ projectId: '123', query: 'search' }, mockCtx);

    expect(result).toEqual(mockResp);
  });

  it('should perform daemon checks via HealthApiClient', async () => {
    await service.load();
    const mockResp = { status: 'UP', version: '1.0.0' };

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: mockResp as unknown as T };
      },
    };

    clientService = new HttpClientService(service, mockTransport, []);
    const client = new HealthApiClient(clientService);
    const result = await client.checkHealth(mockCtx);

    expect(result).toEqual(mockResp);
  });
});
