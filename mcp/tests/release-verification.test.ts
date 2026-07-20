import * as fs from 'fs';
import * as path from 'path';
import { ConfigLoader } from '../src/config';

describe('Release & Packaging Verification', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  describe('1. Versioning Framework', () => {
    it('should read version from VERSION file', () => {
      const versionPath = path.resolve(__dirname, '../VERSION');
      expect(fs.existsSync(versionPath)).toBe(true);

      const versionContent = fs.readFileSync(versionPath, 'utf-8').trim();
      expect(versionContent).toMatch(/^\d+\.\d+\.\d+/);

      const config = ConfigLoader.load();
      expect(config.version).toBe(versionContent);
      expect(config.buildMetadata).toBeDefined();
      expect(config.releaseMetadata).toBeDefined();
    });

    it('should allow override via MEMORA_MCP_SERVER_VERSION environment variable', () => {
      process.env.MEMORA_MCP_SERVER_VERSION = '2.0.0-rc1';
      const config = ConfigLoader.load();
      expect(config.version).toBe('2.0.0-rc1');
    });
  });

  describe('2. Release Configuration Profiles', () => {
    it('should configure development profile correctly', () => {
      process.env.NODE_ENV = 'development';
      delete process.env.MEMORA_MCP_SERVER_PORT;
      const config = ConfigLoader.load();
      expect(config.environment).toBe('development');
      expect(config.port).toBe(8081);
      expect(config.logLevel).toBe('info');
    });

    it('should configure test profile correctly', () => {
      process.env.NODE_ENV = 'test';
      delete process.env.MEMORA_MCP_SERVER_PORT;
      const config = ConfigLoader.load();
      expect(config.environment).toBe('test');
      expect(config.port).toBe(8082);
      expect(config.logLevel).toBe('error');
      expect(config.requestTimeoutMs).toBe(30000);
      expect(config.maxConcurrentRequests).toBe(50);
    });

    it('should configure staging profile correctly', () => {
      process.env.NODE_ENV = 'staging';
      delete process.env.MEMORA_MCP_SERVER_PORT;
      const config = ConfigLoader.load();
      expect(config.environment).toBe('staging');
      expect(config.port).toBe(8080);
      expect(config.logLevel).toBe('info');
      expect(config.maxConcurrentRequests).toBe(100);
    });

    it('should configure production profile correctly', () => {
      process.env.NODE_ENV = 'production';
      delete process.env.MEMORA_MCP_SERVER_PORT;
      const config = ConfigLoader.load();
      expect(config.environment).toBe('production');
      expect(config.port).toBe(8080);
      expect(config.logLevel).toBe('info');
      expect(config.maxConcurrentRequests).toBe(200);
    });
  });

  describe('3. Distribution & Package Structure Verification', () => {
    it('should verify all distribution packaging files exist', () => {
      const baseDir = path.resolve(__dirname, '..');
      const requiredFiles = [
        'VERSION',
        'package.json',
        'LICENSE',
        'NOTICE',
        'CHANGELOG.md',
        'CONTRIBUTING.md',
        'SECURITY.md',
        'CODE_OF_CONDUCT.md',
        'SUPPORTED_VERSIONS.md',
        'Dockerfile',
        '.dockerignore',
        'docker-compose.yml',
      ];

      for (const file of requiredFiles) {
        const filePath = path.join(baseDir, file);
        expect(fs.existsSync(filePath)).toBe(true);
      }
    });

    it('should verify package.json metadata completeness', () => {
      const pkgPath = path.resolve(__dirname, '../package.json');
      const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));

      expect(pkg.name).toBe('memora-mcp');
      expect(pkg.version).toBeDefined();
      expect(pkg.main).toBe('dist/index.js');
      expect(pkg.types).toBe('dist/index.d.ts');
      expect(pkg.bin['memora-mcp']).toBe('dist/index.js');
      expect(pkg.files).toContain('dist');
      expect(pkg.files).toContain('VERSION');
      expect(pkg.license).toBe('MIT');
      expect(pkg.engines.node).toBeDefined();
    });
  });
});
