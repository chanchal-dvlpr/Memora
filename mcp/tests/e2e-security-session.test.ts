import { McpTestHarness } from './harness/mcp_test_client';
import { mockCredentials } from './fixtures';

describe('E2E Integration: Security & Session Pipeline', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should authenticate user via credentials, attach SecurityContext and SessionContext', async () => {
    const meta = new Map<string, unknown>([
      ['credentials', mockCredentials],
      ['sessionId', 'custom-sec-session-1'],
    ]);

    const { result, context } = await harness.callTool('status', {}, meta);

    expect(result.content).toBeDefined();
    expect(context.securityContext).toBeDefined();
    expect(context.securityContext.principal).toBeDefined();
    expect(context.session || context.sessionId).toBeDefined();
    expect(context.session?.id || context.sessionId).toBe('custom-sec-session-1');
  });

  it('should reuse session and refresh lastAccessedAt across multiple pipeline calls', async () => {
    const meta = new Map<string, unknown>([['sessionId', 'reuse-session-99']]);

    const call1 = await harness.callTool('status', {}, meta);
    const initialTouch = harness.getServer().getSessionManager().getStatistics().touchCount;

    const call2 = await harness.readResource('memora://project', meta);
    const secondTouch = harness.getServer().getSessionManager().getStatistics().touchCount;

    const sid1 = call1.context.session?.id || call1.context.sessionId;
    const sid2 = call2.context.session?.id || call2.context.sessionId;

    expect(sid1).toBe('reuse-session-99');
    expect(sid2).toBe('reuse-session-99');
    expect(secondTouch).toBeGreaterThan(initialTouch);
  });

  it('should auto-create a session when no session ID is supplied', async () => {
    const { context } = await harness.callTool('doctor');

    const sid = context.session?.id || context.sessionId;
    expect(sid).toBeDefined();
    expect(harness.getServer().getSessionManager().getRegistry().hasSession(sid)).toBe(true);
  });
});
