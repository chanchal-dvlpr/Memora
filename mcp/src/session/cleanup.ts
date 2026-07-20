import { SessionRegistry } from './registry';
import { ContextStore } from './store';
import { ExpirationEvaluator } from './expiration';
import { SessionExpirationPolicy, SessionCleanupStatistics, SessionId } from '../types/session';

export class SessionCleanupManager {
  private readonly registry: SessionRegistry;
  private readonly store: ContextStore;
  private readonly evaluator: ExpirationEvaluator;
  private readonly policy: SessionExpirationPolicy;

  private cleanupTimer: NodeJS.Timeout | null = null;
  private cleanupRuns = 0;
  private removedSessionsCount = 0;
  private removedContextsCount = 0;
  private lastCleanupTimestamp?: number;

  constructor(
    registry: SessionRegistry,
    store: ContextStore,
    policy: SessionExpirationPolicy = { expirationType: 'none' },
    evaluator = new ExpirationEvaluator()
  ) {
    this.registry = registry;
    this.store = store;
    this.policy = policy;
    this.evaluator = evaluator;
  }

  /**
   * Performs an immediate cleanup pass: purges expired sessions and orphan context entries.
   */
  public async cleanup(): Promise<SessionCleanupStatistics> {
    this.cleanupRuns++;
    this.lastCleanupTimestamp = Date.now();

    const activeSessions = this.registry.listSessions();
    const activeSessionIds = new Set<SessionId>(activeSessions.map((s) => s.id));
    let sessionsPurgedInRun = 0;

    // 1. Evaluate & Purge Expired Sessions
    for (const session of activeSessions) {
      const evalResult = this.evaluator.evaluate(session, this.policy, this.lastCleanupTimestamp);
      if (evalResult.isExpired || session.state === 'expired' || session.state === 'closed') {
        try {
          this.registry.removeSession(session.id);
          sessionsPurgedInRun++;
          activeSessionIds.delete(session.id);
        } catch {
          // Ignore if already removed
        }
      }
    }

    this.removedSessionsCount += sessionsPurgedInRun;

    // 2. Orphan Context Cleanup: Purge context store entries for non-existent session IDs
    let contextsPurgedInRun = 0;
    // We check all sessions that were removed and clear their store
    for (const session of activeSessions) {
      if (!activeSessionIds.has(session.id)) {
        this.store.clear(session.id);
        contextsPurgedInRun++;
      }
    }

    this.removedContextsCount += contextsPurgedInRun;

    return this.getStatistics();
  }

  /**
   * Starts periodic automated cleanup passes.
   */
  public startAutoCleanup(intervalMs: number): void {
    this.stopAutoCleanup();
    this.cleanupTimer = setInterval(() => {
      this.cleanup().catch((err) => {
        console.error('Error during automated session cleanup:', err);
      });
    }, intervalMs);
  }

  /**
   * Stops periodic automated cleanup passes.
   */
  public stopAutoCleanup(): void {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
      this.cleanupTimer = null;
    }
  }

  /**
   * Returns statistics on cleanup passes and purged sessions/contexts.
   */
  public getStatistics(): SessionCleanupStatistics {
    return {
      cleanupRuns: this.cleanupRuns,
      removedSessionsCount: this.removedSessionsCount,
      removedContextsCount: this.removedContextsCount,
      lastCleanupTimestamp: this.lastCleanupTimestamp,
    };
  }
}
