import { SessionManager } from './manager';
import { Session, SessionId } from '../types/session';

export type SessionMiddlewareNext<TResult> = () => Promise<TResult>;

export type SessionMiddleware<TContext, TResult> = (
  context: TContext,
  next: SessionMiddlewareNext<TResult>
) => Promise<TResult>;

export class SessionMiddlewarePipeline<TContext, TResult> {
  private readonly middlewares: SessionMiddleware<TContext, TResult>[] = [];

  public use(middleware: SessionMiddleware<TContext, TResult>): this {
    this.middlewares.push(middleware);
    return this;
  }

  public async execute(context: TContext, target: () => Promise<TResult>): Promise<TResult> {
    let index = -1;

    const dispatch = async (i: number): Promise<TResult> => {
      if (i <= index) {
        throw new Error('next() called multiple times within session middleware');
      }
      index = i;

      if (i === this.middlewares.length) {
        return target();
      }

      const middleware = this.middlewares[i];
      return middleware(context, () => dispatch(i + 1));
    };

    return dispatch(0);
  }
}

// --- Standard Reusable Middleware Factory Functions ---

export interface SessionContextHolder {
  sessionId?: SessionId;
  session?: Session;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
}

/**
 * Middleware that validates the session existence and expiration state.
 */
export function sessionValidationMiddleware<TContext extends SessionContextHolder, TResult>(
  manager: SessionManager
): SessionMiddleware<TContext, TResult> {
  return async (context, next) => {
    if (context.sessionId) {
      await manager.validateSession(context.sessionId);
    }
    return next();
  };
}

/**
 * Middleware that creates a new session if context.sessionId is provided but not yet opened.
 */
export function sessionCreationMiddleware<TContext extends SessionContextHolder, TResult>(
  manager: SessionManager
): SessionMiddleware<TContext, TResult> {
  return async (context, next) => {
    if (context.sessionId && !manager.getRegistry().hasSession(context.sessionId)) {
      context.session = await manager.openSession(context.sessionId, context.clientMetadata);
    }
    return next();
  };
}

/**
 * Middleware that looks up an existing session from the registry and attaches it to the execution context.
 */
export function sessionLookupMiddleware<TContext extends SessionContextHolder, TResult>(
  manager: SessionManager
): SessionMiddleware<TContext, TResult> {
  return async (context, next) => {
    if (context.sessionId) {
      const session = manager.getRegistry().getSession(context.sessionId);
      if (session) {
        context.session = session;
      }
    }
    return next();
  };
}

/**
 * Middleware that touches the session to refresh its lastAccessedAt timestamp.
 */
export function sessionTouchMiddleware<TContext extends SessionContextHolder, TResult>(
  manager: SessionManager
): SessionMiddleware<TContext, TResult> {
  return async (context, next) => {
    if (context.sessionId && manager.getRegistry().hasSession(context.sessionId)) {
      context.session = await manager.touchSession(context.sessionId);
    }
    return next();
  };
}

/**
 * Middleware that checks session expiration policy during pipeline dispatch.
 */
export function sessionExpirationMiddleware<TContext extends SessionContextHolder, TResult>(
  manager: SessionManager
): SessionMiddleware<TContext, TResult> {
  return async (context, next) => {
    if (context.sessionId && manager.getRegistry().hasSession(context.sessionId)) {
      await manager.validateSession(context.sessionId);
    }
    return next();
  };
}
