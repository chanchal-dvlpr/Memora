import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';
import { McpTransport } from './types';

export class HttpTransport implements McpTransport {
  public readonly type = 'http' as const;

  public async initialize(): Promise<void> {
    // Placeholder implementation
  }

  public getTransportInstance(): Transport {
    throw new Error('HttpTransport is a placeholder and not yet implemented.');
  }

  public async close(): Promise<void> {
    // Placeholder implementation
  }
}
