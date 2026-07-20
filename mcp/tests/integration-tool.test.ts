import { MemoraMcpServer } from '../src/server';
import { ToolDispatcher } from '../src/tool/executor';
import { httpClientService } from 'memora-cli/src/http/clientService';
import { HttpTransport } from 'memora-cli/src/http/transport';
import { ToolValidationError } from '../src/errors';
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

describe('Memora MCP Tool Integration Tests', () => {
  let server: MemoraMcpServer;
  let dispatcher: ToolDispatcher;
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

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'req-int',
    sessionId: 'session-int',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params,
    metadata: new Map<string, unknown>(),
  });

  // Simple transport mock helper
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const setMockTransport = (sendFn: any) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
    dispatcher = new ToolDispatcher(server.getToolRegistry());
  });

  describe('Tool Registration & Metadata', () => {
    it('should register status, doctor, projects, search, and handoff tools', () => {
      const registry = server.getToolRegistry();
      expect(registry.hasTool('status')).toBe(true);
      expect(registry.hasTool('doctor')).toBe(true);
      expect(registry.hasTool('projects')).toBe(true);
      expect(registry.hasTool('search')).toBe(true);
      expect(registry.hasTool('handoff')).toBe(true);

      const searchMeta = registry.getMetadata('search')!;
      expect(searchMeta.displayName).toBe('Semantic Knowledge Search');
      expect(searchMeta.version).toBe('1.0.0');
    });
  });

  describe('Status Tool Execution', () => {
    it('should return UP when backend health endpoint reports UP', async () => {
      setMockTransport(async () => {
        return {
          status: 200,
          headers: {},
          data: { status: 'UP', version: 'v1.0.0' },
        };
      });

      const res = await dispatcher.dispatchCall('status', {}, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.backendConnectivity).toBe('UP');
      expect(body.mcpServerStatus).toBe('RUNNING');
    });

    it('should return DOWN connectivity when backend fails', async () => {
      setMockTransport(async () => {
        throw new Error('Connection refused');
      });

      const res = await dispatcher.dispatchCall('status', {}, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.backendConnectivity).toBe('DOWN');
    });
  });

  describe('Doctor Tool Execution', () => {
    it('should diagnose healthy server setup', async () => {
      setMockTransport(async () => {
        return {
          status: 200,
          headers: {},
          data: { status: 'UP' },
        };
      });

      const res = await dispatcher.dispatchCall('doctor', {}, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.backendHealth).toBe('UP');
      expect(body.recommendations[0]).toContain('healthy');
    });

    it('should supply diagnostic recommendations on failures', async () => {
      setMockTransport(async () => {
        throw new Error('Network failure');
      });

      const res = await dispatcher.dispatchCall('doctor', {}, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.backendHealth).toBe('DOWN');
      expect(body.recommendations).toContain('Backend server is down or unreachable. Verify backend process is running.');
    });
  });

  describe('Projects Tool Execution', () => {
    it('should return lists of active registered project workspaces', async () => {
      setMockTransport(async () => {
        return {
          status: 200,
          headers: {},
          data: [
            { id: 'proj-1', name: 'Memora CLI', rootPath: '/workspace/cli' },
            { id: 'proj-2', name: 'Memora MCP', rootPath: '/workspace/mcp' },
          ],
        };
      });

      const res = await dispatcher.dispatchCall('projects', {}, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.projects).toHaveLength(2);
      expect(body.projects[0].name).toBe('Memora CLI');
    });
  });

  describe('Search Tool Execution & Input Validation', () => {
    it('should throw validation error on missing required properties', async () => {
      await expect(dispatcher.dispatchCall('search', {}, mockContext())).rejects.toThrow(ToolValidationError);
      await expect(dispatcher.dispatchCall('search', { projectId: 'id' }, mockContext())).rejects.toThrow(ToolValidationError);
    });

    it('should query knowledge document base and list semantic matches', async () => {
      setMockTransport(async () => {
        return {
          status: 200,
          headers: {},
          data: {
            documents: [
              { id: 'doc-1', title: 'Clean Architecture Guide', content: 'Use controllers and domain entities', score: 0.95 },
            ],
          },
        };
      });

      const res = await dispatcher.dispatchCall('search', { projectId: 'proj-1', query: 'architecture' }, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.documents).toHaveLength(1);
      expect(body.documents[0].title).toBe('Clean Architecture Guide');
      expect(body.documents[0].relevance).toBe(0.95);
    });
  });

  describe('Handoff Tool Execution', () => {
    it('should require projectId parameter', async () => {
      await expect(dispatcher.dispatchCall('handoff', {}, mockContext())).rejects.toThrow(ToolValidationError);
    });

    it('should parse generated markdown context snapshots into structured attributes', async () => {
      const mockMarkdown = `
# Project Summary
A state of the art context manager.

## Architecture
MCP Tool Layer -> Application Services -> Domain.

## Active Tasks
- Compile code integrations.
- Verify tests pass.
      `;

      setMockTransport(async () => {
        return {
          status: 200,
          headers: {},
          data: {
            projectId: 'proj-1',
            content: mockMarkdown,
            updatedAt: new Date().toISOString(),
          },
        };
      });

      const res = await dispatcher.dispatchCall('handoff', { projectId: 'proj-1' }, mockContext());
      const body = JSON.parse(res.content[0].text);
      expect(body.architecture).toContain('MCP Tool Layer');
      expect(body.activeTasks).toContain('Verify tests pass.');
    });
  });
});
