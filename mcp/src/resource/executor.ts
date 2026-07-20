import { ResourceRegistry } from '../registry/resource';
import { ResourceExecutionContext, ResourceReadResult, ResourceContents } from '../types/resource';
import { ResourceValidator } from './validator';
import { ResourceMiddlewarePipeline, ResourceMiddleware } from './middleware';
import {
  ResourceNotFoundError,
  ResourceExecutionError,
  ResourceError,
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

export class ResourceExecutor {
  private readonly registry: ResourceRegistry;

  constructor(registry: ResourceRegistry) {
    this.registry = registry;
  }

  /**
   * Invokes the resource's handler to retrieve the raw resource contents.
   */
  public async execute(
    uri: string,
    context: ResourceExecutionContext
  ): Promise<ResourceReadResult> {
    const validator = new ResourceValidator();
    
    // Normalize incoming URI immediately
    const normalizedUri = validator.validateUri(uri);

    const resource = this.registry.getResource(normalizedUri);
    if (!resource) {
      throw new ResourceNotFoundError(`Resource not found for URI: "${normalizedUri}"`);
    }

    try {
      const res = await resource.handler(context.params || {}, context);

      // Adapt result if not already formatted as a standard contents array
      let contents: ResourceContents[];
      if (Array.isArray(res)) {
        contents = res;
      } else {
        contents = [
          {
            uri: resource.uri,
            mimeType: resource.mimeType || 'text/plain',
            text: typeof res === 'string' ? res : JSON.stringify(res),
          },
        ];
      }

      return { contents };
    } catch (err: unknown) {
      if (err instanceof ResourceError) {
        throw err;
      }
      const msg = err instanceof Error ? err.message : String(err);
      throw new ResourceExecutionError(`Resource execution failed: ${msg}`);
    }
  }
}

export class ResourceDispatcher {
  private readonly registry: ResourceRegistry;
  private readonly pipeline: ResourceMiddlewarePipeline;
  private readonly authManager?: AuthenticationManager;
  private readonly authzManager?: AuthorizationManager;
  private readonly auditLogger?: AuditLogger;
  private readonly config?: ServerConfig;
  private readonly sessionManager?: SessionManager;

  constructor(
    registry: ResourceRegistry,
    authManager?: AuthenticationManager,
    authzManager?: AuthorizationManager,
    auditLogger?: AuditLogger,
    config?: ServerConfig,
    sessionManager?: SessionManager
  ) {
    this.registry = registry;
    this.pipeline = new ResourceMiddlewarePipeline();
    this.authManager = authManager;
    this.authzManager = authzManager;
    this.auditLogger = auditLogger;
    this.config = config;
    this.sessionManager = sessionManager;

    this.registerDefaultMiddlewares();
  }

  /**
   * Registers a custom resource middleware into the pipeline.
   */
  public use(middleware: ResourceMiddleware): void {
    this.pipeline.use(middleware);
  }

  /**
   * Coordinates registry lookup and triggers middleware-wrapped execution.
   */
  public async dispatchRead(
    uri: string,
    context: ResourceExecutionContext
  ): Promise<ResourceReadResult> {
    const validator = new ResourceValidator();
    
    // Normalize incoming URI immediately
    const normalizedUri = validator.validateUri(uri);

    const resource = this.registry.getResource(normalizedUri);
    if (!resource) {
      throw new ResourceNotFoundError(`Resource not found for URI: "${normalizedUri}"`);
    }

    // Attach params to context
    context.params = context.params || {};

    // Trigger execution through pipeline wrapping resource handler execution
    return this.pipeline.execute(context, resource, async () => {
      const executor = new ResourceExecutor(this.registry);
      return executor.execute(normalizedUri, context);
    });
  }

  /**
   * Sets up default middleware hooks in deterministic order:
   * Authentication -> Authorization -> Session (Validation -> Lookup/Create -> Touch) -> Audit -> Logging -> Timing -> Validation -> Execution
   */
  private registerDefaultMiddlewares(): void {
    // 1. Authentication Middleware
    this.pipeline.use(async (context, _resource, next) => {
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
    this.pipeline.use(async (context, resource, next) => {
      if (this.config?.securityEnabled === false) {
        return next();
      }

      if (context.metadata.get('auth_blocked') === true) {
        throw new ResourceExecutionError(`Authorization blocked for resource: ${resource.uri}`);
      }

      if (this.authzManager && context.securityContext) {
        const policy = this.config?.defaultAuthzPolicy || 'allow-all';
        const permissionName = policy !== 'allow-all'
          ? (resource.requiredPermission || `memora://resources/${resource.name}`)
          : undefined;

        const authzResult = await this.authzManager.authorize(
          context.securityContext,
          SecurityAction.READ,
          `memora://resources/${resource.name}`,
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
    this.pipeline.use(async (context, _resource, next) => {
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
    this.pipeline.use(async (context, resource, next) => {
      if (this.config?.auditLogEnabled === false || !this.auditLogger) {
        return next();
      }

      const actor = context.securityContext?.principal?.name || 'anonymous';
      try {
        const res = await next();
        this.auditLogger.log(
          AuditLevel.INFO,
          'resource-execution',
          'read',
          `Executed resource read on "${resource.uri}" successfully.`,
          'success',
          actor
        );
        return res;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        this.auditLogger.log(
          AuditLevel.WARNING,
          'resource-execution',
          'read',
          `Failed resource read on "${resource.uri}". Error: ${msg}`,
          'failure',
          actor
        );
        throw err;
      }
    });

    // 4. Logging Middleware
    this.pipeline.use(async (context, resource, next) => {
      context.logger.debug(`Reading resource "${resource.uri}"`, { requestId: context.requestId });
      try {
        const res = await next();
        context.logger.debug(`Resource "${resource.uri}" read successfully`, { requestId: context.requestId });
        return res;
      } catch (err) {
        context.logger.error(`Read failed for "${resource.uri}": ${err instanceof Error ? err.message : String(err)}`);
        throw err;
      }
    });

    // 5. Timing Middleware
    this.pipeline.use(async (context, resource, next) => {
      const start = performance.now();
      const res = await next();
      const elapsed = performance.now() - start;
      context.logger.info(`[Resource Metrics] Resource "${resource.uri}" latency: ${elapsed.toFixed(2)}ms`);
      return res;
    });

    // 6. Validation Middleware
    this.pipeline.use(async (_context, resource, next) => {
      const validator = new ResourceValidator();
      
      // Validate inputs
      validator.validateUri(resource.uri);

      const res = await next();

      // Validate outputs
      validator.validateContents(res.contents, resource.uri);
      return res;
    });
  }
}
