jest.mock('@modelcontextprotocol/sdk/server/stdio.js', () => {
  return {
    StdioServerTransport: jest.fn().mockImplementation(() => {
      return {
        close: jest.fn().mockResolvedValue(undefined),
      };
    }),
  };
});

jest.mock('@modelcontextprotocol/sdk/server/index.js', () => {
  return {
    Server: jest.fn().mockImplementation(() => {
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        close: jest.fn().mockResolvedValue(undefined),
        onerror: jest.fn(),
        onclose: jest.fn(),
      };
    }),
  };
});

// Mock the server bootstrap
jest.mock('../src/server', () => {
  return {
    MemoraMcpServer: jest.fn().mockImplementation(() => {
      return {
        initialize: jest.fn(),
        start: jest.fn().mockResolvedValue(undefined),
        stop: jest.fn().mockResolvedValue(undefined),
        getLogger: jest.fn().mockReturnValue({
          info: jest.fn(),
          warn: jest.fn(),
          error: jest.fn(),
          fatal: jest.fn(),
        }),
      };
    }),
  };
});

import { bootstrap, resetShutdownState } from '../src/index';

describe('Shutdown Signal Traps', () => {
  let processExitSpy: jest.SpyInstance;
  let processOnSpy: jest.SpyInstance;
  const registeredListeners: Record<string, (...args: unknown[]) => unknown> = {};

  beforeAll(() => {
    processExitSpy = jest.spyOn(process, 'exit').mockImplementation(() => neverExits());
    processOnSpy = jest.spyOn(process, 'on').mockImplementation((event, handler) => {
      registeredListeners[event.toString()] = handler;
      return process;
    });
  });

  afterAll(() => {
    processExitSpy.mockRestore();
    processOnSpy.mockRestore();
  });

  beforeEach(() => {
    processExitSpy.mockClear();
    jest.clearAllMocks();
    resetShutdownState();
    for (const key in registeredListeners) {
      delete registeredListeners[key];
    }
  });

  function neverExits(): never {
    throw new Error('Process exited');
  }

  it('should intercept signals and perform graceful stops', async () => {
    // Run the bootstrap to register listeners
    await bootstrap();

    expect(registeredListeners['SIGINT']).toBeDefined();
    
    // Call the registered SIGINT handler directly
    const sigintHandler = registeredListeners['SIGINT'];
    await expect(sigintHandler()).rejects.toThrow('Process exited');

    expect(processExitSpy).toHaveBeenCalledWith(0);
  });

  it('should support exception hooks and fail shutdown with exit code 1', async () => {
    // Run the bootstrap to register listeners
    await bootstrap();

    expect(registeredListeners['uncaughtException']).toBeDefined();
    
    // Call the registered uncaughtException handler directly
    const uncaughtHandler = registeredListeners['uncaughtException'];
    await expect(uncaughtHandler(new Error('Fatal error'))).rejects.toThrow('Process exited');

    expect(processExitSpy).toHaveBeenCalledWith(1);
  });
});
