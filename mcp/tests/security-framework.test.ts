/* eslint-disable @typescript-eslint/no-unused-vars, @typescript-eslint/no-explicit-any */
import {
  AuthenticationManager,
  MockAuthenticationProvider,
  AuthorizationManager,
  SecurityContextBuilder,
  InMemoryAuditLogger,
  SecurityMiddlewarePipeline,
  createAuthenticationMiddleware,
  createAuthorizationMiddleware,
  createAuditMiddleware,
} from '../src/security';
import { SecurityAction, AuditLevel, SecurityContext, Permission } from '../src/types/security';
import {
  AuthenticationError,
  InvalidCredentialError,
} from '../src/errors';

describe('Security & Permission Framework Tests', () => {
  describe('AuthenticationManager & MockAuthenticationProvider', () => {
    let manager: AuthenticationManager;
    let provider: MockAuthenticationProvider;

    beforeEach(() => {
      manager = new AuthenticationManager();
      provider = new MockAuthenticationProvider();
      manager.registerProvider(provider);
    });

    it('should authenticate user with valid credentials', async () => {
      const response = await manager.authenticate('mock', {
        credentials: { username: 'user', password: 'user-password' },
      });

      expect(response.success).toBe(true);
      expect(response.principal).toBeDefined();
      expect(response.principal?.name).toBe('user');
      expect(response.principal?.roles).toContain('developer');
      expect(response.token).toBe('mock-user-token');
    });

    it('should authenticate admin with valid credentials', async () => {
      const response = await manager.authenticate('mock', {
        credentials: { username: 'admin', password: 'admin-password' },
      });

      expect(response.success).toBe(true);
      expect(response.principal?.name).toBe('admin');
      expect(response.principal?.roles).toContain('admin');
      expect(response.token).toBe('mock-admin-token');
    });

    it('should authenticate with a valid mock token directly', async () => {
      const response = await manager.authenticate('mock', {
        credentials: { token: 'valid-mock-token' },
      });

      expect(response.success).toBe(true);
      expect(response.principal?.name).toBe('mock-user');
    });

    it('should fail authentication with invalid credentials', async () => {
      await expect(
        manager.authenticate('mock', {
          credentials: { username: 'user', password: 'wrong-password' },
        })
      ).rejects.toThrow(InvalidCredentialError);
    });

    it('should fail direct token authenticate with invalid token', async () => {
      await expect(
        manager.authenticate('mock', {
          credentials: { token: 'invalid-token' },
        })
      ).rejects.toThrow(InvalidCredentialError);
    });

    it('should refresh token session successfully', async () => {
      // First authenticate to register token
      const authRes = await manager.authenticate('mock', {
        credentials: { username: 'user', password: 'user-password' },
      });

      const refreshRes = await manager.refresh('mock', authRes.token!);
      expect(refreshRes.success).toBe(true);
      expect(refreshRes.principal?.name).toBe('user');
    });

    it('should invalidate token session successfully', async () => {
      const authRes = await manager.authenticate('mock', {
        credentials: { username: 'user', password: 'user-password' },
      });

      await manager.invalidate('mock', authRes.token!);

      await expect(manager.refresh('mock', authRes.token!)).rejects.toThrow(
        InvalidCredentialError
      );
    });
  });

  describe('AuthorizationManager & Evaluators', () => {
    let authzManager: AuthorizationManager;

    beforeEach(() => {
      authzManager = new AuthorizationManager();
    });

    it('should allow all requests under allow-all policy by default', async () => {
      const builder = new SecurityContextBuilder();
      const context = builder.build();

      const result = await authzManager.authorize(context, SecurityAction.EXECUTE, 'target');
      expect(result.allowed).toBe(true);
    });

    it('should evaluate permissions matching target regex and actions', async () => {
      const permission: Permission = {
        name: 'tools:run',
        scope: {
          target: 'memora://tools/*',
          actions: [SecurityAction.EXECUTE],
        },
      };

      const context = new SecurityContextBuilder()
        .addPermission(permission)
        .build();

      const evaluator = authzManager.getPermissionEvaluator();
      
      // Match target and action
      expect(
        evaluator.checkPermission(context, 'tools:run', SecurityAction.EXECUTE, 'memora://tools/search')
      ).toBe(true);

      // Mismatched action
      expect(
        evaluator.checkPermission(context, 'tools:run', SecurityAction.READ, 'memora://tools/search')
      ).toBe(false);

      // Mismatched target scope pattern
      expect(
        evaluator.checkPermission(context, 'tools:run', SecurityAction.EXECUTE, 'memora://prompts/search')
      ).toBe(false);
    });

    it('should evaluate custom registered policies', async () => {
      const policyEvaluator = authzManager.getPolicyEvaluator();

      policyEvaluator.registerPolicy({
        name: 'deny-write',
        evaluate: async (_ctx, action) => action !== SecurityAction.WRITE,
      });

      const context = new SecurityContextBuilder().build();

      const readResult = await authzManager.authorize(
        context,
        SecurityAction.READ,
        'my-resource',
        undefined,
        'deny-write'
      );
      expect(readResult.allowed).toBe(true);

      const writeResult = await authzManager.authorize(
        context,
        SecurityAction.WRITE,
        'my-resource',
        undefined,
        'deny-write'
      );
      expect(writeResult.allowed).toBe(false);
      expect(writeResult.reason).toContain('Access denied by policy');
    });

    it('should deny access if required permission is missing', async () => {
      const context = new SecurityContextBuilder().build();

      const result = await authzManager.authorize(
        context,
        SecurityAction.EXECUTE,
        'my-tool',
        'tools:run'
      );

      expect(result.allowed).toBe(false);
      expect(result.reason).toContain('lacks required permission');
    });
  });

  describe('SecurityContextBuilder Immutability', () => {
    it('should compile an immutable security context structure', () => {
      const context = new SecurityContextBuilder()
        .addRole('developer')
        .addPermission({
          name: 'read-docs',
          scope: { target: '*', actions: [SecurityAction.READ] },
        })
        .addMetadata('env', 'test')
        .build();

      expect(Object.isFrozen(context)).toBe(true);
      expect(Object.isFrozen(context.roles)).toBe(true);
      expect(Object.isFrozen(context.permissions)).toBe(true);
      expect(Object.isFrozen(context.permissions[0])).toBe(true);
      expect(Object.isFrozen(context.permissions[0].scope)).toBe(true);

      // Mutation checks should throw errors in strict mode
      expect(() => {
        (context as any).roles = ['attacker'];
      }).toThrow();

      expect(() => {
        (context.permissions as any).push({ name: 'evil' });
      }).toThrow();
    });
  });

  describe('InMemoryAuditLogger', () => {
    it('should log and filter entries', () => {
      const logger = new InMemoryAuditLogger();

      logger.log(AuditLevel.INFO, 'authentication', 'login', 'User logged in', 'success', 'user1');
      logger.log(AuditLevel.ALERT, 'security-failure', 'abuse', 'Unauthorized write', 'failure', 'anonymous');

      const all = logger.getEntries();
      expect(all).toHaveLength(2);

      const successes = logger.getEntries({ outcome: 'success' });
      expect(successes).toHaveLength(1);
      expect(successes[0].actor).toBe('user1');

      const failures = logger.getEntries({ outcome: 'failure' });
      expect(failures).toHaveLength(1);
      expect(failures[0].level).toBe(AuditLevel.ALERT);

      logger.clear();
      expect(logger.getEntries()).toHaveLength(0);
    });
  });

  describe('SecurityMiddlewarePipeline', () => {
    let authManager: AuthenticationManager;
    let authzManager: AuthorizationManager;
    let auditLogger: InMemoryAuditLogger;

    beforeEach(() => {
      authManager = new AuthenticationManager();
      const mockProvider = new MockAuthenticationProvider();
      authManager.registerProvider(mockProvider);

      authzManager = new AuthorizationManager();
      auditLogger = new InMemoryAuditLogger();
    });

    it('should execute middleware sequentially in deterministic order', async () => {
      const pipeline = new SecurityMiddlewarePipeline<{ securityContext: SecurityContext; name: string; creds: Record<string, string> }, string>();

      // Register authentication middleware
      pipeline.use(
        createAuthenticationMiddleware(authManager, 'mock', (ctx) => ctx.creds)
      );

      // Register authorization middleware
      pipeline.use(
        createAuthorizationMiddleware(
          authzManager,
          () => 'tools:run',
          () => undefined
        )
      );

      // Register audit middleware
      pipeline.use(
        createAuditMiddleware(auditLogger, 'tool-execution')
      );

      const securityContext = new SecurityContextBuilder().build();

      const context = {
        securityContext,
        name: 'memora://tools/search',
        creds: { username: 'admin', password: 'admin-password' },
      };

      // Set up required permission on admin context dynamically
      // (The middleware authenticates the admin. But we need to make sure the authenticated context has permissions!)
      // To simulate, we mock permissions check or let policy deny/allow.
      // Since admin role evaluates, let's configure permissionEvaluator checking tools:run:
      jest.spyOn(authzManager.getPermissionEvaluator(), 'checkPermission').mockReturnValue(true);

      const result = await pipeline.execute(context, SecurityAction.EXECUTE, async () => {
        expect(context.securityContext.principal?.name).toBe('admin');
        return 'executed';
      });

      expect(result).toBe('executed');
      
      const auditEntries = auditLogger.getEntries();
      expect(auditEntries).toHaveLength(1);
      expect(auditEntries[0].outcome).toBe('success');
      expect(auditEntries[0].actor).toBe('admin');
    });

    it('should abort pipeline on authentication failure', async () => {
      const pipeline = new SecurityMiddlewarePipeline<{ securityContext: SecurityContext; name: string; creds: Record<string, string> }, string>();

      pipeline.use(
        createAuthenticationMiddleware(authManager, 'mock', (ctx) => ctx.creds)
      );

      const securityContext = new SecurityContextBuilder().build();
      const context = {
        securityContext,
        name: 'my-target',
        creds: { username: 'admin', password: 'wrong-password' },
      };

      await expect(
        pipeline.execute(context, SecurityAction.EXECUTE, async () => 'done')
      ).rejects.toThrow(AuthenticationError);
    });
  });
});
