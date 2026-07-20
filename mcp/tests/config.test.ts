import { ConfigLoader } from '../src/config';

describe('ConfigLoader', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('should load default development configuration values', () => {
    delete process.env.NODE_ENV;
    delete process.env.MEMORA_MCP_SERVER_NAME;
    delete process.env.MEMORA_MCP_SERVER_PORT;

    const config = ConfigLoader.load();
    expect(config.environment).toBe('development');
    expect(config.serverName).toBe('memora-mcp-server');
    expect(config.port).toBe(8081);
    expect(config.logLevel).toBe('info');
    expect(config.timeout).toBe(30000);
  });

  it('should adjust defaults for test environment', () => {
    process.env.NODE_ENV = 'test';
    
    const config = ConfigLoader.load();
    expect(config.environment).toBe('test');
    expect(config.port).toBe(8082);
    expect(config.logLevel).toBe('error');
  });

  it('should adjust defaults for production environment', () => {
    process.env.NODE_ENV = 'production';

    const config = ConfigLoader.load();
    expect(config.environment).toBe('production');
    expect(config.port).toBe(8080);
    expect(config.logLevel).toBe('info');
  });

  it('should allow env variable overrides', () => {
    process.env.NODE_ENV = 'production';
    process.env.MEMORA_MCP_SERVER_NAME = 'custom-mcp';
    process.env.MEMORA_MCP_SERVER_PORT = '9090';
    process.env.MEMORA_MCP_SERVER_LOG_LEVEL = 'debug';
    process.env.MEMORA_MCP_SERVER_TIMEOUT = '15000';

    const config = ConfigLoader.load();
    expect(config.serverName).toBe('custom-mcp');
    expect(config.port).toBe(9090);
    expect(config.logLevel).toBe('debug');
    expect(config.timeout).toBe(15000);
  });
});
