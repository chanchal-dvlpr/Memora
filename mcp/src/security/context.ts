import { SecurityPrincipal, Permission, SecurityContext } from '../types/security';

export class SecurityContextBuilder {
  private principal?: SecurityPrincipal;
  private roles: string[] = [];
  private permissions: Permission[] = [];
  private sessionId?: string;
  private requestId?: string;
  private metadata = new Map<string, unknown>();

  public setPrincipal(principal: SecurityPrincipal): this {
    this.principal = principal;
    // Auto populate roles if principal has them
    if (principal.roles) {
      this.roles = [...principal.roles];
    }
    return this;
  }

  public setRoles(roles: string[]): this {
    this.roles = [...roles];
    return this;
  }

  public addRole(role: string): this {
    if (!this.roles.includes(role)) {
      this.roles.push(role);
    }
    return this;
  }

  public setPermissions(permissions: Permission[]): this {
    this.permissions = [...permissions];
    return this;
  }

  public addPermission(permission: Permission): this {
    this.permissions.push(permission);
    return this;
  }

  public setSessionId(sessionId: string): this {
    this.sessionId = sessionId;
    return this;
  }

  public setRequestId(requestId: string): this {
    this.requestId = requestId;
    return this;
  }

  public setMetadata(metadata: Map<string, unknown>): this {
    this.metadata = new Map(metadata);
    return this;
  }

  public addMetadata(key: string, value: unknown): this {
    this.metadata.set(key, value);
    return this;
  }

  /**
   * Helper to recursively freeze objects ensuring full immutability.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private deepFreeze<T>(obj: T): T {
    if (obj === null || obj === undefined || typeof obj !== 'object') {
      return obj;
    }
    
    const props = Object.getOwnPropertyNames(obj);
    for (const prop of props) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const val = (obj as any)[prop];
      if (val && typeof val === 'object' && !Object.isFrozen(val)) {
        this.deepFreeze(val);
      }
    }
    return Object.freeze(obj);
  }

  public build(): SecurityContext {
    const context: SecurityContext = {
      principal: this.principal ? this.deepFreeze({ ...this.principal }) : undefined,
      roles: Object.freeze([...this.roles]),
      permissions: Object.freeze(this.permissions.map(p => this.deepFreeze({ ...p }))),
      sessionId: this.sessionId,
      requestId: this.requestId,
      metadata: Object.freeze(new Map(this.metadata)),
      timestamp: Date.now(),
    };

    return Object.freeze(context);
  }
}
