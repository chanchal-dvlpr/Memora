export interface SecurityPrincipal {
  readonly id: string;
  readonly name: string;
  readonly roles: readonly string[];
  readonly metadata: ReadonlyMap<string, unknown>;
}

export interface AuthenticationResult {
  readonly success: boolean;
  readonly principal?: SecurityPrincipal;
  readonly error?: string;
}

export interface AuthorizationResult {
  readonly allowed: boolean;
  readonly reason?: string;
}

export enum SecurityAction {
  READ = 'read',
  WRITE = 'write',
  EXECUTE = 'execute',
}

export interface PermissionScope {
  readonly target: string; // glob or specific resource/tool/prompt name/URI
  readonly actions: readonly SecurityAction[];
}

export interface Permission {
  readonly name: string;
  readonly scope?: PermissionScope;
}

export interface PermissionPolicy {
  readonly name: string;
  readonly description?: string;
  readonly evaluate: (context: SecurityContext, action: SecurityAction, target: string) => Promise<boolean>;
}

export interface SecurityContext {
  readonly principal?: SecurityPrincipal;
  readonly roles: readonly string[];
  readonly permissions: readonly Permission[];
  readonly sessionId?: string;
  readonly requestId?: string;
  readonly metadata: ReadonlyMap<string, unknown>;
  readonly timestamp: number;
}

export enum AuditLevel {
  INFO = 'info',
  WARNING = 'warning',
  ALERT = 'alert',
}

export interface AuditEntry {
  readonly timestamp: number;
  readonly level: AuditLevel;
  readonly category: string; // e.g. authentication, authorization, execution
  readonly action: string;
  readonly actor?: string; // principal name/id
  readonly details: string;
  readonly outcome: 'success' | 'failure';
}

export interface SecurityMetadata {
  readonly requiredRole?: string;
  readonly requiredPermissions?: readonly string[];
  readonly [key: string]: unknown;
}

export interface SecurityDecision {
  readonly allowed: boolean;
  readonly decisionTime: number;
  readonly policyName?: string;
  readonly explanation?: string;
}
