import { UpdateFramework } from '../src/utils/updateFramework';
import { VersionProvider } from '../src/utils/versionProvider';

describe('UpdateFramework', () => {
  it('should return correct current and latest version placeholders', () => {
    const result = UpdateFramework.checkUpdate('stable');
    expect(result).toBeDefined();
    expect(result.currentVersion).toBe(VersionProvider.getVersionString());
    expect(result.latestVersion).toBe('1.0.0');
    expect(result.updateAvailable).toBe(false);
    expect(result.channel).toBe('stable');
  });

  it('should support checking update status across channels', () => {
    const betaResult = UpdateFramework.checkUpdate('beta');
    expect(betaResult.channel).toBe('beta');
    expect(betaResult.updateAvailable).toBe(false);
  });
});
