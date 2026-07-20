import { MessageParser } from './parser';
import { MessageValidator } from './validator';
import { MessageRouter } from './router';
import { MessageSerializer } from './serializer';
import { ProtocolSession, ProtocolState } from './session';
import { JsonRpcError, JsonRpcInvalidRequestError, JsonRpcInvalidParamsError } from '../errors';
import { StructuredLogger } from '../logging';
import { ToolRegistry } from '../registry/tool';
import { ToolDispatcher } from '../tool/executor';
import { performance } from 'perf_hooks';

export class MessageDispatcher {
  private readonly router: MessageRouter;
  private readonly session: ProtocolSession;
  private readonly serverInfo: { name: string; version: string };
  private readonly logger: StructuredLogger;
  private readonly registry: ToolRegistry;

  constructor(
    router: MessageRouter,
    serverInfo: { name: string; version: string },
    logger: StructuredLogger,
    registry: ToolRegistry
  ) {
    this.router = router;
    this.session = new ProtocolSession();
    this.serverInfo = serverInfo;
    this.logger = logger;
    this.registry = registry;

    this.registerLifecycleHandlers();
    this.registerToolHandlers();
  }

  public getSession(): ProtocolSession {
    return this.session;
  }

  /**
   * Registers protocol handshake, capability negotiation, and lifecycle handlers.
   */
  private registerLifecycleHandlers(): void {
    // 1. initialize request
    this.router.register('initialize', async (params: unknown) => {
      this.session.transitionTo(ProtocolState.INITIALIZING);

      const p = params as Record<string, unknown> | undefined;
      const protocolVersion = p?.protocolVersion;
      if (protocolVersion !== '2024-11-05') {
        throw new JsonRpcInvalidParamsError('Unsupported protocol version. Only "2024-11-05" is supported.');
      }

      const clientInfo = p?.clientInfo as { name: string; version: string } | undefined;
      const capabilities = p?.capabilities || {};

      if (!clientInfo || typeof clientInfo.name !== 'string' || typeof clientInfo.version !== 'string') {
        throw new JsonRpcInvalidParamsError('Client information (name, version) is required.');
      }

      this.session.setClientInfo(clientInfo, protocolVersion, capabilities);

      return {
        protocolVersion: '2024-11-05',
        capabilities: {
          // Expose zero capabilities in this phase
        },
        serverInfo: this.serverInfo,
      };
    });

    // 2. initialized notification
    this.router.register('initialized', async () => {
      this.session.transitionTo(ProtocolState.INITIALIZED);
      return null;
    });

    // 3. shutdown request
    this.router.register('shutdown', async () => {
      this.session.transitionTo(ProtocolState.SHUTDOWN_REQUESTED);
      return {};
    });

    // 4. exit notification
    this.router.register('exit', async () => {
      this.session.transitionTo(ProtocolState.EXITED);
      
      // Clean exit with 50ms delay for stdout flushing
      setTimeout(() => {
        process.exit(0);
      }, 50);
      return null;
    });
  }

  /**
   * Registers tools capability method handlers.
   */
  private registerToolHandlers(): void {
    // 1. tools/list
    this.router.register('tools/list', async () => {
      const tools = this.registry.listTools().map((t) => ({
        name: t.name,
        description: t.description,
        inputSchema: t.inputSchema,
      }));
      return { tools };
    });

    // 2. tools/call
    this.router.register('tools/call', async (params: unknown) => {
      const p = params as { name: string; arguments?: Record<string, unknown> } | undefined;
      if (!p || typeof p.name !== 'string') {
        throw new JsonRpcInvalidParamsError('Tool name is required.');
      }
      const dispatcher = new ToolDispatcher(this.registry);
      const executionContext = {
        requestId: Math.random().toString(36).substring(2, 15),
        sessionId: this.session.sessionId,
        protocolVersion: this.session.getProtocolVersion() || '2024-11-05',
        timestamp: Date.now(),
        logger: this.logger,
        params: p.arguments || {},
        metadata: new Map<string, unknown>(),
      };
      const result = await dispatcher.dispatchCall(p.name, p.arguments || {}, executionContext);
      return result;
    });
  }

  /**
   * Dispatches incoming JSON-RPC payloads, enforcing protocol handshake and validation rules.
   * Tracks parsing, validation, routing, and serialization execution metrics.
   */
  public async dispatch(payload: string): Promise<string | null> {
    const totalStart = performance.now();
    let parseMs = 0;
    let validateMs = 0;
    let routeMs = 0;
    let serializeMs = 0;

    let parsedId: string | number | null = null;
    let methodCalled = 'unknown';

    try {
      const parseStart = performance.now();
      const parsed = MessageParser.parse(payload);
      parseMs = performance.now() - parseStart;

      const validateStart = performance.now();
      MessageValidator.validate(parsed);
      validateMs = performance.now() - validateStart;

      if ('id' in parsed) {
        parsedId = parsed.id;
      }
      if ('method' in parsed) {
        methodCalled = parsed.method;
      }

      const state = this.session.getState();

      // Enforce protocol validation guards before initialized state
      if ('method' in parsed) {
        if (state === ProtocolState.NOT_INITIALIZED) {
          if (parsed.method !== 'initialize' && parsed.method !== 'exit') {
            throw new JsonRpcInvalidRequestError('Server is not initialized. Send "initialize" request first.');
          }
        } else if (state === ProtocolState.INITIALIZING) {
          if (parsed.method !== 'initialized' && parsed.method !== 'exit') {
            throw new JsonRpcInvalidRequestError('Server is in initializing state. Send "initialized" notification.');
          }
        } else if (state === ProtocolState.SHUTDOWN_REQUESTED) {
          if (parsed.method !== 'exit') {
            throw new JsonRpcInvalidRequestError('Server is in shutdown state. Only "exit" notification is allowed.');
          }
        }
      }

      // 1. Handle JSON-RPC Request
      if ('method' in parsed && 'id' in parsed) {
        try {
          const routeStart = performance.now();
          const result = await this.router.route(parsed.method, parsed.params);
          routeMs = performance.now() - routeStart;

          const serializeStart = performance.now();
          const response = MessageSerializer.success(parsed.id, result);
          const serialized = MessageSerializer.serialize(response);
          serializeMs = performance.now() - serializeStart;

          const totalMs = performance.now() - totalStart;
          this.logPerformanceMetrics(parseMs, validateMs, routeMs, serializeMs, totalMs, methodCalled);

          return serialized;
        } catch (err) {
          const routeEnd = performance.now();
          routeMs = routeEnd - totalStart - parseMs - validateMs;

          const code = err instanceof JsonRpcError ? err.code : -32603;
          const msg = err instanceof Error ? err.message : 'Internal error';
          const data = err instanceof JsonRpcError ? err.data : undefined;
          
          const serializeStart = performance.now();
          const errorResponse = MessageSerializer.error(parsed.id, code, msg, data);
          const serialized = MessageSerializer.serialize(errorResponse);
          serializeMs = performance.now() - serializeStart;

          const totalMs = performance.now() - totalStart;
          this.logPerformanceMetrics(parseMs, validateMs, routeMs, serializeMs, totalMs, methodCalled);

          return serialized;
        }
      }

      // 2. Handle JSON-RPC Notification (No ID)
      if ('method' in parsed && !('id' in parsed)) {
        try {
          if (this.router.hasRoute(parsed.method)) {
            const routeStart = performance.now();
            await this.router.route(parsed.method, parsed.params);
            routeMs = performance.now() - routeStart;
          }
        } catch (err) {
          // Notifications do not emit error responses back to transport
        }
        
        const totalMs = performance.now() - totalStart;
        this.logPerformanceMetrics(parseMs, validateMs, routeMs, 0, totalMs, methodCalled);
        return null;
      }

      const totalMs = performance.now() - totalStart;
      this.logPerformanceMetrics(parseMs, validateMs, 0, 0, totalMs, methodCalled);
      return null;
    } catch (err) {
      const code = err instanceof JsonRpcError ? err.code : -32600;
      const msg = err instanceof Error ? err.message : 'Invalid request';
      
      const serializeStart = performance.now();
      const errorResponse = MessageSerializer.error(parsedId, code, msg);
      const serialized = MessageSerializer.serialize(errorResponse);
      serializeMs = performance.now() - serializeStart;

      const totalMs = performance.now() - totalStart;
      this.logPerformanceMetrics(parseMs, validateMs, 0, serializeMs, totalMs, methodCalled);

      return serialized;
    }
  }

  /**
   * Logs execution times for performance monitoring.
   */
  private logPerformanceMetrics(
    parseMs: number,
    validateMs: number,
    routeMs: number,
    serializeMs: number,
    totalMs: number,
    method: string
  ): void {
    this.logger.debug(`Processed MCP method: ${method}`, {
      performance: {
        method,
        parseMs,
        validateMs,
        routeMs,
        serializeMs,
        totalMs,
      },
    });
  }
}
