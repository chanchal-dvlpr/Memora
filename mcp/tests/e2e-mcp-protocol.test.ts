import { McpTestHarness } from './harness/mcp_test_client';
import { ToolNotFoundError, ResourceNotFoundError, PromptNotFoundError } from '../src/errors';

describe('E2E Integration: JSON-RPC & MCP Protocol Layer', () => {
  let harness: McpTestHarness;

  beforeEach(async () => {
    harness = new McpTestHarness();
    await harness.setup();
  });

  afterEach(async () => {
    await harness.teardown();
  });

  it('should initialize server correctly and report state', () => {
    const server = harness.getServer();
    expect(server.getLifecycleState().toLowerCase()).toBe('initialized');
    expect(server.getToolRegistry().listTools().length).toBeGreaterThan(0);
    expect(server.getResourceRegistry().listResources().length).toBeGreaterThan(0);
    expect(server.getPromptRegistry().listPrompts().length).toBeGreaterThan(0);
  });

  it('should list all registered tools via registry and produce valid metadata', () => {
    const tools = harness.getServer().getToolRegistry().listTools();
    const toolNames = tools.map((t) => t.name);

    expect(toolNames).toContain('status');
    expect(toolNames).toContain('doctor');
    expect(toolNames).toContain('projects');
    expect(toolNames).toContain('search');
    expect(toolNames).toContain('handoff');
  });

  it('should list all registered resources via registry and produce valid metadata', () => {
    const resources = harness.getServer().getResourceRegistry().listResources();
    const uris = resources.map((r) => r.uri);

    expect(uris).toContain('memora://project');
    expect(uris).toContain('memora://architecture');
    expect(uris).toContain('memora://tasks');
    expect(uris).toContain('memora://decisions');
  });

  it('should list all registered prompts via registry and produce valid metadata', () => {
    const prompts = harness.getServer().getPromptRegistry().listPrompts();
    const promptNames = prompts.map((p) => p.name);

    expect(promptNames).toContain('generate-handoff');
    expect(promptNames).toContain('review-architecture');
    expect(promptNames).toContain('summarize-project');
    expect(promptNames).toContain('explain-module');
    expect(promptNames).toContain('review-tasks');
  });

  it('should throw ToolNotFoundError when calling unregistered tool', async () => {
    await expect(harness.callTool('non_existent_tool')).rejects.toThrow(ToolNotFoundError);
  });

  it('should throw ResourceNotFoundError when reading unregistered resource', async () => {
    await expect(harness.readResource('memora://unknown')).rejects.toThrow(ResourceNotFoundError);
  });

  it('should throw PromptNotFoundError when getting unregistered prompt', async () => {
    await expect(harness.getPrompt('non_existent_prompt')).rejects.toThrow(PromptNotFoundError);
  });
});
