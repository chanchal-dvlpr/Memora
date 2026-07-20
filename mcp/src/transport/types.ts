import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';

export interface McpTransport {
  readonly type: 'stdio' | 'http' | 'websocket';
  
  /**
   * Initializes the transport instance.
   */
  initialize(): Promise<void>;

  /**
   * Returns the underlying SDK Transport implementation.
   */
  getTransportInstance(): Transport;

  /**
   * Closes the transport layer cleanly.
   */
  close(): Promise<void>;
}
