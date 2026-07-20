import { Session, SessionId, SessionMetadata, SessionContext, SessionAttributes, SessionState } from '../types/session';
import { DuplicateSessionError, SessionNotFoundError } from '../errors';

function deepFreeze<T extends object>(obj: T): T {
  Object.freeze(obj);
  Object.getOwnPropertyNames(obj).forEach((prop) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const val = (obj as any)[prop];
    if (val !== null && (typeof val === 'object' || typeof val === 'function') && !Object.isFrozen(val)) {
      deepFreeze(val);
    }
  });
  return obj;
}

export class SessionRegistry {
  private readonly sessions = new Map<SessionId, Session>();

  public createSession(
    id: SessionId,
    metadata: SessionMetadata,
    attributes: SessionAttributes = {},
    context: SessionContext = {},
    state: SessionState = 'active'
  ): Session {
    if (this.sessions.has(id)) {
      throw new DuplicateSessionError(`Session with ID "${id}" already exists.`);
    }

    const session: Session = {
      id,
      state,
      metadata,
      attributes: { ...attributes },
      context: { ...context },
      lastAccessedAt: Date.now(),
    };

    // Deep freeze session to ensure immutability when returned to clients
    const frozenSession = deepFreeze(session);
    this.sessions.set(id, frozenSession);
    return frozenSession;
  }

  public getSession(id: SessionId): Session | undefined {
    return this.sessions.get(id);
  }

  public updateSession(id: SessionId, updates: Partial<Pick<Session, 'state' | 'attributes' | 'context' | 'lastAccessedAt'>>): Session {
    const existing = this.sessions.get(id);
    if (!existing) {
      throw new SessionNotFoundError(`Session "${id}" does not exist.`);
    }

    const newAttributes = updates.attributes 
      ? { ...existing.attributes, ...updates.attributes }
      : existing.attributes;

    const newContext = updates.context
      ? { ...existing.context, ...updates.context }
      : existing.context;

    const updatedSession: Session = {
      id: existing.id,
      state: updates.state !== undefined ? updates.state : existing.state,
      metadata: existing.metadata, // metadata is fully immutable
      attributes: newAttributes,
      context: newContext,
      lastAccessedAt: updates.lastAccessedAt !== undefined ? updates.lastAccessedAt : Date.now(),
    };

    const frozenSession = deepFreeze(updatedSession);
    this.sessions.set(id, frozenSession);
    return frozenSession;
  }

  public removeSession(id: SessionId): void {
    if (!this.sessions.has(id)) {
      throw new SessionNotFoundError(`Session "${id}" does not exist.`);
    }
    this.sessions.delete(id);
  }

  public hasSession(id: SessionId): boolean {
    return this.sessions.has(id);
  }

  public listSessions(): ReadonlyArray<Session> {
    const list = Array.from(this.sessions.values())
      .sort((a, b) => {
        // Deterministic ordering: sort primarily by createdAt timestamp, secondary by ID alphabetically
        const timeDiff = a.metadata.createdAt - b.metadata.createdAt;
        if (timeDiff !== 0) {
          return timeDiff;
        }
        return a.id.localeCompare(b.id);
      });
    return Object.freeze(list);
  }

  public clear(): void {
    this.sessions.clear();
  }
}
