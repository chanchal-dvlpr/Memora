import { Session, SessionExpirationPolicy } from '../types/session';

export interface ExpirationEvaluationResult {
  readonly isExpired: boolean;
  readonly reason?: string;
  readonly elapsedMs?: number;
}

export class ExpirationEvaluator {
  /**
   * Evaluates whether a session has expired based on the specified expiration policy.
   */
  public evaluate(session: Session, policy: SessionExpirationPolicy, now = Date.now()): ExpirationEvaluationResult {
    if (session.state === 'expired') {
      return {
        isExpired: true,
        reason: 'Session state is already marked expired',
      };
    }

    if (session.state === 'closed') {
      return {
        isExpired: false,
        reason: 'Session is closed',
      };
    }

    switch (policy.expirationType) {
      case 'none':
        return { isExpired: false };

      case 'manual': {
        const isManual = session.isManuallyExpired === true || policy.isManuallyExpired === true;
        return {
          isExpired: isManual,
          reason: isManual ? 'Session manually expired' : undefined,
        };
      }

      case 'absolute': {
        const timeout = policy.absoluteTimeoutMs || 0;
        if (timeout <= 0) {
          return { isExpired: false };
        }
        const elapsed = now - session.metadata.createdAt;
        const expired = elapsed > timeout;
        return {
          isExpired: expired,
          reason: expired ? `Absolute timeout exceeded (${elapsed}ms > ${timeout}ms)` : undefined,
          elapsedMs: elapsed,
        };
      }

      case 'sliding': {
        const maxIdle = policy.maxIdleTimeMs || 0;
        if (maxIdle <= 0) {
          return { isExpired: false };
        }
        const idleTime = now - session.lastAccessedAt;
        const expired = idleTime > maxIdle;
        return {
          isExpired: expired,
          reason: expired ? `Sliding idle timeout exceeded (${idleTime}ms > ${maxIdle}ms)` : undefined,
          elapsedMs: idleTime,
        };
      }

      default:
        return { isExpired: false };
    }
  }
}
