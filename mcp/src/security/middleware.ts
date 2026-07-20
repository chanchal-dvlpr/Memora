import { SecurityContext, SecurityAction, AuditLevel } from '../types/security';
import { AuthenticationManager } from './authentication';
import { AuthorizationManager } from './authorization';
import { AuditLogger } from './audit';
import { AuthenticationError, PermissionDeniedError } from '../errors';

export type SecurityMiddlewareNext<TResult> = () => Promise<TResult>;

export type SecurityMiddleware<TContext extends { securityContext: SecurityContext; name: string }, TResult> = (
  context: TContext,
  action: SecurityAction,
  next: SecurityMiddlewareNext<TResult>
) => Promise<TResult>;

export class SecurityMiddlewarePipeline<TContext extends { securityContext: SecurityContext; name: string }, TResult> {
  private middlewares: SecurityMiddleware<TContext, TResult>[] = [];

  public use(middleware: SecurityMiddleware<TContext, TResult>): void {
    this.middlewares.push(middleware);
  }

  public async execute(
    context: TContext,
    action: SecurityAction,
    finalHandler: () => Promise<TResult>
  ): Promise<TResult> {
    let index = -1;

    const dispatch = async (i: number): Promise<TResult> => {
      if (i <= index) {
        throw new Error('next() called multiple times');
      }
      index = i;
      const middleware = this.middlewares[i];
      if (middleware) {
        return middleware(context, action, () => dispatch(i + 1));
      }
      return finalHandler();
    };

    return dispatch(0);
  }
}

/**
 * Deterministic Authentication Middleware Creator
 */
export function createAuthenticationMiddleware<TContext extends { securityContext: SecurityContext; name: string }, TResult>(
  authManager: AuthenticationManager,
  providerName: string,
  getCredentials: (context: TContext) => Record<string, string>
): SecurityMiddleware<TContext, TResult> {
  return async (context, _action, next) => {
    const credentials = getCredentials(context);
    const authResult = await authManager.authenticate(providerName, { credentials });
    if (!authResult.success || !authResult.principal) {
      throw new AuthenticationError(authResult.error || 'Authentication failed.');
    }
    // Update the context principal
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const mutableContext = context as any;
    mutableContext.securityContext = {
      ...context.securityContext,
      principal: authResult.principal,
      roles: [...authResult.principal.roles],
    };
    return next();
  };
}

/**
 * Deterministic Authorization Middleware Creator
 */
export function createAuthorizationMiddleware<TContext extends { securityContext: SecurityContext; name: string }, TResult>(
  authzManager: AuthorizationManager,
  getRequiredPermission: (context: TContext) => string | undefined,
  getPolicyName: (context: TContext) => string | undefined
): SecurityMiddleware<TContext, TResult> {
  return async (context, action, next) => {
    const requiredPermission = getRequiredPermission(context);
    const policyName = getPolicyName(context);
    const authzResult = await authzManager.authorize(
      context.securityContext,
      action,
      context.name,
      requiredPermission,
      policyName
    );

    if (!authzResult.allowed) {
      throw new PermissionDeniedError(authzResult.reason || 'Permission denied.');
    }

    return next();
  };
}

/**
 * Deterministic Audit Middleware Creator
 */
export function createAuditMiddleware<TContext extends { securityContext: SecurityContext; name: string }, TResult>(
  auditLogger: AuditLogger,
  category: string
): SecurityMiddleware<TContext, TResult> {
  return async (context, action, next) => {
    const actor = context.securityContext.principal?.name || 'anonymous';
    try {
      const result = await next();
      auditLogger.log(
        AuditLevel.INFO,
        category,
        action,
        `Executed action "${action}" on target "${context.name}" successfully.`,
        'success',
        actor
      );
      return result;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      auditLogger.log(
        AuditLevel.WARNING,
        category,
        action,
        `Failed execution of action "${action}" on target "${context.name}". Error: ${message}`,
        'failure',
        actor
      );
      throw err;
    }
  };
}
