/* eslint-disable @typescript-eslint/no-explicit-any */
import { MemoraMcpServer } from '../../src/server';
import { ConfigLoader, ServerConfig } from '../../src/config';
import { StdioTransport } from '../../src/transport';
import { ToolDispatcher } from '../../src/tool/executor';
import { ResourceDispatcher } from '../../src/resource/executor';
import { PromptDispatcher } from '../../src/prompt/executor';
import { ToolExecutionResult } from '../../src/types/tool';
import { ResourceReadResult } from '../../src/types/resource';
import { PromptInvocationResult } from '../../src/types/prompt';

export class McpTestHarness {
  private server: MemoraMcpServer;

  constructor(customConfig?: Partial<ServerConfig>) {
    const baseConfig = ConfigLoader.load();
    const config: ServerConfig = {
      ...baseConfig,
      environment: 'test',
      usePlaceholder: true, // In-memory placeholder handlers for fast integration testing
      ...customConfig,
    };

    const transport = new StdioTransport();
    this.server = new MemoraMcpServer(config, transport);
  }

  public async setup(): Promise<void> {
    this.server.initialize();
  }

  public async teardown(): Promise<void> {
    await this.server.stop();
  }

  public getServer(): MemoraMcpServer {
    return this.server;
  }

  /**
   * Executes a tool call through ToolDispatcher.
   */
  public async callTool(
    name: string,
    args: Record<string, unknown> = {},
    metadata: Map<string, unknown> = new Map()
  ): Promise<{ result: ToolExecutionResult; context: any }> {
    const dispatcher = new ToolDispatcher(
      this.server.getToolRegistry(),
      this.server.getAuthenticationManager(),
      this.server.getAuthorizationManager(),
      this.server.getAuditLogger(),
      this.server.getConfig(),
      this.server.getSessionManager()
    );

    const context: any = {
      requestId: `test-req-${Math.random().toString(36).substring(2, 9)}`,
      sessionId: (metadata.get('sessionId') as string) || 'test-session-id',
      protocolVersion: '2024-11-05',
      timestamp: Date.now(),
      logger: this.server.getLogger(),
      params: args,
      metadata,
    };

    const result = await dispatcher.dispatchCall(name, args, context);
    return { result, context };
  }

  /**
   * Reads a resource through ResourceDispatcher.
   */
  public async readResource(
    uri: string,
    metadata: Map<string, unknown> = new Map()
  ): Promise<{ result: ResourceReadResult; context: any }> {
    const dispatcher = new ResourceDispatcher(
      this.server.getResourceRegistry(),
      this.server.getAuthenticationManager(),
      this.server.getAuthorizationManager(),
      this.server.getAuditLogger(),
      this.server.getConfig(),
      this.server.getSessionManager()
    );

    const context: any = {
      requestId: `test-req-${Math.random().toString(36).substring(2, 9)}`,
      sessionId: (metadata.get('sessionId') as string) || 'test-session-id',
      protocolVersion: '2024-11-05',
      timestamp: Date.now(),
      logger: this.server.getLogger(),
      params: {},
      metadata,
    };

    const result = await dispatcher.dispatchRead(uri, context);
    return { result, context };
  }

  /**
   * Gets a prompt through PromptDispatcher.
   */
  public async getPrompt(
    name: string,
    args: Record<string, string> = {},
    metadata: Map<string, unknown> = new Map()
  ): Promise<{ result: PromptInvocationResult; context: any }> {
    const dispatcher = new PromptDispatcher(
      this.server.getPromptRegistry(),
      this.server.getAuthenticationManager(),
      this.server.getAuthorizationManager(),
      this.server.getAuditLogger(),
      this.server.getConfig(),
      this.server.getSessionManager()
    );

    const context: any = {
      requestId: `test-req-${Math.random().toString(36).substring(2, 9)}`,
      sessionId: (metadata.get('sessionId') as string) || 'test-session-id',
      protocolVersion: '2024-11-05',
      timestamp: Date.now(),
      logger: this.server.getLogger(),
      params: args,
      metadata,
    };

    const result = await dispatcher.dispatchGet(name, args, context);
    return { result, context };
  }
}
