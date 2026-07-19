import * as path from 'path';
import * as os from 'os';
import * as fs from 'fs';

/**
 * Returns default lookup path for the configuration file.
 */
export function getDefaultConfigPath(): string {
  return path.join(os.homedir(), '.memora', 'config.json');
}

/**
 * Resolves configuration path from CLI arguments, environment, or default fallbacks.
 */
export function getResolvedConfigPath(cliOptions: Record<string, unknown> = {}): string {
  if (process.env.MEMORA_CONFIG) {
    return path.resolve(process.env.MEMORA_CONFIG);
  }
  if (typeof cliOptions.config === 'string') {
    return path.resolve(cliOptions.config);
  }
  return getDefaultConfigPath();
}

/**
 * Checks file permissions on Unix-based systems. Emits console warning if permissions are loose.
 */
export function isFileWritableOnlyByOwner(filePath: string): void {
  if (process.platform === 'win32') return;

  try {
    const stat = fs.statSync(filePath);
    if ((stat.mode & 0o077) !== 0) {
      process.stderr.write(
        `\x1b[1;33m[WARN] Configuration file "${filePath}" has insecure permissions. It is readable/writable by others.\x1b[0m\n`,
      );
    }
  } catch (err) {
    // Ignore stat failures on uninitialized settings files
  }
}

/**
 * Securely writes JSON content to disk, setting owner-only permissions.
 */
export function saveConfigFile(filePath: string, fileContent: string): void {
  const dir = path.dirname(filePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    if (process.platform !== 'win32') {
      fs.chmodSync(dir, 0o700);
    }
  }

  fs.writeFileSync(filePath, fileContent, {
    encoding: 'utf8',
    mode: 0o600,
  });
}
