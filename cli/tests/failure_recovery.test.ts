import { run } from '../src/cli';

describe('Failure Recovery Integration Test Suite', () => {
  let logSpy: jest.SpyInstance;
  let stderrSpy: jest.SpyInstance;
  let fetchSpy: jest.SpyInstance;

  beforeEach(() => {
    logSpy = jest.spyOn(console, 'log').mockImplementation();
    stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);
  });

  afterEach(() => {
    logSpy.mockRestore();
    stderrSpy.mockRestore();
    if (fetchSpy) {
      fetchSpy.mockRestore();
    }
  });

  it('should handle Connection Refused gracefully and return exit code 1', async () => {
    const connError = new Error('connect ECONNREFUSED 127.0.0.1');
    fetchSpy = jest.spyOn(global, 'fetch').mockRejectedValue(connError);

    const code = await run(['node', 'memora', 'project', 'list']);
    expect(code).toBe(1);
    expect(stderrSpy).toHaveBeenCalled();
    const combined = stderrSpy.mock.calls.map(call => call[0]).join('\n');
    expect(combined).toContain('ECONNREFUSED');
  });

  it('should handle HTTP Timeout gracefully and return exit code 4', async () => {
    const abortError = new Error('The operation was aborted.');
    abortError.name = 'AbortError';
    fetchSpy = jest.spyOn(global, 'fetch').mockRejectedValue(abortError);

    const code = await run(['node', 'memora', 'project', 'list']);
    expect(code).toBe(4);
    expect(stderrSpy).toHaveBeenCalled();
    const combined = stderrSpy.mock.calls.map(call => call[0]).join('\n');
    expect(combined).toContain('timed out');
  });

  it('should handle missing positional arguments gracefully and return exit code 6', async () => {
    const code = await run(['node', 'memora', 'project', 'show']);
    expect(code).toBe(6);
  });

  it('should handle non-existent project refresh gracefully and return exit code 2', async () => {
    // Return empty list of projects so target cannot be resolved
    fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue({
      status: 200,
      headers: new Map([['content-type', 'application/json']]),
      text: async () => '[]',
      ok: true,
    } as unknown as Response);

    const code = await run(['node', 'memora', 'project', 'refresh', 'p-nonexistent']);
    expect(code).toBe(2);
  });
});
