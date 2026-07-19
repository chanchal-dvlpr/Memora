import * as fs from 'fs';
import * as path from 'path';
import { Logger } from '../src/logger/logger';
import { Pipeline } from '../src/utils/middleware';
import { ExecutionContext } from '../src/models/context';
import { ValidationError } from '../src/errors/errors';
import {
  validateDirectoryExists,
  validateFileExists,
  validatePathReadable,
} from '../src/validators/validator';
import { dispatcher } from '../src/commands/dispatcher';

describe('Logger Engine', () => {
  let stderrSpy: jest.SpyInstance;

  beforeEach(() => {
    stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should format logs as JSON when json flag is enabled', () => {
    const logger = new Logger({ json: true });
    logger.info('test info');
    expect(stderrSpy).toHaveBeenCalled();
    const parsed = JSON.parse(stderrSpy.mock.calls[0][0]);
    expect(parsed.message).toBe('test info');
    expect(parsed.level).toBe('INFO');
  });

  it('should print colored output on text mode', () => {
    const logger = new Logger({ verbose: true });
    logger.debug('test debug');
    logger.trace('test trace');
    logger.error('test error');
    logger.warn('test warn');
    logger.info('test info');
    expect(stderrSpy).toHaveBeenCalledTimes(5);
  });

  it('should suppress logs when quiet is enabled', () => {
    const logger = new Logger({ quiet: true });
    logger.info('suppress this');
    expect(stderrSpy).not.toHaveBeenCalled();
  });
});

describe('Pipeline Onion Model', () => {
  it('should execute middleware in onion sequence', async () => {
    const order: number[] = [];
    const pipeline = new Pipeline();

    pipeline.use({
      name: 'm1',
      execute: async (_ctx, next) => {
        order.push(1);
        await next();
        order.push(6);
      },
    });

    pipeline.use({
      name: 'm2',
      execute: async (_ctx, next) => {
        order.push(2);
        await next();
        order.push(5);
      },
    });

    const mockCtx = {} as ExecutionContext;
    await pipeline.run(mockCtx, () => {
      order.push(3);
      order.push(4);
    });

    expect(order).toEqual([1, 2, 3, 4, 5, 6]);
  });
});

describe('Validation Framework', () => {
  const tempDir = path.resolve(__dirname, 'temp-val-dir');
  const tempFile = path.resolve(tempDir, 'file.txt');

  beforeAll(() => {
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir);
    }
    fs.writeFileSync(tempFile, 'content');
  });

  afterAll(() => {
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile);
    }
    if (fs.existsSync(tempDir)) {
      fs.rmdirSync(tempDir);
    }
  });

  it('should validate directory exists', () => {
    expect(() => validateDirectoryExists(tempDir)).not.toThrow();
    expect(() => validateDirectoryExists(tempFile)).toThrow(ValidationError);
    expect(() => validateDirectoryExists('nonexistent')).toThrow(ValidationError);
  });

  it('should validate file exists', () => {
    expect(() => validateFileExists(tempFile)).not.toThrow();
    expect(() => validateFileExists(tempDir)).toThrow(ValidationError);
    expect(() => validateFileExists('nonexistent')).toThrow(ValidationError);
  });

  it('should validate path readable', () => {
    expect(() => validatePathReadable(tempFile)).not.toThrow();
    expect(() => validatePathReadable('nonexistent')).toThrow(ValidationError);
  });
});

describe('Dispatcher Pipeline Errors', () => {
  let stderrSpy: jest.SpyInstance;

  beforeEach(() => {
    stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should intercept validation errors and return matching exit code', async () => {
    const exitCode = await dispatcher.dispatch('test-cmd', [], {}, () => {
      throw new ValidationError('Overlap error');
    });
    expect(exitCode).toBe(2);
    expect(stderrSpy).toHaveBeenCalled();
  });

  it('should handle unexpected errors and return exit code 1', async () => {
    const exitCode = await dispatcher.dispatch('test-cmd', [], {}, () => {
      throw new Error('Fatal database lock');
    });
    expect(exitCode).toBe(1);
    expect(stderrSpy).toHaveBeenCalled();
  });
});
