import { StdioTransport } from '../src/transport/stdio';
import { HttpTransport } from '../src/transport/http';
import { WebSocketTransport } from '../src/transport/websocket';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';

jest.mock('@modelcontextprotocol/sdk/server/stdio.js', () => {
  return {
    StdioServerTransport: jest.fn().mockImplementation(() => {
      return {
        close: jest.fn().mockResolvedValue(undefined),
      };
    }),
  };
});

describe('Transports', () => {
  describe('StdioTransport', () => {
    it('should initialize and return underlying transport', async () => {
      const transport = new StdioTransport();
      expect(transport.type).toBe('stdio');

      expect(() => transport.getTransportInstance()).toThrow(
        'StdioTransport has not been initialized. Call initialize() first.'
      );

      await transport.initialize();
      const sdkTransport = transport.getTransportInstance();
      expect(sdkTransport).toBeDefined();
      expect(StdioServerTransport).toHaveBeenCalled();
    });

    it('should close cleanly', async () => {
      const transport = new StdioTransport();
      await transport.initialize();
      const sdkTransport = transport.getTransportInstance();

      await transport.close();
      expect(() => transport.getTransportInstance()).toThrow(
        'StdioTransport has not been initialized. Call initialize() first.'
      );
      expect(sdkTransport.close).toHaveBeenCalled();
    });
  });

  describe('HttpTransport (placeholder)', () => {
    it('should initialize and throw on transport retrieval', async () => {
      const transport = new HttpTransport();
      expect(transport.type).toBe('http');

      await transport.initialize();
      expect(() => transport.getTransportInstance()).toThrow(
        'HttpTransport is a placeholder and not yet implemented.'
      );
      await transport.close();
    });
  });

  describe('WebSocketTransport (placeholder)', () => {
    it('should initialize and throw on transport retrieval', async () => {
      const transport = new WebSocketTransport();
      expect(transport.type).toBe('websocket');

      await transport.initialize();
      expect(() => transport.getTransportInstance()).toThrow(
        'WebSocketTransport is a placeholder and not yet implemented.'
      );
      await transport.close();
    });
  });
});
