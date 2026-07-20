/* eslint-disable @typescript-eslint/no-explicit-any */
import { MemoraMcpServer } from '../src/server';
import { ResourceDispatcher } from '../src/resource';
import { httpClientService } from 'memora-cli/src/http/clientService';
import { HttpTransport } from 'memora-cli/src/http/transport';
import { ResourceValidationError, ResourceExecutionError } from '../src/errors';
import { StructuredLogger } from '../src/logging/logger';
import { configService } from 'memora-cli/src/config/service';

jest.mock('@modelcontextprotocol/sdk/types.js', () => {
  return {
    CallToolRequestSchema: { method: 'tools/call' },
    ListToolsRequestSchema: { method: 'tools/list' },
    ListResourcesRequestSchema: { method: 'resources/list' },
    ReadResourceRequestSchema: { method: 'resources/read' },
  };
});

jest.mock('@modelcontextprotocol/sdk/server/index.js', () => {
  return {
    Server: jest.fn().mockImplementation(() => {
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        close: jest.fn().mockResolvedValue(undefined),
        onerror: jest.fn(),
        onclose: jest.fn(),
        setRequestHandler: jest.fn(),
      };
    }),
  };
});

describe('Memora MCP Resource Integration Tests', () => {
  let server: MemoraMcpServer;
  let dispatcher: ResourceDispatcher;
  const logger = new StructuredLogger('test', 'error');

  const baseConfig = {
    serverName: 'test-mcp',
    version: '1.0.0',
    host: '127.0.0.1',
    port: 8082,
    environment: 'test' as const,
    timeout: 5000,
    logLevel: 'error' as const,
  };

  const mockContext = (uri: string, params: Record<string, unknown> = {}) => ({
    requestId: 'req-res-int',
    sessionId: 'session-res-int',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params: {
      uri,
      ...params,
    },
    metadata: new Map<string, unknown>(),
  });

  // Simple transport mock helper
  const setMockTransport = (sendFn: any) => {
    (httpClientService as any).pipeline.transport = {
      send: sendFn,
    } as HttpTransport;
  };

  beforeAll(async () => {
    const mockMcpTransport = {
      type: 'stdio' as const,
      initialize: jest.fn().mockResolvedValue(undefined),
      getTransportInstance: jest.fn().mockReturnValue({}),
      close: jest.fn().mockResolvedValue(undefined),
    };

    server = new MemoraMcpServer(baseConfig, mockMcpTransport);
    server.initialize();
    await configService.load();
    dispatcher = new ResourceDispatcher(server.getResourceRegistry());
  });

  describe('Resource Registrations & Metadata', () => {
    it('should register all production resources with detailed metadata', () => {
      const registry = server.getResourceRegistry();
      const resources = registry.listResources();

      expect(resources.some(r => r.name === 'project')).toBe(true);
      expect(resources.some(r => r.name === 'architecture')).toBe(true);
      expect(resources.some(r => r.name === 'knowledge')).toBe(true);
      expect(resources.some(r => r.name === 'tasks')).toBe(true);
      expect(resources.some(r => r.name === 'decisions')).toBe(true);

      // Check project resource metadata
      const projMeta = registry.getMetadata('memora://project')!;
      expect(projMeta.displayName).toBe('Memora Project Details');
      expect(projMeta.mimeType).toBe('application/json');
      expect(projMeta.tags).toContain('project-info');

      // Check architecture resource metadata
      const archMeta = registry.getMetadata('memora://architecture')!;
      expect(archMeta.displayName).toBe('Memora Architecture Design');
      expect(archMeta.mimeType).toBe('text/markdown');
      expect(archMeta.tags).toContain('architecture');
    });
  });

  describe('Project Resource', () => {
    it('should retrieve read-only project info', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return {
            status: 200,
            headers: {},
            data: [
              { id: 'proj123', name: 'memora-project', rootPath: process.cwd() },
            ],
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead('memora://project', mockContext('memora://project'));
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('application/json');

      const payload = JSON.parse(res.contents[0].text!);
      expect(payload.projectId).toBe('proj123');
      expect(payload.projectName).toBe('memora-project');
      expect(payload.rootPath).toBe(process.cwd());
      expect(payload.languages).toContain('TypeScript');
    });
  });

  describe('Architecture Resource', () => {
    it('should retrieve architecture info as markdown by default', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: process.cwd() }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '## Architecture\nMy custom architecture layout\n## Modules\nMy modules overview',
              updatedAt: '2026-07-20T00:00:00Z',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead('memora://architecture', mockContext('memora://architecture'));
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('text/markdown');
      expect(res.contents[0].text).toContain('# Architecture Details');
      expect(res.contents[0].text).toContain('My custom architecture layout');
    });

    it('should retrieve architecture info as JSON when mimeType is requested', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: process.cwd() }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '## Architecture\nMy custom architecture layout\n## Modules\nMy modules overview',
              updatedAt: '2026-07-20T00:00:00Z',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead(
        'memora://architecture',
        mockContext('memora://architecture', { mimeType: 'application/json' })
      );
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('application/json');

      const payload = JSON.parse(res.contents[0].text!);
      expect(payload.architectureSummary).toContain('My custom architecture layout');
      expect(payload.moduleOverview).toContain('My modules overview');
    });
  });

  describe('Knowledge Resource', () => {
    it('should retrieve indexed knowledge base and support filtering', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: process.cwd() }] };
        }
        if (path.includes('/knowledge/')) {
          return {
            status: 200,
            headers: {},
            data: {
              documents: [
                { id: 'k1', title: 'class ProjectApplicationService', content: 'Orchestrates project tasks', type: 'symbol', path: '/src/service.ts' },
              ],
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead(
        'memora://knowledge?query=Project',
        mockContext('memora://knowledge?query=Project')
      );
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('application/json');

      const payload = JSON.parse(res.contents[0].text!);
      expect(payload.symbols).toHaveLength(1);
      expect(payload.symbols[0].name).toBe('class ProjectApplicationService');
      expect(payload.summaries[0].summary).toBe('Orchestrates project tasks');
    });
  });

  describe('Tasks Resource', () => {
    it('should retrieve parsed tasks from context', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: process.cwd() }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '## Tasks\n- [ ] Implement resources\n- [x] Implement tools\n',
              updatedAt: '2026-07-20T00:00:00Z',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead('memora://tasks', mockContext('memora://tasks'));
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('application/json');

      const payload = JSON.parse(res.contents[0].text!);
      expect(payload.activeTasks).toHaveLength(1);
      expect(payload.activeTasks[0].title).toBe('Implement resources');
      expect(payload.completedTasks).toHaveLength(1);
      expect(payload.completedTasks[0].title).toBe('Implement tools');
    });
  });

  describe('Decisions Resource', () => {
    it('should retrieve parsed ADR decisions from context', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: process.cwd() }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '## Decisions\n### ADR-1 Use MCP\nWe decided to use MCP because it is robust.\n',
              updatedAt: '2026-07-20T00:00:00Z',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const res = await dispatcher.dispatchRead('memora://decisions', mockContext('memora://decisions'));
      expect(res.contents).toHaveLength(1);
      expect(res.contents[0].mimeType).toBe('application/json');

      const payload = JSON.parse(res.contents[0].text!);
      expect(payload.adrs).toHaveLength(1);
      expect(payload.adrs[0].title).toBe('ADR-1 Use MCP');
      expect(payload.adrs[0].rationale).toBe('We decided to use MCP because it is robust.');
    });
  });

  describe('Error Handling', () => {
    it('should translate backend connection errors to ResourceExecutionError', async () => {
      setMockTransport(async () => {
        throw new Error('Connection refused');
      });

      await expect(
        dispatcher.dispatchRead('memora://project', mockContext('memora://project'))
      ).rejects.toThrow(ResourceExecutionError);
    });

    it('should translate invalid parameters to ResourceValidationError', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [] };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      await expect(
        dispatcher.dispatchRead('memora://project', mockContext('memora://project'))
      ).rejects.toThrow(ResourceValidationError);
    });
  });
});
