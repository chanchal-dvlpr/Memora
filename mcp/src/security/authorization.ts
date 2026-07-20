import { SecurityContext, SecurityAction, PermissionPolicy, AuthorizationResult } from '../types/security';
import { AuthorizationError } from '../errors';

export class PermissionEvaluator {
  /**
   * Evaluates if a given security context possesses permission to perform action on a target.
   */
  public checkPermission(context: SecurityContext, permissionName: string, action: SecurityAction, target: string): boolean {
    const matchingPermissions = context.permissions.filter((p) => p.name === permissionName);
    if (matchingPermissions.length === 0) {
      return false;
    }

    for (const p of matchingPermissions) {
      if (p.scope) {
        // Glob pattern matcher for target (e.g. '*' matches everything, or exact match)
        const targetPattern = new RegExp('^' + p.scope.target.replace(/\*/g, '.*') + '$');
        if (targetPattern.test(target) && p.scope.actions.includes(action)) {
          return true;
        }
      } else {
        // If permission has no scope, it's a global permission representing access
        return true;
      }
    }

    return false;
  }
}

export class PolicyEvaluator {
  private policies = new Map<string, PermissionPolicy>();

  public registerPolicy(policy: PermissionPolicy): void {
    if (this.policies.has(policy.name)) {
      throw new AuthorizationError(`Policy "${policy.name}" is already registered.`);
    }
    this.policies.set(policy.name, policy);
  }

  public async evaluatePolicy(policyName: string, context: SecurityContext, action: SecurityAction, target: string): Promise<boolean> {
    const policy = this.policies.get(policyName);
    if (!policy) {
      throw new AuthorizationError(`Policy "${policyName}" is not registered.`);
    }
    return policy.evaluate(context, action, target);
  }
}

export class AuthorizationManager {
  private permissionEvaluator: PermissionEvaluator;
  private policyEvaluator: PolicyEvaluator;
  private defaultPolicy = 'allow-all';

  constructor(permissionEvaluator = new PermissionEvaluator(), policyEvaluator = new PolicyEvaluator()) {
    this.permissionEvaluator = permissionEvaluator;
    this.policyEvaluator = policyEvaluator;

    // 1. Default allow-all policy baseline
    this.policyEvaluator.registerPolicy({
      name: 'allow-all',
      description: 'Default allow all policy baseline',
      evaluate: async () => true,
    });

    // 2. Deny-all policy
    this.policyEvaluator.registerPolicy({
      name: 'deny-all',
      description: 'Deny all requests policy',
      evaluate: async () => false,
    });

    // 3. Permission-based policy
    this.policyEvaluator.registerPolicy({
      name: 'permission-based',
      description: 'Require matching permission scope for target/action',
      evaluate: async (context, action, target) => {
        // Use PermissionEvaluator logic to check all permissions in securityContext
        for (const p of context.permissions) {
          if (p.scope) {
            const targetPattern = new RegExp('^' + p.scope.target.replace(/\*/g, '.*') + '$');
            if (targetPattern.test(target) && p.scope.actions.includes(action)) {
              return true;
            }
          } else {
            // Permission without scope checks exact name match
            if (p.name === target) {
              return true;
            }
          }
        }
        return false;
      },
    });
  }

  public getPermissionEvaluator(): PermissionEvaluator {
    return this.permissionEvaluator;
  }

  public getPolicyEvaluator(): PolicyEvaluator {
    return this.policyEvaluator;
  }

  public setDefaultPolicy(policyName: string): void {
    this.defaultPolicy = policyName;
  }

  public async authorize(
    context: SecurityContext,
    action: SecurityAction,
    target: string,
    requiredPermission?: string,
    policyName?: string
  ): Promise<AuthorizationResult> {
    // 1. Run permission check if required
    if (requiredPermission) {
      const hasPerm = this.permissionEvaluator.checkPermission(context, requiredPermission, action, target);
      if (!hasPerm) {
        return {
          allowed: false,
          reason: `Principal "${context.principal?.name || 'anonymous'}" lacks required permission "${requiredPermission}".`,
        };
      }
    }

    // 2. Evaluate policy
    const targetPolicy = policyName || this.defaultPolicy;
    const allowedByPolicy = await this.policyEvaluator.evaluatePolicy(targetPolicy, context, action, target);
    if (!allowedByPolicy) {
      return {
        allowed: false,
        reason: `Access denied by policy "${targetPolicy}".`,
      };
    }

    return { allowed: true };
  }
}
