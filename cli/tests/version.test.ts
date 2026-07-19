import { VersionProvider } from '../src/utils/versionProvider';
import { run } from '../src/cli';

describe('VersionProvider & SemVer Compliance', () => {
  it('should return valid structured version info', () => {
    const info = VersionProvider.getVersionInfo();
    expect(info).toBeDefined();
    expect(info.major).toBe(1);
    expect(info.minor).toBe(0);
    expect(info.patch).toBe(0);
    expect(info.prerelease).toBeUndefined();
    expect(info.buildMetadata).toBeUndefined();
  });

  it('should compile correct full version string', () => {
    const ver = VersionProvider.getVersionString();
    expect(ver).toBe('1.0.0');
  });

  it('should support dynamic version outputs on CLI interceptor', async () => {
    const originalConsoleLog = console.log;
    const logs: string[] = [];
    console.log = (msg: string) => {
      logs.push(msg);
    };

    const code = await run(['node', 'memora', 'version']);
    console.log = originalConsoleLog;

    expect(code).toBe(0);
    expect(logs[0]).toContain('Memora CLI v1.0.0');
    expect(logs[0]).not.toContain('Backend');
  });

  it('should support JSON version outputs on CLI interceptor', async () => {
    const originalConsoleLog = console.log;
    const logs: string[] = [];
    console.log = (msg: string) => {
      logs.push(msg);
    };

    const code = await run(['node', 'memora', '--json', 'version']);
    console.log = originalConsoleLog;

    expect(code).toBe(0);
    const parsed = JSON.parse(logs[0]);
    expect(parsed.cliVersion).toBe('1.0.0');
    expect(parsed.platform).toBe(process.platform);
    expect(parsed.backend).toBeUndefined();
    expect(parsed.engineVersion).toBeUndefined();
  });
});
