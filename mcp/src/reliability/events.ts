import { ReliabilityEvent, ReliabilityEventType } from '../types/reliability';
export { ReliabilityEvent, ReliabilityEventType };

export type ReliabilityEventListener = (event: ReliabilityEvent) => void | Promise<void>;

export class ReliabilityEventEmitter {
  private readonly listeners = new Map<ReliabilityEventType | '*', Set<ReliabilityEventListener>>();

  public on(type: ReliabilityEventType | '*', listener: ReliabilityEventListener): () => void {
    let set = this.listeners.get(type);
    if (!set) {
      set = new Set<ReliabilityEventListener>();
      this.listeners.set(type, set);
    }
    set.add(listener);

    return () => {
      set?.delete(listener);
    };
  }

  public off(type: ReliabilityEventType | '*', listener: ReliabilityEventListener): void {
    const set = this.listeners.get(type);
    if (set) {
      set.delete(listener);
    }
  }

  public async emit(event: ReliabilityEvent): Promise<void> {
    const specificListeners = this.listeners.get(event.type);
    const wildcardListeners = this.listeners.get('*');

    const promises: Promise<void>[] = [];

    const invoke = (listener: ReliabilityEventListener) => {
      try {
        const res = listener(event);
        if (res instanceof Promise) {
          promises.push(res);
        }
      } catch (err) {
        console.error('Error in reliability event listener:', err);
      }
    };

    if (specificListeners) {
      specificListeners.forEach(invoke);
    }
    if (wildcardListeners) {
      wildcardListeners.forEach(invoke);
    }

    if (promises.length > 0) {
      await Promise.allSettled(promises);
    }
  }

  public clear(): void {
    this.listeners.clear();
  }
}
