import * as fs from 'fs';
import * as path from 'path';
import { ConfigLoader } from '../src/config/loader';
import {
  DefaultConfigSource,
  FileConfigSource,
  EnvConfigSource,
  CliOptionConfigSource,
} from '../src/config/source';
import { ConfigurationError } from '../src/errors/errors';

describe('Configuration Loader Engine', () => {
  const tempConfigDir = path.resolve(__dirname, 'temp-config-dir');
  const tempConfigFile = path.resolve(tempConfigDir, 'config.json');

  let loader: ConfigLoader;

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
    delete process.env.MEMORA_BACKEND_URL;
    delete process.env.MEMORA_LOG_LEVEL;
    delete process.env.MEMORA_OUTPUT;
    delete process.env.MEMORA_WORKSPACE;

    loader = new ConfigLoader([
      new DefaultConfigSource(),
      new FileConfigSource(),
      new EnvConfigSource(),
      new CliOptionConfigSource(),
    ]);
  });

  it('should load default parameters when no config file or env is present', async () => {
    const config = await loader.load();
    expect(config.backend.url).toBe('http://localhost:8080');
    expect(config.logging.level).toBe('INFO');
    expect(config.output.mode).toBe('text');
  });

  it('should override configuration parameters with environment variables', async () => {
    process.env.MEMORA_BACKEND_URL = 'http://localhost:9000';
    process.env.MEMORA_LOG_LEVEL = 'WARN';
    process.env.MEMORA_OUTPUT = 'json';

    const config = await loader.load();
    expect(config.backend.url).toBe('http://localhost:9000');
    expect(config.logging.level).toBe('WARN');
    expect(config.output.mode).toBe('json');
  });

  it('should override configuration parameters with CLI options', async () => {
    const config = await loader.load({
      json: true,
      verbose: true,
      quiet: true,
    });
    expect(config.output.mode).toBe('json');
    expect(config.logging.level).toBe('ERROR');
  });

  it('should merge JSON configuration file properties successfully', async () => {
    const validJson = {
      backend: {
        url: 'http://my-host:1234',
        timeoutMs: 5000,
      },
      workspace: {
        path: tempConfigDir,
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(validJson));

    const config = await loader.load({ config: tempConfigFile });
    expect(config.backend.url).toBe('http://my-host:1234');
    expect(config.backend.timeoutMs).toBe(5000);
    expect(config.workspace.path).toBe(tempConfigDir);
  });

  it('should reject JSON configuration files with unknown sections or keys', async () => {
    const badJson = {
      invalidSection: {
        url: 'http://test',
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(badJson));

    await expect(loader.load({ config: tempConfigFile })).rejects.toThrow(ConfigurationError);
  });

  it('should reject configuration with invalid backend URL format', async () => {
    const badJson = {
      backend: {
        url: 'not-a-valid-url',
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(badJson));

    await expect(loader.load({ config: tempConfigFile })).rejects.toThrow(ConfigurationError);
  });

  it('should reject configuration with negative timeout values', async () => {
    const badJson = {
      backend: {
        timeoutMs: -500,
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(badJson));

    await expect(loader.load({ config: tempConfigFile })).rejects.toThrow(ConfigurationError);
  });

  it('should reject configuration with invalid log level values', async () => {
    const badJson = {
      logging: {
        level: 'NOT_A_LEVEL',
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(badJson));

    await expect(loader.load({ config: tempConfigFile })).rejects.toThrow(ConfigurationError);
  });

  it('should reject configuration with missing workspace directory', async () => {
    const badJson = {
      workspace: {
        path: '/nonexistent/path/for/sure',
      },
    };
    fs.writeFileSync(tempConfigFile, JSON.stringify(badJson));

    await expect(loader.load({ config: tempConfigFile })).rejects.toThrow(ConfigurationError);
  });

  it('should deep-freeze configuration objects to enforce absolute immutability', async () => {
    const config = await loader.load();
    expect(Object.isFrozen(config)).toBe(true);
    expect(Object.isFrozen(config.backend)).toBe(true);

    expect(() => {
      (config as unknown as { backend: { url: string } }).backend.url = 'hack';
    }).toThrow();
  });
});
