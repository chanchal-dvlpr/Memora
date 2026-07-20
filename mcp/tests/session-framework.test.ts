/* eslint-disable @typescript-eslint/no-explicit-any */
import { SessionRegistry } from '../src/session/registry';
import { SessionManager } from '../src/session/manager';
import { ContextStore } from '../src/session/store';
import { SessionContextBuilder, SessionContextResolver } from '../src/session/context';
import { ExpirationEvaluator } from '../src/session/expiration';
import { SessionCleanupManager } from '../src/session/cleanup';
import {
  SessionMiddlewarePipeline,
  sessionValidationMiddleware,
  sessionCreationMiddleware,
  sessionLookupMiddleware,
  sessionTouchMiddleware,
  sessionExpirationMiddleware,
} from '../src/session/middleware';
import { DuplicateSessionError, SessionNotFoundError } from '../src/errors';
import { SecurityContextBuilder } from '../src/security/context';
import { SecurityAction } from '../src/types/security';
import { ToolDispatcher } from '../src/tool/executor';
import { ToolRegistry } from '../src/registry/tool';
import { ResourceDispatcher } from '../src/resource/executor';
import { ResourceRegistry } from '../src/registry/resource';
import { PromptDispatcher } from '../src/prompt/executor';
import { PromptRegistry } from '../src/registry/prompt';
import { ToolVisibility, ToolCategory } from '../src/types/tool';
import { ResourceVisibility, ResourceCategory } from '../src/types/resource';
import { PromptVisibility, PromptCategory } from '../src/types/prompt';

describe('Session & Context Management Framework Tests', () => {
  describe('SessionRegistry', () => {
    let registry: SessionRegistry;

    beforeEach(() => {
      registry = new SessionRegistry();
    });

    it('should create and retrieve a session', () => {
      const session = registry.createSession('session-1', { createdAt: Date.now() });
      expect(session.id).toBe('session-1');
      expect(session.state).toBe('active');

      const retrieved = registry.getSession('session-1');
      expect(retrieved).toBeDefined();
      expect(retrieved?.id).toBe('session-1');
    });

    it('should prevent duplicate session IDs', () => {
      registry.createSession('session-1', { createdAt: Date.now() });
      expect(() => {
        registry.createSession('session-1', { createdAt: Date.now() });
      }).toThrow(DuplicateSessionError);
    });

    it('should update session attributes and state', () => {
      registry.createSession('session-1', { createdAt: Date.now() }, { initial: 'value' });
      const updated = registry.updateSession('session-1', {
        state: 'inactive',
        attributes: { next: 'value2' },
      });

      expect(updated.state).toBe('inactive');
      expect(updated.attributes.initial).toBe('value');
      expect(updated.attributes.next).toBe('value2');
    });

    it('should throw SessionNotFoundError when updating non-existent session', () => {
      expect(() => {
        registry.updateSession('non-existent', { state: 'inactive' });
      }).toThrow(SessionNotFoundError);
    });

    it('should remove a session', () => {
      registry.createSession('session-1', { createdAt: Date.now() });
      expect(registry.hasSession('session-1')).toBe(true);

      registry.removeSession('session-1');
      expect(registry.hasSession('session-1')).toBe(false);
      expect(registry.getSession('session-1')).toBeUndefined();
    });

    it('should throw SessionNotFoundError when removing non-existent session', () => {
      expect(() => {
        registry.removeSession('non-existent');
      }).toThrow(SessionNotFoundError);
    });

    it('should return deep-frozen immutable session views', () => {
      const session = registry.createSession('session-1', { createdAt: Date.now() }, { test: 'data' });
      expect(Object.isFrozen(session)).toBe(true);
      expect(Object.isFrozen(session.attributes)).toBe(true);
      expect(Object.isFrozen(session.metadata)).toBe(true);

      expect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (session as any).state = 'closed';
      }).toThrow();
    });

    it('should list sessions in deterministic order', () => {
      registry.createSession('session-b', { createdAt: 1000 });
      registry.createSession('session-a', { createdAt: 1000 });
      registry.createSession('session-c', { createdAt: 500 });

      const list = registry.listSessions();
      expect(list).toHaveLength(3);
      expect(list[0].id).toBe('session-c');
      expect(list[1].id).toBe('session-a');
      expect(list[2].id).toBe('session-b');
    });
  });

  describe('ContextStore', () => {
    let store: ContextStore;

    beforeEach(() => {
      store = new ContextStore();
    });

    it('should perform CRUD context storage operations associated with a session', () => {
      store.put('session-1', 'key1', 'value1');
      store.put('session-1', 'key2', { nested: 'obj' });

      expect(store.contains('session-1', 'key1')).toBe(true);
      expect(store.get('session-1', 'key1')).toBe('value1');
      expect(store.get('session-1', 'key2')).toEqual({ nested: 'obj' });

      store.remove('session-1', 'key1');
      expect(store.contains('session-1', 'key1')).toBe(false);
      expect(store.get('session-1', 'key1')).toBeUndefined();
    });

    it('should clear stored contexts', () => {
      store.put('session-1', 'key1', 'value1');
      store.put('session-2', 'key2', 'value2');

      store.clear('session-1');
      expect(store.contains('session-1', 'key1')).toBe(false);
      expect(store.contains('session-2', 'key2')).toBe(true);

      store.clear();
      expect(store.contains('session-2', 'key2')).toBe(false);
    });
  });

  describe('ExpirationEvaluator Strategies', () => {
    let evaluator: ExpirationEvaluator;

    beforeEach(() => {
      evaluator = new ExpirationEvaluator();
    });

    it('should evaluate none expiration policy', () => {
      const session = { state: 'active', metadata: { createdAt: 100 }, lastAccessedAt: 100 } as any;
      const res = evaluator.evaluate(session, { expirationType: 'none' });
      expect(res.isExpired).toBe(false);
    });

    it('should evaluate manual expiration policy', () => {
      const session = { state: 'active', isManuallyExpired: true, metadata: { createdAt: 100 }, lastAccessedAt: 100 } as any;
      const res = evaluator.evaluate(session, { expirationType: 'manual' });
      expect(res.isExpired).toBe(true);
      expect(res.reason).toContain('manually');
    });

    it('should evaluate absolute expiration policy', () => {
      const now = 1000;
      const session = { state: 'active', metadata: { createdAt: 500 }, lastAccessedAt: 500 } as any;

      const resNotExpired = evaluator.evaluate(session, { expirationType: 'absolute', absoluteTimeoutMs: 600 }, now);
      expect(resNotExpired.isExpired).toBe(false);

      const resExpired = evaluator.evaluate(session, { expirationType: 'absolute', absoluteTimeoutMs: 400 }, now);
      expect(resExpired.isExpired).toBe(true);
      expect(resExpired.reason).toContain('Absolute timeout');
    });

    it('should evaluate sliding expiration policy', () => {
      const now = 1000;
      const session = { state: 'active', metadata: { createdAt: 100 }, lastAccessedAt: 800 } as any;

      const resNotExpired = evaluator.evaluate(session, { expirationType: 'sliding', maxIdleTimeMs: 300 }, now);
      expect(resNotExpired.isExpired).toBe(false);

      const resExpired = evaluator.evaluate(session, { expirationType: 'sliding', maxIdleTimeMs: 150 }, now);
      expect(resExpired.isExpired).toBe(true);
      expect(resExpired.reason).toContain('Sliding idle timeout');
    });
  });

  describe('SessionCleanupManager & Orphan Context Removal', () => {
    it('should purge expired sessions and orphan context entries', async () => {
      const registry = new SessionRegistry();
      const store = new ContextStore();
      const cleanupManager = new SessionCleanupManager(registry, store, {
        expirationType: 'absolute',
        absoluteTimeoutMs: 100,
      });

      // Session 1: expired
      registry.createSession('session-1', { createdAt: 1000 });
      store.put('session-1', 'cacheKey', 'cacheVal');

      // Session 2: active
      registry.createSession('session-2', { createdAt: Date.now() });
      store.put('session-2', 'cacheKey2', 'cacheVal2');

      // Orphan Session 3 (in store but never in registry)
      store.put('session-orphan', 'orphanKey', 'orphanVal');

      const stats = await cleanupManager.cleanup();
      expect(stats.cleanupRuns).toBe(1);
      expect(stats.removedSessionsCount).toBe(1);
      expect(stats.removedContextsCount).toBe(1);

      expect(registry.hasSession('session-1')).toBe(false);
      expect(registry.hasSession('session-2')).toBe(true);
      expect(store.contains('session-1', 'cacheKey')).toBe(false);
      expect(store.contains('session-2', 'cacheKey2')).toBe(true);
    });

    it('should support scheduled interval auto-cleanup', async () => {
      const registry = new SessionRegistry();
      const store = new ContextStore();
      const cleanupManager = new SessionCleanupManager(registry, store, { expirationType: 'none' });

      cleanupManager.startAutoCleanup(20);
      await new Promise((resolve) => setTimeout(resolve, 50));
      cleanupManager.stopAutoCleanup();

      const stats = cleanupManager.getStatistics();
      expect(stats.cleanupRuns).toBeGreaterThanOrEqual(1);
    });
  });

  describe('Expanded SessionContextBuilder & Resolver', () => {
    it('should propagate correlation IDs and rich metadata fields', () => {
      const securityContext = new SecurityContextBuilder()
        .addRole('admin')
        .addPermission({ name: 'all', scope: { target: '*', actions: [SecurityAction.EXECUTE] } })
        .build();

      const context = new SessionContextBuilder()
        .setRequestId('req-123')
        .setCorrelationId('corr-456')
        .setRequestMetadata({ header: 'val' })
        .setProtocolMetadata({ protocol: 'mcp-jsonrpc' })
        .setClientInformation({ name: 'vscode', version: '1.80' })
        .setExecutionMetadata({ thread: 1 })
        .setSecurityContext(securityContext)
        .build();

      const mockSession = { id: 's1', context } as any;

      expect(SessionContextResolver.resolveCorrelationId(mockSession)).toBe('corr-456');
      expect(SessionContextResolver.resolveRequestMetadata(mockSession).header).toBe('val');
      expect(SessionContextResolver.resolveProtocolMetadata(mockSession).protocol).toBe('mcp-jsonrpc');
      expect(SessionContextResolver.resolveClientInformation(mockSession).name).toBe('vscode');
      expect(SessionContextResolver.resolveExecutionMetadata(mockSession).thread).toBe(1);
    });
  });

  describe('SessionMiddlewarePipeline', () => {
    it('should execute middlewares in deterministic onion order', async () => {
      const pipeline = new SessionMiddlewarePipeline<{ trace: string[] }, string>();

      pipeline.use(async (ctx, next) => {
        ctx.trace.push('m1-start');
        const res = await next();
        ctx.trace.push('m1-end');
        return res;
      });

      pipeline.use(async (ctx, next) => {
        ctx.trace.push('m2-start');
        const res = await next();
        ctx.trace.push('m2-end');
        return res;
      });

      const ctx: { trace: string[] } = { trace: [] };
      const result = await pipeline.execute(ctx, async () => {
        ctx.trace.push('target');
        return 'success';
      });

      expect(result).toBe('success');
      expect(ctx.trace).toEqual(['m1-start', 'm2-start', 'target', 'm2-end', 'm1-end']);
    });

    it('should execute standard reusable middleware hooks', async () => {
      const manager = new SessionManager();
      await manager.openSession('s1');

      const pipeline = new SessionMiddlewarePipeline<any, string>();
      pipeline.use(sessionCreationMiddleware(manager));
      pipeline.use(sessionLookupMiddleware(manager));
      pipeline.use(sessionTouchMiddleware(manager));
      pipeline.use(sessionValidationMiddleware(manager));
      pipeline.use(sessionExpirationMiddleware(manager));

      const reqCtx: Record<string, unknown> = { sessionId: 's1' };
      const res = await pipeline.execute(reqCtx, async () => 'ok');

      expect(res).toBe('ok');
      expect(reqCtx['session']).toBeDefined();
      expect(manager.getStatistics().touchCount).toBe(1);

      // Verify sessionCreationMiddleware auto-creates missing session
      const reqCtx2: Record<string, unknown> = { sessionId: 's2' };
      await pipeline.execute(reqCtx2, async () => 'ok');
      expect(manager.getRegistry().hasSession('s2')).toBe(true);
    });
  });

  describe('SessionManager Touch & Cleanup Metrics', () => {
    it('should track touchCount and cleanup statistics', async () => {
      const manager = new SessionManager();
      await manager.openSession('s1');
      await manager.touchSession('s1');
      await manager.touchSession('s1');

      const cleanupManager = manager.getCleanupManager();
      await cleanupManager.cleanup();

      const stats = manager.getStatistics();
      expect(stats.touchCount).toBe(2);
      expect(stats.cleanupCount).toBe(1);
    });
  });

  describe('Dispatcher Pipeline Integration & Context Propagation', () => {
    it('should integrate SessionManager into ToolDispatcher and propagate SessionContext', async () => {
      const toolRegistry = new ToolRegistry();
      toolRegistry.registerTool(
        {
          name: 'test_tool',
          description: 'test tool',
          inputSchema: { type: 'object' },
          handler: async (_params) => ({ content: [{ type: 'text', text: 'result' }] }),
        },
        { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
      );

      const sessionManager = new SessionManager();
      const dispatcher = new ToolDispatcher(toolRegistry, undefined, undefined, undefined, undefined, sessionManager);

      const context: any = {
        requestId: 'req-1',
        sessionId: 'session-tool-1',
        protocolVersion: '2024-11-05',
        timestamp: Date.now(),
        logger: console,
        params: {},
        metadata: new Map(),
      };

      const result = await dispatcher.dispatchCall('test_tool', {}, context);
      expect(result.content[0].text).toBe('result');
      expect(context.sessionContext).toBeDefined();
      expect(sessionManager.getRegistry().hasSession('session-tool-1')).toBe(true);

      // Verify touch count
      expect(sessionManager.getStatistics().touchCount).toBe(1);

      // Subsequent call reuses and touches session
      await dispatcher.dispatchCall('test_tool', {}, context);
      expect(sessionManager.getStatistics().touchCount).toBe(2);
    });

    it('should integrate SessionManager into ResourceDispatcher and propagate SessionContext', async () => {
      const resourceRegistry = new ResourceRegistry();
      resourceRegistry.registerResource(
        {
          uri: 'memora://res/1',
          name: 'res1',
          handler: async () => [{ uri: 'memora://res/1', text: 'content' }],
        },
        { visibility: ResourceVisibility.PUBLIC, categories: [ResourceCategory.UTILITY] }
      );

      const sessionManager = new SessionManager();
      const dispatcher = new ResourceDispatcher(resourceRegistry, undefined, undefined, undefined, undefined, sessionManager);

      const context: any = {
        requestId: 'req-res-1',
        sessionId: 'session-res-1',
        protocolVersion: '2024-11-05',
        timestamp: Date.now(),
        logger: console,
        params: {},
        metadata: new Map(),
      };

      const result = await dispatcher.dispatchRead('memora://res/1', context);
      expect(result.contents[0].text).toBe('content');
      expect(context.sessionContext).toBeDefined();
      expect(sessionManager.getRegistry().hasSession('session-res-1')).toBe(true);
    });

    it('should integrate SessionManager into PromptDispatcher and propagate SessionContext', async () => {
      const promptRegistry = new PromptRegistry();
      promptRegistry.registerPrompt(
        {
          name: 'test_prompt',
          handler: async () => [{ role: 'user', content: { type: 'text', text: 'hello' } }],
        },
        { visibility: PromptVisibility.PUBLIC, category: PromptCategory.SYSTEM }
      );

      const sessionManager = new SessionManager();
      const dispatcher = new PromptDispatcher(promptRegistry, undefined, undefined, undefined, undefined, sessionManager);

      const context: any = {
        requestId: 'req-prompt-1',
        sessionId: 'session-prompt-1',
        protocolVersion: '2024-11-05',
        timestamp: Date.now(),
        logger: console,
        params: {},
        metadata: new Map(),
      };

      const result = await dispatcher.dispatchGet('test_prompt', {}, context);
      expect(result.messages[0].role).toBe('user');
      expect(context.sessionContext).toBeDefined();
      expect(sessionManager.getRegistry().hasSession('session-prompt-1')).toBe(true);
    });

    it('should support concurrent sessions across pipeline dispatchers', async () => {
      const toolRegistry = new ToolRegistry();
      toolRegistry.registerTool(
        {
          name: 'conc_tool',
          inputSchema: { type: 'object' },
          handler: async () => ({ content: [{ type: 'text', text: 'ok' }] }),
        },
        { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
      );

      const sessionManager = new SessionManager();
      const dispatcher = new ToolDispatcher(toolRegistry, undefined, undefined, undefined, undefined, sessionManager);

      const promises = [1, 2, 3, 4, 5].map((i) => {
        const ctx: any = {
          requestId: `req-conc-${i}`,
          sessionId: `session-conc-${i}`,
          protocolVersion: '2024-11-05',
          timestamp: Date.now(),
          logger: console,
          params: {},
          metadata: new Map(),
        };
        return dispatcher.dispatchCall('conc_tool', {}, ctx);
      });

      await Promise.all(promises);
      expect(sessionManager.getStatistics().activeSessionsCount).toBe(5);
    });
  });
});
