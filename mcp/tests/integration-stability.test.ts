import { McpTestHarness } from './harness/mcp_test_client';
import { SessionManager } from '../src/session/manager';
import { ContextStore } from '../src/session/store';

describe('Integration: Long-Running Stability (Phase 13.9.7)', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should maintain state integrity over 100 repeated tool, resource, and prompt dispatch cycles', async () => {
    for (let i = 0; i < 100; i++) {
      const meta = new Map<string, unknown>([['sessionId', `stab-session-${i % 5}`]]);

      const tRes = await harness.callTool('status', {}, meta);
      expect(tRes.result.content).toBeDefined();

      const rRes = await harness.readResource('memora://project', meta);
      expect(rRes.result.contents).toBeDefined();

      const pRes = await harness.getPrompt('generate-handoff', {}, meta);
      expect(pRes.result.messages).toBeDefined();
    }

    const sessionStats = harness.getServer().getSessionManager().getStatistics();
    expect(sessionStats.touchCount).toBeGreaterThanOrEqual(300);
  });

  it('should handle high frequency session opening, key writing, and purging without memory leaks', async () => {
    const store = new ContextStore();
    const manager = new SessionManager(undefined, undefined, {
      expirationPolicy: { expirationType: 'absolute', absoluteTimeoutMs: 1 },
    });
    const cleanup = manager.getCleanupManager(store);

    for (let i = 0; i < 100; i++) {
      const sid = `stab-leak-test-${i}`;
      await manager.openSession(sid);
      store.put(sid, 'key', `val-${i}`);
    }

    await new Promise((resolve) => setTimeout(resolve, 5));

    const purgeResult = await cleanup.cleanup();
    expect(purgeResult.removedSessionsCount).toBe(100);
    expect(manager.getRegistry().listSessions()).toHaveLength(0);
  });
});
