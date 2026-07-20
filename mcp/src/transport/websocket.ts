import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';
import { McpTransport } from './types';

export class WebSocketTransport implements McpTransport {
  public readonly type = 'websocket' as const;

  public async initialize(): Promise<void> {
    // Placeholder implementation
  }

  public getTransportInstance(): Transport {
    throw new Error('WebSocketTransport is a placeholder and not yet implemented.');
  }

  public async close(): Promise<void> {
    // Placeholder implementation
  }
}
