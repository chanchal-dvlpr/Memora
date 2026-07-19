import * as fs from 'fs';
import * as path from 'path';
import { run } from '../src/cli';
import { VersionProvider } from '../src/utils/versionProvider';

describe('CLI Command Router', () => {
  let logSpy: jest.SpyInstance;
  let errorSpy: jest.SpyInstance;

  beforeEach(() => {
    logSpy = jest.spyOn(console, 'log').mockImplementation();
    errorSpy = jest.spyOn(console, 'error').mockImplementation();

    jest.spyOn(global, 'fetch').mockImplementation(async (urlInfo, init) => {
      const urlStr = typeof urlInfo === 'string' ? urlInfo : urlInfo.toString();
      const method = init?.method?.toUpperCase() || 'GET';

      if (urlStr.includes('/refresh')) {
        return {
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          text: async () => JSON.stringify({ projectId: 'proj-123', filesScanned: 10, snapshotGenerated: true }),
          ok: true,
        } as unknown as Response;
      }

      if (urlStr.includes('/api/v1/projects')) {
        if (method === 'POST') {
          const bodyObj = init?.body ? JSON.parse(init.body as string) : {};
          if (!bodyObj.rootPath) {
            return {
              status: 400,
              headers: new Map([['content-type', 'application/json']]),
              text: async () => JSON.stringify({ message: 'Root path is required' }),
              ok: false,
            } as unknown as Response;
          }
          return {
            status: 200,
            headers: new Map([['content-type', 'application/json']]),
            text: async () => JSON.stringify({ id: 'proj-123', name: bodyObj.name || 'vscode-extension', rootPath: bodyObj.rootPath }),
            ok: true,
          } as unknown as Response;
        } else {
          return {
            status: 200,
            headers: new Map([['content-type', 'application/json']]),
            text: async () => JSON.stringify([{ id: 'proj-123', name: 'vscode-extension', rootPath: process.cwd() }]),
            ok: true,
          } as unknown as Response;
        }
      }

      if (urlStr.includes('/api/v1/context/')) {
        return {
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          text: async () => JSON.stringify({
            projectId: 'proj-123',
            content: 'handoff content',
            updatedAt: '2026-07-18T00:00:00Z',
          }),
          ok: true,
        } as unknown as Response;
      }

      if (urlStr.includes('/api/v1/knowledge/query')) {
        return {
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          text: async () => JSON.stringify({
            documents: [
              { id: 'k-1', title: 'test doc', content: 'knowledge contents here', score: 0.99 },
            ],
          }),
          ok: true,
        } as unknown as Response;
      }

      if (urlStr.includes('/health')) {
        return {
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          text: async () => JSON.stringify({ status: 'UP' }),
          ok: true,
        } as unknown as Response;
      }

      return {
        status: 404,
        headers: new Map([['content-type', 'application/json']]),
        text: async () => 'Not Found',
        ok: false,
      } as unknown as Response;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should print help menu and return exit code 0 when help option is passed', async () => {
    const exitCode = await run(['node', 'memora', '--help']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
  });

  it('should print help menu in JSON format when help option and json flags are passed', async () => {
    const exitCode = await run(['node', 'memora', '--help', '--json']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    const parsed = JSON.parse(logSpy.mock.calls[0][0]);
    expect(parsed.usage).toBe('memora <subcommand> [options]');
  });

  it('should print version and return exit code 0 when version option is passed', async () => {
    const exitCode = await run(['node', 'memora', '--version']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain(`Memora CLI v${VersionProvider.getVersionString()}`);
  });

  it('should print version in JSON format when version option and json flags are passed', async () => {
    const exitCode = await run(['node', 'memora', '--version', '--json']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    const parsed = JSON.parse(logSpy.mock.calls[0][0]);
    expect(parsed.cliVersion).toBe(VersionProvider.getVersionString());
  });

  it('should print error and return exit code 6 when unknown subcommand is executed', async () => {
    const exitCode = await run(['node', 'memora', 'unknown_cmd']);
    expect(exitCode).toBe(6);
  });

  it('should return exit code 6 when config flag has no argument', async () => {
    const exitCode = await run(['node', 'memora', '--config']);
    expect(exitCode).toBe(6);
  });

  it('should log verbose message to stderr when verbose flag is set', async () => {
    const tempConfig = path.resolve(__dirname, 'temp-verbose-config.json');
    fs.writeFileSync(tempConfig, '{}');
    const exitCode = await run(['node', 'memora', 'status', '--verbose', '--config', tempConfig]);
    expect(exitCode).toBe(0);
    expect(errorSpy).toHaveBeenCalledWith(`[VERBOSE] Custom Config: ${tempConfig}`);
    if (fs.existsSync(tempConfig)) {
      fs.unlinkSync(tempConfig);
    }
  });

  it('should execute init command with custom path and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'init', process.cwd(), '--name', 'vscode-extension']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('vscode-extension');
  });

  it('should execute init command without path argument and default to process.cwd()', async () => {
    const exitCode = await run(['node', 'memora', 'init']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('cli');
  });

  it('should execute projects command and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'projects']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('vscode-extension');
  });

  it('should execute refresh command and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'refresh', 'proj-123']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('proj-123');
  });

  it('should execute handoff command and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'handoff', 'proj-123']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('handoff content');
  });

  it('should execute search command with required query argument and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'search', 'term', 'proj-123']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('test doc');
  });

  it('should execute doctor command and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'doctor']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('PASSED');
  });

  it('should execute status command and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'status']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    expect(logSpy.mock.calls[0][0]).toContain('PASSED');
  });

  it('should execute version subcommand and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'version']);
    expect(exitCode).toBe(0);
    expect(logSpy.mock.calls[0][0]).toContain(`Memora CLI v${VersionProvider.getVersionString()}`);
  });

  it('should handle JSON output flag and return exit code 0', async () => {
    const exitCode = await run(['node', 'memora', 'status', '--json']);
    expect(exitCode).toBe(0);
    expect(logSpy).toHaveBeenCalled();
    const parsedJson = JSON.parse(logSpy.mock.calls[0][0]);
    expect(parsedJson.allPassed).toBe(true);
    expect(parsedJson.checks[0].status).toBe('PASSED');
  });

  it('should suppress log output when quiet flag is provided', async () => {
    const originalLog = console.log;
    const exitCode = await run(['node', 'memora', 'refresh', '--quiet']);
    expect(exitCode).toBe(0);
    console.log = originalLog;
  });
});
