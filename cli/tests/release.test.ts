import * as fs from 'fs';
import * as path from 'path';
import { ReleaseManager } from '../src/utils/releaseManager';
import { commandEventPublisher } from '../src/events/commandEvents';

describe('ReleaseManager Validation & Verification', () => {
  const mockPkgDir = path.join(__dirname, 'mock_release_dir');

  beforeAll(() => {
    if (!fs.existsSync(mockPkgDir)) {
      fs.mkdirSync(mockPkgDir, { recursive: true });
    }
  });

  afterAll(() => {
    if (fs.existsSync(mockPkgDir)) {
      fs.rmSync(mockPkgDir, { recursive: true, force: true });
    }
  });

  it('should fail release validation when required files are missing', () => {
    const success = ReleaseManager.validateRelease(mockPkgDir);
    expect(success).toBe(false);
  });

  it('should pass release validation when manifest and required files are present', () => {
    const requiredFiles = ['package.json', 'README.md', 'LICENSE', 'CHANGELOG.md'];
    for (const file of requiredFiles) {
      if (file === 'package.json') {
        fs.writeFileSync(
          path.join(mockPkgDir, file),
          JSON.stringify({ name: 'memora-cli', version: '1.0.0' })
        );
      } else {
        fs.writeFileSync(path.join(mockPkgDir, file), 'content');
      }
    }

    const success = ReleaseManager.validateRelease(mockPkgDir);
    expect(success).toBe(true);
  });

  it('should generate valid release metadata matching current version', () => {
    const dummyTarball = path.join(mockPkgDir, 'dummy.tgz');
    fs.writeFileSync(dummyTarball, 'tarball content');

    const metadata = ReleaseManager.generateMetadata(dummyTarball);
    expect(metadata).toBeDefined();
    expect(metadata.checksum).toBeDefined();
    expect(metadata.version).toContain('1.0.0');
    expect(metadata.supportedPlatforms).toContain('darwin');
  });

  it('should verify packages and publish verification events', () => {
    const dummyTarball = path.join(mockPkgDir, 'dummy.tgz');
    fs.writeFileSync(dummyTarball, 'tarball content');

    const metadata = ReleaseManager.generateMetadata(dummyTarball);

    let eventType = '';
    const unsubscribe = commandEventPublisher.subscribe({
      onEvent(e) {
        eventType = e.type;
      }
    });

    const verified = ReleaseManager.verifyPackage(dummyTarball, metadata.checksum);
    unsubscribe();

    expect(verified).toBe(true);
    expect(eventType).toBe('PackageVerified');
  });
});
