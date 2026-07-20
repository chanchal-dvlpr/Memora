import { Session, SessionId, SessionStatistics, SessionConfiguration, SessionContext } from '../types/session';
import { SessionRegistry } from './registry';
import { SessionEventEmitter } from './events';
import { ExpirationEvaluator } from './expiration';
import { SessionCleanupManager } from './cleanup';
import { ContextStore } from './store';
import { SessionExpiredError, SessionNotFoundError } from '../errors';

export class SessionManager {
  private readonly registry: SessionRegistry;
  private readonly emitter: SessionEventEmitter;
  private readonly config: SessionConfiguration;
  private readonly evaluator: ExpirationEvaluator;
  private cleanupManager?: SessionCleanupManager;

  // Statistics trackers
  private totalSessionsCreated = 0;
  private expiredSessionsCount = 0;
  private closedSessionsCount = 0;
  private peakActiveSessionsCount = 0;
  private touchCount = 0;
  private readonly sessionLifetimes: number[] = [];

  constructor(
    registry = new SessionRegistry(),
    emitter = new SessionEventEmitter(),
    config: SessionConfiguration = { expirationPolicy: { expirationType: 'none' } },
    evaluator = new ExpirationEvaluator()
  ) {
    this.registry = registry;
    this.emitter = emitter;
    this.config = config;
    this.evaluator = evaluator;
  }

  public getRegistry(): SessionRegistry {
    return this.registry;
  }

  public getEventEmitter(): SessionEventEmitter {
    return this.emitter;
  }

  public getCleanupManager(store?: ContextStore): SessionCleanupManager {
    if (!this.cleanupManager) {
      this.cleanupManager = new SessionCleanupManager(
        this.registry,
        store || new ContextStore(),
        this.config.expirationPolicy,
        this.evaluator
      );
    }
    return this.cleanupManager;
  }

  public async openSession(
    id: SessionId,
    clientMetadata?: Record<string, unknown>,
    attributes?: Record<string, unknown>,
    context?: SessionContext
  ): Promise<Session> {
    const session = this.registry.createSession(
      id,
      {
        createdAt: Date.now(),
        clientMetadata,
      },
      attributes,
      context
    );

    this.totalSessionsCreated++;
    const currentActive = this.registry.listSessions().length;
    if (currentActive > this.peakActiveSessionsCount) {
      this.peakActiveSessionsCount = currentActive;
    }

    await this.emitter.emit({
      type: 'SessionCreated',
      sessionId: id,
      session,
      timestamp: Date.now(),
    });

    await this.emitter.emit({
      type: 'SessionOpened',
      sessionId: id,
      session,
      timestamp: Date.now(),
    });

    return session;
  }

  public async touchSession(id: SessionId): Promise<Session> {
    await this.validateSession(id);
    this.touchCount++;
    const session = this.registry.updateSession(id, { lastAccessedAt: Date.now() });

    await this.emitter.emit({
      type: 'SessionTouched',
      sessionId: id,
      session,
      timestamp: Date.now(),
    });

    return session;
  }

  public async closeSession(id: SessionId): Promise<void> {
    const session = this.registry.getSession(id);
    if (!session) {
      throw new SessionNotFoundError(`Session "${id}" does not exist.`);
    }

    // Transition state
    const updated = this.registry.updateSession(id, { state: 'closed' });
    this.closedSessionsCount++;
    this.sessionLifetimes.push(Date.now() - updated.metadata.createdAt);

    await this.emitter.emit({
      type: 'SessionClosed',
      sessionId: id,
      session: updated,
      timestamp: Date.now(),
    });

    this.registry.removeSession(id);

    await this.emitter.emit({
      type: 'SessionRemoved',
      sessionId: id,
      timestamp: Date.now(),
    });
  }

  public async expireSession(id: SessionId): Promise<void> {
    const session = this.registry.getSession(id);
    if (!session) {
      throw new SessionNotFoundError(`Session "${id}" does not exist.`);
    }

    // Transition state
    const updated = this.registry.updateSession(id, { state: 'expired' });
    this.expiredSessionsCount++;
    this.sessionLifetimes.push(Date.now() - updated.metadata.createdAt);

    await this.emitter.emit({
      type: 'SessionExpired',
      sessionId: id,
      session: updated,
      timestamp: Date.now(),
    });

    this.registry.removeSession(id);

    await this.emitter.emit({
      type: 'SessionRemoved',
      sessionId: id,
      timestamp: Date.now(),
    });
  }

  public async validateSession(id: SessionId): Promise<boolean> {
    const session = this.registry.getSession(id);
    if (!session) {
      throw new SessionNotFoundError(`Session "${id}" does not exist.`);
    }

    if (session.state === 'expired') {
      throw new SessionExpiredError(`Session "${id}" has expired.`);
    }

    if (session.state === 'closed') {
      return false;
    }

    // Delegate expiration evaluation to ExpirationEvaluator
    const evalResult = this.evaluator.evaluate(session, this.config.expirationPolicy);
    if (evalResult.isExpired) {
      await this.expireSession(id);
      throw new SessionExpiredError(`Session "${id}" has expired: ${evalResult.reason || 'Timeout'}`);
    }

    return true;
  }

  public getStatistics(): SessionStatistics {
    const avgDuration = this.sessionLifetimes.length > 0
      ? this.sessionLifetimes.reduce((sum, val) => sum + val, 0) / this.sessionLifetimes.length
      : 0;

    const cleanupStats = this.cleanupManager?.getStatistics();

    return {
      totalSessionsCreated: this.totalSessionsCreated,
      activeSessionsCount: this.registry.listSessions().filter((s) => s.state === 'active').length,
      expiredSessionsCount: this.expiredSessionsCount,
      closedSessionsCount: this.closedSessionsCount,
      peakActiveSessionsCount: this.peakActiveSessionsCount,
      averageSessionDurationMs: avgDuration,
      touchCount: this.touchCount,
      cleanupCount: cleanupStats?.cleanupRuns || 0,
      removedContextsCount: cleanupStats?.removedContextsCount || 0,
    };
  }

  public async updateSessionAttributes(id: SessionId, attributes: Record<string, unknown>): Promise<Session> {
    await this.validateSession(id);
    
    // Check attributes count limit if configured
    const existing = this.registry.getSession(id);
    if (existing && this.config.maxAttributesCount) {
      const mergedKeys = new Set([...Object.keys(existing.attributes), ...Object.keys(attributes)]);
      if (mergedKeys.size > this.config.maxAttributesCount) {
        throw new Error('Max attributes count limit reached.');
      }
    }

    const session = this.registry.updateSession(id, { attributes });

    await this.emitter.emit({
      type: 'SessionUpdated',
      sessionId: id,
      session,
      timestamp: Date.now(),
    });

    return session;
  }
}
