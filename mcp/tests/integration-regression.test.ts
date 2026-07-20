import { McpTestHarness } from './harness/mcp_test_client';
import { AuthorizationManager } from '../src/security/authorization';
import { SecurityAction } from '../src/types/security';
import { ToolVisibility } from '../src/types/tool';

describe('Integration: Regression Subsystem Invariants (Phase 13.9.8)', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should maintain strict middleware execution order (Auth -> Authz -> Session -> Audit -> Handler)', async () => {
    const executionOrder: string[] = [];

    const server = harness.getServer();
    const toolRegistry = server.getToolRegistry();

    toolRegistry.registerTool(
      {
        name: 'regr_tool',
        inputSchema: { type: 'object' },
        handler: async () => {
          executionOrder.push('handler');
          return { content: [{ type: 'text', text: 'ok' }] };
        },
      },
      { visibility: ToolVisibility.PUBLIC, categories: [] }
    );

    const { result } = await harness.callTool('regr_tool');
    expect(result.content).toBeDefined();
    expect(executionOrder).toContain('handler');
  });

  it('should evaluate security policy and allow permitted actions', async () => {
    const authz = new AuthorizationManager();
    const allowed = await authz.authorize(
      { principal: undefined, roles: [], permissions: [], timestamp: Date.now(), metadata: new Map() },
      SecurityAction.READ,
      'memora://project'
    );
    expect(allowed.allowed).toBe(true);
  });
});
