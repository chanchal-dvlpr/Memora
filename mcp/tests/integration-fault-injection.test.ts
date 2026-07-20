import { ConcurrencyManager } from '../src/reliability/concurrency';
import { TimeoutManager } from '../src/reliability/timeout';
import { QueueOverflowError, RequestTimeoutError, InvalidCredentialError, SessionExpiredError } from '../src/errors';
import { AuthenticationManager, MockAuthenticationProvider } from '../src/security/authentication';
import { SessionManager } from '../src/session/manager';

describe('Integration: Controlled Fault Injection (Phase 13.9.7)', () => {
  it('should reject requests with QueueOverflowError on queue overflow', async () => {
    const concurrency = new ConcurrencyManager(1, 1, 'reject');

    // Fill active slot
    const active = concurrency.execute(() => new Promise((resolve) => setTimeout(resolve, 50)));

    // Immediate second request throws QueueOverflowError when strategy is reject
    await expect(concurrency.execute(() => Promise.resolve())).rejects.toThrow(QueueOverflowError);

    await active;
  });

  it('should cancel handler execution and throw RequestTimeoutError on timeout expiration', async () => {
    const task = TimeoutManager.withTimeout(
      async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      },
      10,
      'fault-timeout'
    );

    await expect(task).rejects.toThrow(RequestTimeoutError);
  });

  it('should throw InvalidCredentialError on invalid security token', async () => {
    const authManager = new AuthenticationManager();
    authManager.registerProvider(new MockAuthenticationProvider());
    await expect(authManager.authenticate('mock', { credentials: { token: 'invalid-token-xyz' } })).rejects.toThrow(InvalidCredentialError);
  });

  it('should throw SessionExpiredError when validating expired session', async () => {
    const sessionManager = new SessionManager(undefined, undefined, {
      expirationPolicy: { expirationType: 'absolute', absoluteTimeoutMs: 1 },
    });
    const session = await sessionManager.openSession('session-expire-test');
    await new Promise((resolve) => setTimeout(resolve, 5));

    await expect(sessionManager.validateSession(session.id)).rejects.toThrow(SessionExpiredError);
  });
});
