import { StdioTransport, HttpTransport, WebSocketTransport } from '../src/transport';

describe('Integration: Multi-Transport Protocol Layer (Phase 13.9.5)', () => {
  describe('STDIO Transport', () => {
    it('should initialize and close STDIO transport cleanly', async () => {
      const transport = new StdioTransport();
      expect(transport.type).toBe('stdio');

      await transport.initialize();
      await transport.close();
    });
  });

  describe('HTTP Transport', () => {
    it('should initialize and close HTTP transport cleanly', async () => {
      const transport = new HttpTransport();
      expect(transport.type).toBe('http');

      await transport.initialize();
      await transport.close();
    });
  });

  describe('WebSocket Transport', () => {
    it('should initialize and close WebSocket transport cleanly', async () => {
      const transport = new WebSocketTransport();
      expect(transport.type).toBe('websocket');

      await transport.initialize();
      await transport.close();
    });
  });
});
