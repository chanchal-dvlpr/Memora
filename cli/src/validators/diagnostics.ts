import { ValidationError } from '../errors/errors';

/**
 * Validates diagnostic check suite category strings.
 */
export function validateDiagnosticType(type: string): void {
  const allowed = ['health', 'config', 'connectivity', 'environment', 'report'];
  if (!allowed.includes(type)) {
    throw new ValidationError(`Unknown diagnostics run type: ${type}`);
  }
}

/**
 * Validates ExecutionContext environmental entries parameter values.
 */
export function validateEnvironmentData(env: Record<string, string | undefined>): void {
  if (!env) {
    throw new ValidationError('Environment parameters cannot be null or undefined.');
  }
}
