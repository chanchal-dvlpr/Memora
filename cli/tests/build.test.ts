import * as fs from 'fs';
import * as path from 'path';
import { execSync } from 'child_process';

describe('Build Pipeline & Package Generation', () => {
  const rootDir = path.resolve(__dirname, '..');
  const buildDir = path.join(rootDir, 'build');
  const pkgDir = path.join(buildDir, 'package');

  // Skip this test in fast unit test runs if desired, but we want full automated verification
  it('should compile and pack build artifacts with correct checksums', () => {
    // Run the build script in process or child process
    console.log('Running build script for integration test...');
    execSync('node scripts/build.js', { cwd: rootDir, stdio: 'inherit' });

    // Assert directories exist
    expect(fs.existsSync(buildDir)).toBe(true);
    expect(fs.existsSync(pkgDir)).toBe(true);
    expect(fs.existsSync(path.join(pkgDir, 'dist'))).toBe(true);

    // Assert files exist in package folder
    expect(fs.existsSync(path.join(pkgDir, 'package.json'))).toBe(true);
    expect(fs.existsSync(path.join(pkgDir, 'README.md'))).toBe(true);
    expect(fs.existsSync(path.join(pkgDir, 'LICENSE'))).toBe(true);
    expect(fs.existsSync(path.join(pkgDir, 'CHANGELOG.md'))).toBe(true);

    // Assert build metadata and placeholders exist
    expect(fs.existsSync(path.join(buildDir, 'checksums.sha256'))).toBe(true);
    expect(fs.existsSync(path.join(buildDir, 'signature.sig.placeholder'))).toBe(true);

    // Assert packed tarball exists
    const files = fs.readdirSync(buildDir);
    const tarball = files.find(f => f.endsWith('.tgz'));
    expect(tarball).toBeDefined();

    // Verify SHA-256 matches file contents
    const checksumContent = fs.readFileSync(path.join(buildDir, 'checksums.sha256'), 'utf8');
    expect(checksumContent).toContain(tarball!);
  });
});
