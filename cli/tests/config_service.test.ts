import * as fs from 'fs';
import * as path from 'path';
import { ConfigService } from '../src/config/service';
import { ConfigLoader } from '../src/config/loader';
import {
  DefaultConfigSource,
  FileConfigSource,
  EnvConfigSource,
  CliOptionConfigSource,
} from '../src/config/source';
import { isFileWritableOnlyByOwner } from '../src/config/storage';
import { ConfigurationError } from '../src/errors/errors';

describe('Configuration Service API Lifecycle', () => {
  const tempConfigDir = path.resolve(__dirname, 'temp-srv-dir');
  const tempConfigFile = path.resolve(tempConfigDir, 'config.json');

  let service: ConfigService;

  beforeAll(() => {
    if (!fs.existsSync(tempConfigDir)) {
      fs.mkdirSync(tempConfigDir);
    }
  });

  afterAll(() => {
    if (fs.existsSync(tempConfigFile)) {
      fs.unlinkSync(tempConfigFile);
    }
    if (fs.existsSync(tempConfigDir)) {
      fs.rmdirSync(tempConfigDir);
    }
  });

  beforeEach(() => {
    delete process.env.MEMORA_CONFIG;
    if (fs.existsSync(tempConfigFile)) {
      fs.unlinkSync(tempConfigFile);
    }
    service = new ConfigService(
      new ConfigLoader([
        new DefaultConfigSource(),
        new FileConfigSource(),
        new EnvConfigSource(),
        new CliOptionConfigSource(),
      ]),
    );
  });

  it('should load default configurations on load call', async () => {
    const config = await service.load();
    expect(config.backend.url).toBe('http://localhost:8080');
  });

  it('should cache resolved config and return identical reference', async () => {
    const config1 = await service.load();
    const config2 = service.getConfig();
    expect(config1).toBe(config2);
  });

  it('should reload fresh configuration', async () => {
    await service.load();
    const reloadConfig = await service.reload({ json: true });
    expect(reloadConfig.output.mode).toBe('json');
  });

  it('should persist configuration updates, invalidate cache, and require reload', async () => {
    fs.writeFileSync(tempConfigFile, '{}');
    await service.load({ config: tempConfigFile });

    await service.updateFile({
      backend: { url: 'http://updated-host:9999' },
    });

    expect(() => service.getConfig()).toThrow(ConfigurationError);

    const loaded = await service.reload({ config: tempConfigFile });
    expect(loaded.backend.url).toBe('http://updated-host:9999');
  });

  it('should reset config file back to defaults', async () => {
    fs.writeFileSync(tempConfigFile, '{}');
    await service.load({ config: tempConfigFile });

    await service.updateFile({
      backend: { url: 'http://updated-host:9999' },
    });

    await service.reset();

    const resetConfig = await service.reload({ config: tempConfigFile });
    expect(resetConfig.backend.url).toBe('http://localhost:8080');
  });

  it('should warn about loose file permissions on macOS/Linux', () => {
    if (process.platform === 'win32') return;

    const stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation();

    fs.writeFileSync(tempConfigFile, '{}', { mode: 0o666 });
    isFileWritableOnlyByOwner(tempConfigFile);

    expect(stderrSpy).toHaveBeenCalledWith(expect.stringContaining('has insecure permissions'));
    stderrSpy.mockRestore();
  });
});
