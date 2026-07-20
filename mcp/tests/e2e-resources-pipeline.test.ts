import { McpTestHarness } from './harness/mcp_test_client';

describe('E2E Integration: Resources Reading Pipeline', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should read "memora://project" resource through full pipeline and attach sessionContext', async () => {
    const { result, context } = await harness.readResource('memora://project');

    expect(result.contents).toBeDefined();
    expect(result.contents.length).toBeGreaterThan(0);
    expect(result.contents[0].uri).toBe('memora://project');
    expect(context.sessionContext).toBeDefined();
  });

  it('should read "memora://architecture" resource through full pipeline', async () => {
    const { result, context } = await harness.readResource('memora://architecture');

    expect(result.contents).toBeDefined();
    expect(result.contents.length).toBeGreaterThan(0);
    expect(result.contents[0].uri).toBe('memora://architecture');
    expect(context.sessionContext).toBeDefined();
  });

  it('should read "memora://tasks" resource through full pipeline', async () => {
    const { result, context } = await harness.readResource('memora://tasks');

    expect(result.contents).toBeDefined();
    expect(result.contents.length).toBeGreaterThan(0);
    expect(result.contents[0].uri).toBe('memora://tasks');
    expect(context.sessionContext).toBeDefined();
  });

  it('should read "memora://decisions" resource through full pipeline', async () => {
    const { result, context } = await harness.readResource('memora://decisions');

    expect(result.contents).toBeDefined();
    expect(result.contents.length).toBeGreaterThan(0);
    expect(result.contents[0].uri).toBe('memora://decisions');
    expect(context.sessionContext).toBeDefined();
  });
});
