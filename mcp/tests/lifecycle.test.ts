import { LifecycleManager, LifecycleState } from '../src/server/lifecycle';

describe('LifecycleManager', () => {
  let manager: LifecycleManager;

  beforeEach(() => {
    manager = new LifecycleManager();
  });

  it('should start in UNINITIALIZED state', () => {
    expect(manager.getState()).toBe(LifecycleState.UNINITIALIZED);
  });

  it('should transition to INITIALIZED on initialize()', () => {
    const initSpy = jest.fn();
    manager.initialize(initSpy);

    expect(initSpy).toHaveBeenCalledTimes(1);
    expect(manager.getState()).toBe(LifecycleState.INITIALIZED);
  });

  it('should throw on duplicate initialize()', () => {
    manager.initialize(() => {});
    expect(() => manager.initialize(() => {})).toThrow(
      'Server is already in state "INITIALIZED"'
    );
  });

  it('should transition to STARTED on start()', async () => {
    manager.initialize(() => {});

    const startSpy = jest.fn().mockResolvedValue(undefined);
    await manager.start(startSpy);

    expect(startSpy).toHaveBeenCalledTimes(1);
    expect(manager.getState()).toBe(LifecycleState.STARTED);
  });

  it('should revert state if start() throws error', async () => {
    manager.initialize(() => {});

    const startSpy = jest.fn().mockRejectedValue(new Error('Start Fail'));
    await expect(manager.start(startSpy)).rejects.toThrow('Start Fail');

    expect(manager.getState()).toBe(LifecycleState.INITIALIZED);
  });

  it('should throw when starting uninitialized server', async () => {
    await expect(manager.start(async () => {})).rejects.toThrow(
      'Server is UNINITIALIZED. Call initialize() first.'
    );
  });

  it('should throw on duplicate start()', async () => {
    manager.initialize(() => {});
    await manager.start(async () => {});

    await expect(manager.start(async () => {})).rejects.toThrow(
      'Server is already in state "STARTED"'
    );
  });

  it('should transition to STOPPED on stop()', async () => {
    manager.initialize(() => {});
    await manager.start(async () => {});

    const stopSpy = jest.fn().mockResolvedValue(undefined);
    await manager.stop(stopSpy);

    expect(stopSpy).toHaveBeenCalledTimes(1);
    expect(manager.getState()).toBe(LifecycleState.STOPPED);
  });

  it('should throw when stopping stopped or uninitialized server', async () => {
    await expect(manager.stop(async () => {})).rejects.toThrow(
      'Server is already in state "UNINITIALIZED"'
    );

    manager.initialize(() => {});
    await manager.start(async () => {});
    await manager.stop(async () => {});

    await expect(manager.stop(async () => {})).rejects.toThrow(
      'Server is already in state "STOPPED"'
    );
  });

  it('should restart successfully from active state', async () => {
    manager.initialize(() => {});
    await manager.start(async () => {});

    const stopSpy = jest.fn().mockResolvedValue(undefined);
    const initSpy = jest.fn();
    const startSpy = jest.fn().mockResolvedValue(undefined);

    await manager.restart(stopSpy, initSpy, startSpy);

    expect(stopSpy).toHaveBeenCalledTimes(1);
    expect(initSpy).toHaveBeenCalledTimes(1);
    expect(startSpy).toHaveBeenCalledTimes(1);
    expect(manager.getState()).toBe(LifecycleState.STARTED);
  });
});
