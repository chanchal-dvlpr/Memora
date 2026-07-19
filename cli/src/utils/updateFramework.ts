import { VersionProvider } from './versionProvider';

export interface UpdateCheckResult {
  readonly currentVersion: string;
  readonly latestVersion: string;
  readonly updateAvailable: boolean;
  readonly channel: 'stable' | 'beta' | 'alpha';
}

export class UpdateFramework {
  private static readonly LATEST_VERSION_PLACEHOLDER = '1.0.0';

  /**
   * Performs check to determine if updates are available.
   */
  public static checkUpdate(channel: 'stable' | 'beta' | 'alpha' = 'stable'): UpdateCheckResult {
    const current = VersionProvider.getVersionString();
    const latest = this.LATEST_VERSION_PLACEHOLDER;

    // A basic check comparing current against latest
    const updateAvailable = current !== latest && !current.startsWith('1.0.0');

    return {
      currentVersion: current,
      latestVersion: latest,
      updateAvailable,
      channel,
    };
  }
}
