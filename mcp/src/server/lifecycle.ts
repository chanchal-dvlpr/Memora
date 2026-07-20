import { LifecycleTransitionError } from '../errors';

export enum LifecycleState {
  UNINITIALIZED = 'UNINITIALIZED',
  INITIALIZED = 'INITIALIZED',
  STARTING = 'STARTING',
  STARTED = 'STARTED',
  STOPPING = 'STOPPING',
  STOPPED = 'STOPPED',
}

export class LifecycleManager {
  private state: LifecycleState = LifecycleState.UNINITIALIZED;

  /**
   * Returns the current lifecycle state.
   */
  public getState(): LifecycleState {
    return this.state;
  }

  /**
   * Transitions to INITIALIZED if currently UNINITIALIZED.
   */
  public initialize(initFn: () => void): void {
    if (this.state !== LifecycleState.UNINITIALIZED) {
      throw new LifecycleTransitionError(`Server is already in state "${this.state}"`);
    }
    initFn();
    this.state = LifecycleState.INITIALIZED;
  }

  /**
   * Transitions to STARTED via STARTING.
   */
  public async start(startFn: () => Promise<void>): Promise<void> {
    if (this.state === LifecycleState.UNINITIALIZED) {
      throw new LifecycleTransitionError('Server is UNINITIALIZED. Call initialize() first.');
    }
    if (this.state === LifecycleState.STARTING || this.state === LifecycleState.STARTED) {
      throw new LifecycleTransitionError(`Server is already in state "${this.state}"`);
    }

    const previousState = this.state;
    this.state = LifecycleState.STARTING;
    try {
      await startFn();
      this.state = LifecycleState.STARTED;
    } catch (err) {
      this.state = previousState;
      throw err;
    }
  }

  /**
   * Transitions to STOPPED via STOPPING.
   */
  public async stop(stopFn: () => Promise<void>): Promise<void> {
    if (this.state === LifecycleState.STOPPED || this.state === LifecycleState.UNINITIALIZED) {
      throw new LifecycleTransitionError(`Server is already in state "${this.state}"`);
    }
    if (this.state === LifecycleState.STOPPING) {
      throw new LifecycleTransitionError('Server is already in state "STOPPING"');
    }

    this.state = LifecycleState.STOPPING;
    try {
      await stopFn();
      this.state = LifecycleState.STOPPED;
    } catch (err) {
      this.state = LifecycleState.STOPPED;
      throw err;
    }
  }

  /**
   * Performs graceful stop, resets state, and starts the server again.
   */
  public async restart(
    stopFn: () => Promise<void>,
    initFn: () => void,
    startFn: () => Promise<void>,
  ): Promise<void> {
    if (this.state !== LifecycleState.UNINITIALIZED && this.state !== LifecycleState.STOPPED) {
      await this.stop(stopFn);
    }
    
    this.state = LifecycleState.UNINITIALIZED;
    this.initialize(initFn);
    await this.start(startFn);
  }
}
