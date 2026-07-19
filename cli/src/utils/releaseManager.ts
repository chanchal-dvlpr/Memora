import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { commandEventPublisher } from '../events/commandEvents';
import { VersionProvider } from './versionProvider';

export interface ReleaseMetadata {
  readonly version: string;
  readonly buildTimestamp: string;
  readonly gitCommit: string;
  readonly checksum: string;
  readonly supportedPlatforms: string[];
  readonly supportedNodeVersions: string;
}

export class ReleaseManager {
  /**
   * Validates a release directory structure and assets.
   */
  public static validateRelease(pkgDir: string): boolean {
    const requiredFiles = ['package.json', 'README.md', 'LICENSE', 'CHANGELOG.md'];
    let success = true;

    for (const file of requiredFiles) {
      if (!fs.existsSync(path.join(pkgDir, file))) {
        success = false;
        break;
      }
    }

    if (success) {
      const manifestPath = path.join(pkgDir, 'package.json');
      try {
        const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8')) as Record<string, unknown>;
        if (!manifest.version || !manifest.name) {
          success = false;
        }
      } catch (err) {
        success = false;
      }
    }

    commandEventPublisher.publish({
      type: 'ReleaseValidated',
      timestamp: new Date(),
      payload: {
        version: VersionProvider.getVersionString(),
        success,
      },
    });

    return success;
  }

  /**
   * Generates release metadata JSON.
   */
  public static generateMetadata(tarballPath: string): ReleaseMetadata {
    const fileBuffer = fs.readFileSync(tarballPath);
    const hash = crypto.createHash('sha256').update(fileBuffer).digest('hex');

    return {
      version: VersionProvider.getVersionString(),
      buildTimestamp: new Date().toISOString(),
      gitCommit: 'GIT_COMMIT_PLACEHOLDER',
      checksum: hash,
      supportedPlatforms: ['darwin', 'linux', 'win32'],
      supportedNodeVersions: '>=18.0.0',
    };
  }

  /**
   * Verifies the generated package tarball content.
   */
  public static verifyPackage(tarballPath: string, expectedHash: string): boolean {
    let checksumMatches = false;
    try {
      if (fs.existsSync(tarballPath)) {
        const fileBuffer = fs.readFileSync(tarballPath);
        const hash = crypto.createHash('sha256').update(fileBuffer).digest('hex');
        checksumMatches = (hash === expectedHash);
      }
    } catch (err) {
      checksumMatches = false;
    }

    commandEventPublisher.publish({
      type: 'PackageVerified',
      timestamp: new Date(),
      payload: {
        tarballPath,
        checksumMatches,
      },
    });

    return checksumMatches;
  }
}
