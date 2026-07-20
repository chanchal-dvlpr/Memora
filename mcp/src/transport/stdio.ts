import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { McpTransport } from './types';

export class StdioTransport implements McpTransport {
  public readonly type = 'stdio' as const;
  private instance: StdioServerTransport | null = null;

  public async initialize(): Promise<void> {
    if (!this.instance) {
      this.instance = new StdioServerTransport();
    }
  }

  public getTransportInstance(): Transport {
    if (!this.instance) {
      throw new Error('StdioTransport has not been initialized. Call initialize() first.');
    }
    return this.instance;
  }

  public async close(): Promise<void> {
    if (this.instance) {
      await this.instance.close();
      this.instance = null;
    }
  }
}
