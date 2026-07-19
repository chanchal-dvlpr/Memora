export interface VersionInfo {
  readonly version: string;
  readonly major: number;
  readonly minor: number;
  readonly patch: number;
  readonly prerelease?: string;
  readonly buildMetadata?: string;
}

export class VersionProvider {
  private static readonly info: VersionInfo = {
    version: '1.0.0',
    major: 1,
    minor: 0,
    patch: 0,
  };

  public static getVersionInfo(): VersionInfo {
    return this.info;
  }

  public static getVersionString(): string {
    return this.info.version;
  }
}
