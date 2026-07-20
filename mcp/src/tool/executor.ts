import { ToolRegistry } from '../registry/tool';
import { ToolDefinition, ToolExecutionContext, ToolExecutionResult } from '../types/tool';
import { ToolMiddlewarePipeline, ToolMiddleware } from './middleware';
import { ToolValidator } from './validator';
import { ToolNotFoundError, ToolExecutionError, AuthenticationError, PermissionDeniedError } from '../errors';
import { performance } from 'perf_hooks';
import { AuthenticationManager } from '../security/authentication';
import { AuthorizationManager } from '../security/authorization';
import { AuditLogger } from '../security/audit';
import { ServerConfig } from '../config';
import { SecurityAction, AuditLevel, Permission } from '../types/security';
import { SessionManager } from '../session/manager';

export class ToolExecutor {
  /**
   * Invokes the tool's handler with the provided parameters.
   */
  public static async execute(tool: ToolDefinition, params: Record<string, unknown>): Promise<ToolExecutionResult> {
    try {
      const result = await tool.handler(params);
      return result;
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown tool execution error';
      throw new ToolExecutionError(`Tool execution failed: ${msg}`);
    }
  }
}

export class ToolDispatcher {
  private readonly registry: ToolRegistry;
  private readonly pipeline: ToolMiddlewarePipeline;
  private readonly authManager?: AuthenticationManager;
  private readonly authzManager?: AuthorizationManager;
  private readonly auditLogger?: AuditLogger;
  private readonly config?: ServerConfig;
  private readonly sessionManager?: SessionManager;

  constructor(
    registry: ToolRegistry,
    authManager?: AuthenticationManager,
    authzManager?: AuthorizationManager,
    auditLogger?: AuditLogger,
    config?: ServerConfig,
    sessionManager?: SessionManager
  ) {
    this.registry = registry;
    this.pipeline = new ToolMiddlewarePipeline();
    this.authManager = authManager;
    this.authzManager = authzManager;
    this.auditLogger = auditLogger;
    this.config = config;
    this.sessionManager = sessionManager;

    // Register standard middlewares in deterministic order:
    // Authentication -> Authorization -> Session (Validation -> Lookup/Create -> Touch) -> Audit -> Logging -> Timing -> Validation -> Execution
    this.registerDefaultMiddlewares();
  }

  /**
   * Registers a custom middleware into the pipeline.
   */
  public use(middleware: ToolMiddleware): void {
    this.pipeline.use(middleware);
  }

  /**
   * Coordinates registry lookup and triggers middleware-wrapped execution.
   */
  public async dispatchCall(
    name: string,
    params: Record<string, unknown>,
    context: ToolExecutionContext
  ): Promise<ToolExecutionResult> {
    const tool = this.registry.getTool(name);
    if (!tool) {
      throw new ToolNotFoundError(`Tool "${name}" is not registered.`);
    }

    // Attach params to context for validator access
    context.params = params;

    // Trigger execution through pipeline wrapping tool handler
    return this.pipeline.execute(context, tool, async () => {
      return ToolExecutor.execute(tool, params);
    });
  }

  /**
   * Sets up default middleware hooks.
   */
  private registerDefaultMiddlewares(): void {
    // 1. Authentication Middleware
    this.pipeline.use(async (context, _tool, next) => {
      if (this.config?.securityEnabled === false) {
        context.securityContext = {
          principal: undefined,
          roles: [],
          permissions: [],
          sessionId: context.sessionId,
          requestId: context.requestId,
          metadata: new Map(),
          timestamp: Date.now(),
        };
        return next();
      }

      let credentials = context.metadata.get('credentials') as Record<string, string>;
      if (!credentials && context.params) {
        credentials = context.params['credentials'] as Record<string, string>;
        if (!credentials && typeof context.params['token'] === 'string') {
          credentials = { token: context.params['token'] };
        }
      }

      if (!credentials) {
        context.securityContext = {
          principal: undefined,
          roles: [],
          permissions: [],
          sessionId: context.sessionId,
          requestId: context.requestId,
          metadata: new Map(),
          timestamp: Date.now(),
        };
        return next();
      }

      if (this.authManager) {
        const providerName = this.config?.defaultAuthProvider || 'mock';
        const authRes = await this.authManager.authenticate(providerName, { credentials });
        if (authRes.success && authRes.principal) {
          context.securityContext = {
            principal: authRes.principal,
            roles: [...authRes.principal.roles],
            permissions: (authRes.principal.metadata.get('permissions') || []) as Permission[],
            sessionId: context.sessionId,
            requestId: context.requestId,
            metadata: new Map(),
            timestamp: Date.now(),
          };
        } else {
          throw new AuthenticationError(authRes.error || 'Authentication failed.');
        }
      } else {
        context.securityContext = {
          principal: undefined,
          roles: [],
          permissions: [],
          sessionId: context.sessionId,
          requestId: context.requestId,
          metadata: new Map(),
          timestamp: Date.now(),
        };
      }

      return next();
    });

    // 2. Authorization Middleware
    this.pipeline.use(async (context, tool, next) => {
      if (this.config?.securityEnabled === false) {
        return next();
      }

      if (context.metadata.get('auth_blocked') === true) {
        throw new ToolExecutionError(`Authorization blocked for tool ${tool.name}`);
      }

      if (this.authzManager && context.securityContext) {
        const policy = this.config?.defaultAuthzPolicy || 'allow-all';
        const permissionName = policy !== 'allow-all'
          ? (tool.requiredPermission || `memora://tools/${tool.name}`)
          : undefined;

        const authzResult = await this.authzManager.authorize(
          context.securityContext,
          SecurityAction.EXECUTE,
          `memora://tools/${tool.name}`,
          permissionName,
          policy
        );

        if (!authzResult.allowed) {
          throw new PermissionDeniedError(authzResult.reason || 'Permission denied.');
        }
      }

      return next();
    });

    // 3. Session Middleware (Validation -> Lookup/Create -> Touch)
    this.pipeline.use(async (context, _tool, next) => {
      if (!this.sessionManager) {
        return next();
      }

      const sessionId = context.sessionId || (context.metadata.get('sessionId') as string) || `session-${context.requestId}`;

      // Session Validation
      if (this.sessionManager.getRegistry().hasSession(sessionId)) {
        await this.sessionManager.validateSession(sessionId);
      }

      // Session Lookup / Auto-Create
      let session = this.sessionManager.getRegistry().getSession(sessionId);
      if (!session) {
        session = await this.sessionManager.openSession(
          sessionId,
          (context.metadata.get('clientMetadata') as Record<string, unknown>) || {}
        );
      }

      // Session Touch
      session = await this.sessionManager.touchSession(sessionId);

      // Attach SessionContext to execution context
      context.sessionContext = session.context;
      return next();
    });

    // 3. Auditing Middleware
    this.pipeline.use(async (context, tool, next) => {
      if (this.config?.auditLogEnabled === false || !this.auditLogger) {
        return next();
      }

      const actor = context.securityContext?.principal?.name || 'anonymous';
      try {
        const res = await next();
        this.auditLogger.log(
          AuditLevel.INFO,
          'tool-execution',
          'execute',
          `Executed tool "${tool.name}" successfully.`,
          'success',
          actor
        );
        return res;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        this.auditLogger.log(
          AuditLevel.WARNING,
          'tool-execution',
          'execute',
          `Failed executing tool "${tool.name}". Error: ${msg}`,
          'failure',
          actor
        );
        throw err;
      }
    });

    // 4. Logging Middleware
    this.pipeline.use(async (context, tool, next) => {
      context.logger.debug(`Executing tool "${tool.name}"`, { requestId: context.requestId });
      try {
        const res = await next();
        context.logger.debug(`Tool "${tool.name}" executed successfully`, { requestId: context.requestId });
        return res;
      } catch (err) {
        context.logger.error(`Execution failed for "${tool.name}": ${err instanceof Error ? err.message : String(err)}`);
        throw err;
      }
    });

    // 5. Timing Middleware
    this.pipeline.use(async (context, tool, next) => {
      const start = performance.now();
      const res = await next();
      const elapsed = performance.now() - start;
      context.logger.info(`[Metrics] Tool "${tool.name}" latency: ${elapsed.toFixed(2)}ms`);
      return res;
    });

    // 6. Validation Middleware
    this.pipeline.use(async (context, tool, next) => {
      // Validate inputs
      ToolValidator.validateInput(tool.inputSchema, context.params);
      
      const res = await next();
      
      // Validate outputs
      ToolValidator.validateOutput(res);
      return res;
    });
  }
}
