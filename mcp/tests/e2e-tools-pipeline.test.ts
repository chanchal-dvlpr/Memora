import { McpTestHarness } from './harness/mcp_test_client';

describe('E2E Integration: Tools Execution Pipeline', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should execute "status" tool through full pipeline and attach sessionContext', async () => {
    const { result, context } = await harness.callTool('status');

    expect(result.content).toBeDefined();
    expect(result.content.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should execute "doctor" tool through full pipeline and return system diagnostics', async () => {
    const { result, context } = await harness.callTool('doctor');

    expect(result.content).toBeDefined();
    expect(result.content.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should execute "projects" tool through full pipeline', async () => {
    const { result, context } = await harness.callTool('projects');

    expect(result.content).toBeDefined();
    expect(result.content.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should execute "search" tool with query parameter through full pipeline', async () => {
    const { result, context } = await harness.callTool('search', { query: 'test' });

    expect(result.content).toBeDefined();
    expect(result.content.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should execute "handoff" tool through full pipeline', async () => {
    const { result, context } = await harness.callTool('handoff');

    expect(result.content).toBeDefined();
    expect(result.content.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });
});
