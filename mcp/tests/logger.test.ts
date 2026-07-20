import { StructuredLogger } from '../src/logging/logger';

describe('StructuredLogger', () => {
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('should emit structured JSON records to console.error', () => {
    const logger = new StructuredLogger('TestComponent', 'info');
    logger.info('Hello World', { userId: 123 });

    expect(consoleErrorSpy).toHaveBeenCalledTimes(1);
    
    const logStr = consoleErrorSpy.mock.calls[0][0];
    const logObj = JSON.parse(logStr);

    expect(logObj.component).toBe('TestComponent');
    expect(logObj.level).toBe('INFO');
    expect(logObj.message).toBe('Hello World');
    expect(logObj.timestamp).toBeDefined();
    expect(logObj.metadata).toEqual({ userId: 123 });
  });

  it('should respect logging levels and filter messages below threshold', () => {
    const logger = new StructuredLogger('TestComponent', 'warn');

    logger.debug('This is debug');
    logger.info('This is info');
    expect(consoleErrorSpy).not.toHaveBeenCalled();

    logger.warn('This is warn');
    logger.error('This is error');
    expect(consoleErrorSpy).toHaveBeenCalledTimes(2);
  });

  it('should format errors with stack traces in logging', () => {
    const logger = new StructuredLogger('TestComponent', 'debug');
    const dummyError = new Error('Test Failure');

    logger.error('Operation failed', dummyError, { retries: 3 });

    expect(consoleErrorSpy).toHaveBeenCalledTimes(1);
    const logObj = JSON.parse(consoleErrorSpy.mock.calls[0][0]);

    expect(logObj.level).toBe('ERROR');
    expect(logObj.message).toBe('Operation failed');
    expect(logObj.stack).toBeDefined();
    expect(logObj.stack).toContain('Test Failure');
    expect(logObj.metadata).toEqual({ retries: 3 });
  });

  it('should use default message from error if message string is empty', () => {
    const logger = new StructuredLogger('TestComponent', 'error');
    const dummyError = new Error('Explicit Error Message');

    logger.error('', dummyError);

    const logObj = JSON.parse(consoleErrorSpy.mock.calls[0][0]);
    expect(logObj.message).toBe('Explicit Error Message');
  });
});
