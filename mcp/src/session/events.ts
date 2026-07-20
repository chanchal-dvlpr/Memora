import { Session, SessionId } from '../types/session';

export type SessionEventType = 
  | 'SessionCreated'
  | 'SessionOpened'
  | 'SessionUpdated'
  | 'SessionTouched'
  | 'SessionExpired'
  | 'SessionClosed'
  | 'SessionRemoved';

export interface SessionEvent {
  readonly type: SessionEventType;
  readonly sessionId: SessionId;
  readonly session?: Session;
  readonly timestamp: number;
  readonly metadata?: Record<string, unknown>;
}

export type SessionEventListener = (event: SessionEvent) => void | Promise<void>;

export class SessionEventEmitter {
  private readonly listeners = new Map<SessionEventType | '*', Set<SessionEventListener>>();

  public on(type: SessionEventType | '*', listener: SessionEventListener): () => void {
    let set = this.listeners.get(type);
    if (!set) {
      set = new Set<SessionEventListener>();
      this.listeners.set(type, set);
    }
    set.add(listener);

    // Return an unsubscribe function
    return () => {
      set?.delete(listener);
    };
  }

  public off(type: SessionEventType | '*', listener: SessionEventListener): void {
    const set = this.listeners.get(type);
    if (set) {
      set.delete(listener);
    }
  }

  public async emit(event: SessionEvent): Promise<void> {
    const specificListeners = this.listeners.get(event.type);
    const wildcardListeners = this.listeners.get('*');

    const promises: Promise<void>[] = [];

    const invoke = (listener: SessionEventListener) => {
      try {
        const res = listener(event);
        if (res instanceof Promise) {
          promises.push(res);
        }
      } catch (err) {
        // Suppress errors during dispatch to ensure all listeners get notified
        console.error('Error in session event listener:', err);
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
