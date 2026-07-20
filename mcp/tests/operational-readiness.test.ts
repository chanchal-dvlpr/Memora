import * as fs from 'fs';
import * as path from 'path';

describe('Operational Readiness & Deployment Validation', () => {
  const rootDir = path.resolve(__dirname, '../../');
  const mcpDir = path.resolve(__dirname, '../');

  describe('1. CI/CD Workflow Files', () => {
    it('should verify CI and release GitHub Actions workflow files exist', () => {
      const ciPath = path.join(rootDir, '.github/workflows/ci.yml');
      const releasePath = path.join(rootDir, '.github/workflows/release.yml');

      expect(fs.existsSync(ciPath)).toBe(true);
      expect(fs.existsSync(releasePath)).toBe(true);

      const ciContent = fs.readFileSync(ciPath, 'utf-8');
      expect(ciContent).toContain('npm run typecheck');
      expect(ciContent).toContain('npm run lint');
      expect(ciContent).toContain('npm test');
    });
  });

  describe('2. Operational Documentation & Runbooks', () => {
    it('should verify all required operational docs exist in mcp/docs and root docs/', () => {
      const requiredDocs = [
        'runbook.md',
        'upgrade_guide.md',
        'release_checklist.md',
        'cicd.md',
        'installation.md',
        'deployment.md',
        'release_process.md',
        'configuration_reference.md',
        'production_readiness_report.md',
        'project_completion_report.md',
        'release_manifest.md',
      ];

      for (const doc of requiredDocs) {
        const mcpDocPath = path.join(mcpDir, 'docs', doc);
        const rootDocPath = path.join(rootDir, 'docs', doc);
        expect(fs.existsSync(mcpDocPath)).toBe(true);
        expect(fs.existsSync(rootDocPath)).toBe(true);
      }
    });
  });

  describe('3. Docker Artifact Static Validation', () => {
    it('should validate Dockerfile security and healthcheck rules', () => {
      const dockerfilePath = path.join(mcpDir, 'Dockerfile');
      expect(fs.existsSync(dockerfilePath)).toBe(true);

      const content = fs.readFileSync(dockerfilePath, 'utf-8');
      expect(content).toContain('USER node');
      expect(content).toContain('HEALTHCHECK');
      expect(content).toContain('EXPOSE 8080');
    });

    it('should validate docker-compose.yml configuration', () => {
      const composePath = path.join(mcpDir, 'docker-compose.yml');
      expect(fs.existsSync(composePath)).toBe(true);

      const content = fs.readFileSync(composePath, 'utf-8');
      expect(content).toContain('memora-mcp-prod');
      expect(content).toContain('memora-mcp-dev');
      expect(content).toContain('8080:8080');
    });
  });

  describe('4. CLI Entry Point & Package Manifest Validation', () => {
    it('should verify package.json bin entry point is correctly specified', () => {
      const pkgPath = path.join(mcpDir, 'package.json');
      const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));

      expect(pkg.bin).toBeDefined();
      expect(pkg.bin['memora-mcp']).toBe('dist/index.js');
    });
  });
});
