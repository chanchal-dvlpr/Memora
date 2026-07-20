import { ConfigLoader } from './config';
import { MemoraMcpServer } from './server';
import { StdioTransport } from './transport';

let server: MemoraMcpServer | null = null;
let isShuttingDown = false;

async function bootstrap() {
  const config = ConfigLoader.load();
  const transport = new StdioTransport();
  server = new MemoraMcpServer(config, transport);

  const logger = server.getLogger();

  // Setup graceful shutdown function
  const shutdown = async (reason: string, exitCode = 0) => {
    if (isShuttingDown) {
      return; // Idempotent check
    }
    isShuttingDown = true;

    logger.warn(`Shutdown initiated: ${reason}`);
    try {
      if (server) {
        await server.stop();
      }
      logger.info('Graceful shutdown completed successfully.');
    } catch (err) {
      logger.error('Error occurred during server shutdown', err as Error);
      exitCode = 1;
    } finally {
      process.exit(exitCode);
    }
  };

  // Bind signal listeners
  process.on('SIGINT', () => shutdown('SIGINT (Ctrl+C)', 0));
  process.on('SIGTERM', () => shutdown('SIGTERM', 0));

  // Bind exception handlers
  process.on('uncaughtException', async (error) => {
    logger.fatal('Uncaught Exception detected', error);
    await shutdown('uncaughtException', 1);
  });

  process.on('unhandledRejection', async (reason) => {
    const error = reason instanceof Error ? reason : new Error(String(reason));
    logger.fatal('Unhandled Promise Rejection detected', error);
    await shutdown('unhandledRejection', 1);
  });

  try {
    server.initialize();
    await server.start();
  } catch (error) {
    logger.fatal('Failed to bootstrap Memora MCP server', error as Error);
    process.exit(1);
  }
}

function resetShutdownState() {
  isShuttingDown = false;
  server = null;
}

// Start the server if executed directly
if (require.main === module) {
  bootstrap().catch((err) => {
    console.error('Fatal bootstrap failure:', err);
    process.exit(1);
  });
}
export { bootstrap, server, isShuttingDown, resetShutdownState };
