import { PromptRegistry } from '../registry/prompt';
import { PromptDefinition, PromptExecutionContext, PromptInvocationResult } from '../types/prompt';
import { PromptMiddlewarePipeline, PromptMiddleware } from './middleware';
import { PromptValidator } from './validator';
import {
  PromptNotFoundError,
  PromptExecutionError,
  PromptError,
  AuthenticationError,
  PermissionDeniedError,
} from '../errors';
import { performance } from 'perf_hooks';
import { AuthenticationManager } from '../security/authentication';
import { AuthorizationManager } from '../security/authorization';
import { AuditLogger } from '../security/audit';
import { ServerConfig } from '../config';
import { SecurityAction, AuditLevel, Permission } from '../types/security';
import { SessionManager } from '../session/manager';

export class PromptExecutor {
  /**
   * Invokes the prompt's handler.
   */
  public static async execute(
    prompt: PromptDefinition,
    args: Record<string, string>,
    context: PromptExecutionContext
  ): Promise<PromptInvocationResult> {
    try {
      const res = await prompt.handler(args, context);
      if (Array.isArray(res)) {
        return { messages: res };
      }
      return res;
    } catch (err) {
      if (err instanceof PromptError) {
        throw err;
      }
      const msg = err instanceof Error ? err.message : 'Unknown prompt execution error';
      throw new PromptExecutionError(`Prompt execution failed: ${msg}`);
    }
  }
}

export class PromptDispatcher {
  private readonly registry: PromptRegistry;
  private readonly pipeline: PromptMiddlewarePipeline;
  private readonly authManager?: AuthenticationManager;
  private readonly authzManager?: AuthorizationManager;
  private readonly auditLogger?: AuditLogger;
  private readonly config?: ServerConfig;
  private readonly sessionManager?: SessionManager;

  constructor(
    registry: PromptRegistry,
    authManager?: AuthenticationManager,
    authzManager?: AuthorizationManager,
    auditLogger?: AuditLogger,
    config?: ServerConfig,
    sessionManager?: SessionManager
  ) {
    this.registry = registry;
    this.pipeline = new PromptMiddlewarePipeline();
    this.authManager = authManager;
    this.authzManager = authzManager;
    this.auditLogger = auditLogger;
    this.config = config;
    this.sessionManager = sessionManager;

    this.registerDefaultMiddlewares();
  }

  /**
   * Registers a custom middleware into the pipeline.
   */
  public use(middleware: PromptMiddleware): void {
    this.pipeline.use(middleware);
  }

  /**
   * Coordinates registry lookup and triggers middleware-wrapped execution.
   */
  public async dispatchGet(
    name: string,
    args: Record<string, string>,
    context: PromptExecutionContext
  ): Promise<PromptInvocationResult> {
    const prompt = this.registry.getPrompt(name);
    if (!prompt) {
      throw new PromptNotFoundError(`Prompt "${name}" is not registered.`);
    }

    // Attach args to context params
    context.params = {
      name,
      ...args,
    };

    try {
      return await this.pipeline.execute(context, prompt, async () => {
        return PromptExecutor.execute(prompt, args, context);
      });
    } catch (err) {
      throw this.translateError(err);
    }
  }

  /**
   * Sets up default middleware hooks:
   * Authentication -> Authorization -> Session (Validation -> Lookup/Create -> Touch) -> Audit -> Logging -> Timing -> Validation -> Execution
   */
  private registerDefaultMiddlewares(): void {
    // 1. Authentication Middleware
    this.pipeline.use(async (context, _prompt, next) => {
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
    this.pipeline.use(async (context, prompt, next) => {
      if (this.config?.securityEnabled === false) {
        return next();
      }

      if (context.metadata.get('auth_blocked') === true) {
        throw new PromptExecutionError(`Authorization blocked for prompt: ${prompt.name}`);
      }

      if (this.authzManager && context.securityContext) {
        const policy = this.config?.defaultAuthzPolicy || 'allow-all';
        const permissionName = policy !== 'allow-all'
          ? (prompt.requiredPermission || `memora://prompts/${prompt.name}`)
          : undefined;

        const authzResult = await this.authzManager.authorize(
          context.securityContext,
          SecurityAction.EXECUTE,
          `memora://prompts/${prompt.name}`,
          permissionName,
          policy
        );

        if (!authzResult.allowed) {
          throw new PermissionDeniedError(authzResult.reason || 'Permission denied.');
        }
      }

      return next();
    });

    // 2.5. Session Middleware (Validation -> Lookup/Create -> Touch)
    this.pipeline.use(async (context, _prompt, next) => {
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
    this.pipeline.use(async (context, prompt, next) => {
      if (this.config?.auditLogEnabled === false || !this.auditLogger) {
        return next();
      }

      const actor = context.securityContext?.principal?.name || 'anonymous';
      try {
        const res = await next();
        this.auditLogger.log(
          AuditLevel.INFO,
          'prompt-execution',
          'execute',
          `Executed prompt "${prompt.name}" successfully.`,
          'success',
          actor
        );
        return res;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        this.auditLogger.log(
          AuditLevel.WARNING,
          'prompt-execution',
          'execute',
          `Failed executing prompt "${prompt.name}". Error: ${msg}`,
          'failure',
          actor
        );
        throw err;
      }
    });

    // 4. Logging Middleware
    this.pipeline.use(async (context, prompt, next) => {
      context.logger.debug(`Invoking prompt "${prompt.name}"`, { requestId: context.requestId });
      try {
        const res = await next();
        context.logger.debug(`Prompt "${prompt.name}" invoked successfully`, { requestId: context.requestId });
        return res;
      } catch (err) {
        context.logger.error(`Prompt invocation failed for "${prompt.name}": ${err instanceof Error ? err.message : String(err)}`);
        throw err;
      }
    });

    // 5. Timing Middleware
    this.pipeline.use(async (context, prompt, next) => {
      const start = performance.now();
      const res = await next();
      const elapsed = performance.now() - start;
      context.logger.info(`[Metrics] Prompt "${prompt.name}" latency: ${elapsed.toFixed(2)}ms`);
      return res;
    });

    // 6. Validation Middleware
    this.pipeline.use(async (context, prompt, next) => {
      // Validate inputs
      const rawArgs = { ...context.params };
      delete rawArgs.name;
      PromptValidator.validateInput(prompt.arguments, rawArgs as Record<string, string>);

      const res = await next();

      // Validate outputs
      PromptValidator.validateOutput(res);
      return res;
    });
  }

  /**
   * Safe mapping of general errors to specific PromptException models.
   */
  private translateError(err: unknown): Error {
    if (err instanceof PromptError) {
      return err;
    }
    const msg = err instanceof Error ? err.message : String(err);
    return new PromptExecutionError(msg);
  }
}
