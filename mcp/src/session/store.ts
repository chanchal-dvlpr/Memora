import { SessionId } from '../types/session';

export class ContextStore {
  // Map of SessionId to a secondary Map of key-value pairs
  private readonly store = new Map<SessionId, Map<string, unknown>>();

  public put(sessionId: SessionId, key: string, value: unknown): void {
    let sessionMap = this.store.get(sessionId);
    if (!sessionMap) {
      sessionMap = new Map<string, unknown>();
      this.store.set(sessionId, sessionMap);
    }
    sessionMap.set(key, value);
  }

  public get<T = unknown>(sessionId: SessionId, key: string): T | undefined {
    const sessionMap = this.store.get(sessionId);
    if (!sessionMap) {
      return undefined;
    }
    return sessionMap.get(key) as T;
  }

  public remove(sessionId: SessionId, key: string): boolean {
    const sessionMap = this.store.get(sessionId);
    if (!sessionMap) {
      return false;
    }
    const deleted = sessionMap.delete(key);
    if (sessionMap.size === 0) {
      this.store.delete(sessionId);
    }
    return deleted;
  }

  public clear(sessionId?: SessionId): void {
    if (sessionId) {
      this.store.delete(sessionId);
    } else {
      this.store.clear();
    }
  }

  public contains(sessionId: SessionId, key: string): boolean {
    const sessionMap = this.store.get(sessionId);
    if (!sessionMap) {
      return false;
    }
    return sessionMap.has(key);
  }
}
