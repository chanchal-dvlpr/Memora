import * as fs from 'fs';
import * as path from 'path';
import { ValidationError } from '../errors/errors';

/**
 * Validates directory existence and types.
 */
export function validateDirectoryExists(dirPath: string): void {
  const resolved = path.resolve(dirPath);
  if (!fs.existsSync(resolved)) {
    throw new ValidationError(`Error: Directory path "${dirPath}" does not exist.`);
  }
  const stat = fs.statSync(resolved);
  if (!stat.isDirectory()) {
    throw new ValidationError(`Error: Path "${dirPath}" is not a directory.`);
  }
}

/**
 * Validates file existence and types.
 */
export function validateFileExists(filePath: string): void {
  const resolved = path.resolve(filePath);
  if (!fs.existsSync(resolved)) {
    throw new ValidationError(`Error: File path "${filePath}" does not exist.`);
  }
  const stat = fs.statSync(resolved);
  if (!stat.isFile()) {
    throw new ValidationError(`Error: Path "${filePath}" is not a file.`);
  }
}

/**
 * Validates filesystem read permissions.
 */
export function validatePathReadable(targetPath: string): void {
  const resolved = path.resolve(targetPath);
  try {
    fs.accessSync(resolved, fs.constants.R_OK);
  } catch (err) {
    throw new ValidationError(`Error: Path "${targetPath}" is not readable.`);
  }
}
