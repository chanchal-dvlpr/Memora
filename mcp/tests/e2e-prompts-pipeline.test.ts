import { McpTestHarness } from './harness/mcp_test_client';

describe('E2E Integration: Prompts Pipeline', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should get "generate-handoff" prompt through full pipeline and attach sessionContext', async () => {
    const { result, context } = await harness.getPrompt('generate-handoff');

    expect(result.messages).toBeDefined();
    expect(result.messages.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should get "review-architecture" prompt through full pipeline', async () => {
    const { result, context } = await harness.getPrompt('review-architecture', { projectId: 'test-proj' });

    expect(result.messages).toBeDefined();
    expect(result.messages.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should get "summarize-project" prompt through full pipeline', async () => {
    const { result, context } = await harness.getPrompt('summarize-project', { projectId: 'test-proj' });

    expect(result.messages).toBeDefined();
    expect(result.messages.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should get "explain-module" prompt through full pipeline', async () => {
    const { result, context } = await harness.getPrompt('explain-module', { moduleName: 'core' });

    expect(result.messages).toBeDefined();
    expect(result.messages.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });

  it('should get "review-tasks" prompt through full pipeline', async () => {
    const { result, context } = await harness.getPrompt('review-tasks', { projectId: 'test-proj' });

    expect(result.messages).toBeDefined();
    expect(result.messages.length).toBeGreaterThan(0);
    expect(context.sessionContext).toBeDefined();
  });
});
