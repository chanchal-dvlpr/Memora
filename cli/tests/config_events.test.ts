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
import { saveConfigFile } from '../src/config/storage';
import { ConfigurationError } from '../src/errors/errors';
import { ConfigEvent } from '../src/config/events';

describe('Configuration Events & Failure Verifications', () => {
  const tempDir = path.resolve(__dirname, 'temp-evt-dir');
  const tempFile = path.resolve(tempDir, 'config.json');

  let service: ConfigService;
  let eventHistory: ConfigEvent[];

  beforeAll(() => {
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir);
    }
  });

  afterAll(() => {
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile);
    }
    if (fs.existsSync(tempDir)) {
      fs.rmdirSync(tempDir);
    }
  });

  beforeEach(() => {
    delete process.env.MEMORA_CONFIG;
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile);
    }

    eventHistory = [];
    service = new ConfigService(
      new ConfigLoader([
        new DefaultConfigSource(),
        new FileConfigSource(),
        new EnvConfigSource(),
        new CliOptionConfigSource(),
      ]),
    );

    service.events.subscribe((evt) => {
      eventHistory.push(evt);
    });
  });

  it('should emit ConfigurationLoaded event on load', async () => {
    await service.load();
    expect(eventHistory).toHaveLength(1);
    expect(eventHistory[0].type).toBe('ConfigurationLoaded');
  });

  it('should emit Reload, Update, and Invalidate events during service activities', async () => {
    fs.writeFileSync(tempFile, '{}');
    await service.load({ config: tempFile });

    await service.updateFile({ backend: { timeoutMs: 3000 } });
    await service.reload({ config: tempFile });

    const types = eventHistory.map((e) => e.type);
    expect(types).toContain('ConfigurationUpdated');
    expect(types).toContain('ConfigurationCacheInvalidated');
    expect(types).toContain('ConfigurationReloaded');
  });

  it('should emit ValidationFailed on corrupt settings updates', async () => {
    fs.writeFileSync(tempFile, '{}');
    await service.load({ config: tempFile });

    await expect(service.updateFile({ backend: { timeoutMs: -100 } })).rejects.toThrow(
      ConfigurationError,
    );

    const types = eventHistory.map((e) => e.type);
    expect(types).toContain('ConfigurationValidationFailed');
  });

  it('should auto-create parent directories recursively when path is missing', () => {
    const missingSubDir = path.resolve(tempDir, 'nested-subdir', 'config.json');

    expect(() => saveConfigFile(missingSubDir, '{}')).not.toThrow();
    expect(fs.existsSync(missingSubDir)).toBe(true);

    if (fs.existsSync(missingSubDir)) {
      fs.unlinkSync(missingSubDir);
      fs.rmdirSync(path.dirname(missingSubDir));
    }
  });

  it('should raise ConfigurationError on malformed JSON settings files', async () => {
    fs.writeFileSync(tempFile, '{ malformed json content }');
    await expect(service.load({ config: tempFile })).rejects.toThrow(ConfigurationError);
  });
});
