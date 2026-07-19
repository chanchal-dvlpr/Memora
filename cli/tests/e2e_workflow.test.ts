import * as fs from 'fs';
import * as path from 'path';
import { run } from '../src/cli';

describe('End-to-End Workflow Integration Test Suite', () => {
  const mockWorkingDir = path.join(__dirname, 'mock_e2e_workspace');
  let logSpy: jest.SpyInstance;
  let fetchSpy: jest.SpyInstance;

  beforeAll(() => {
    if (!fs.existsSync(mockWorkingDir)) {
      fs.mkdirSync(mockWorkingDir, { recursive: true });
    }
  });

  afterAll(() => {
    if (fs.existsSync(mockWorkingDir)) {
      fs.rmSync(mockWorkingDir, { recursive: true, force: true });
    }
  });

  beforeEach(() => {
    logSpy = jest.spyOn(console, 'log').mockImplementation();
  });

  afterEach(() => {
    logSpy.mockRestore();
    if (fetchSpy) {
      fetchSpy.mockRestore();
    }
  });

  it('should run a complete E2E CLI command lifecycle with mock backend responses', async () => {
    // 1. Setup fetch mocks for API backend calls
    const mockResponses: Record<string, { status: number; body: string }> = {
      '/api/v1/projects/p-123/refresh': {
        status: 200,
        body: JSON.stringify({ projectId: 'p-123', filesScanned: 12, snapshotGenerated: true }),
      },
      '/api/v1/context/p-123/generate': {
        status: 200,
        body: JSON.stringify({
          projectId: 'p-123',
          content: 'mock generated context content',
          updatedAt: new Date().toISOString(),
        }),
      },
      '/api/v1/knowledge/query': {
        status: 200,
        body: JSON.stringify({
          documents: [
            { id: 'k-1', title: 'test doc', content: 'knowledge contents here', score: 0.99 },
          ],
        }),
      },
      '/api/v1/projects': {
        status: 200,
        body: JSON.stringify({ id: 'p-123', name: 'mock-e2e', rootPath: mockWorkingDir }),
      },
      '/health': {
        status: 200,
        body: JSON.stringify({ status: 'UP' }),
      },
    };

    fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(async (urlInfo) => {
      const urlStr = typeof urlInfo === 'string' ? urlInfo : urlInfo.toString();
      // Sort keys by descending length so that more specific paths match first
      const matchedKey = Object.keys(mockResponses)
        .sort((a, b) => b.length - a.length)
        .find((key) => urlStr.includes(key));
      const res = matchedKey ? mockResponses[matchedKey] : { status: 404, body: 'Not Found' };

      return {
        status: res.status,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => res.body,
        ok: res.status >= 200 && res.status < 300,
      } as unknown as Response;
    });

    // 2. Command: Register Project
    const initCode = await run(['node', 'memora', 'project', 'register', mockWorkingDir, '--name', 'mock-e2e']);
    expect(initCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('mock-e2e');

    // 3. Command: List Workspaces
    mockResponses['/api/v1/projects'] = {
      status: 200,
      body: JSON.stringify([{ id: 'p-123', name: 'mock-e2e', rootPath: mockWorkingDir }]),
    };
    logSpy.mockClear();
    const listCode = await run(['node', 'memora', 'project', 'list']);
    expect(listCode).toBe(0);
    expect(logSpy.mock.calls[0][0]).toContain('mock-e2e');

    // 4. Command: Generate Context (refresh)
    logSpy.mockClear();
    const refreshCode = await run(['node', 'memora', 'project', 'refresh', 'p-123']);
    expect(refreshCode).toBe(0);
    expect(logSpy.mock.calls[0][0]).toContain('p-123');

    // 5. Command: context generate
    logSpy.mockClear();
    const genContextCode = await run(['node', 'memora', 'context', 'generate', 'p-123']);
    expect(genContextCode).toBe(0);

    // 6. Command: Query Knowledge (search)
    logSpy.mockClear();
    const searchCode = await run(['node', 'memora', 'knowledge', 'search', 'test', 'p-123']);
    expect(searchCode).toBe(0);

    // 7. Command: Diagnostics Check (health)
    logSpy.mockClear();
    const healthCode = await run(['node', 'memora', 'diagnostics', 'health']);
    expect(healthCode).toBe(0);
    expect(logSpy.mock.calls[0][0]).toContain('PASSED');

    // 8. Output Format check (JSON flag)
    logSpy.mockClear();
    const jsonCode = await run(['node', 'memora', '--json', 'project', 'list']);
    expect(jsonCode).toBe(0);
    const parsed = JSON.parse(logSpy.mock.calls[0][0]);
    expect(parsed[0].id).toBe('p-123');
  });
});
