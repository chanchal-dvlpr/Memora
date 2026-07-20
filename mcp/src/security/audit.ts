import { AuditEntry, AuditLevel } from '../types/security';

export interface AuditLogger {
  log(level: AuditLevel, category: string, action: string, details: string, outcome: 'success' | 'failure', actor?: string): void;
  getEntries(filter?: {
    level?: AuditLevel;
    category?: string;
    actor?: string;
    outcome?: 'success' | 'failure';
  }): AuditEntry[];
  clear(): void;
}

export class InMemoryAuditLogger implements AuditLogger {
  private entries: AuditEntry[] = [];

  public log(
    level: AuditLevel,
    category: string,
    action: string,
    details: string,
    outcome: 'success' | 'failure',
    actor?: string
  ): void {
    const entry: AuditEntry = {
      timestamp: Date.now(),
      level,
      category,
      action,
      actor,
      details,
      outcome,
    };
    this.entries.push(Object.freeze(entry));
  }

  public getEntries(filter?: {
    level?: AuditLevel;
    category?: string;
    actor?: string;
    outcome?: 'success' | 'failure';
  }): AuditEntry[] {
    let result = [...this.entries];

    if (filter) {
      if (filter.level) {
        result = result.filter((e) => e.level === filter.level);
      }
      if (filter.category) {
        result = result.filter((e) => e.category === filter.category);
      }
      if (filter.actor) {
        result = result.filter((e) => e.actor === filter.actor);
      }
      if (filter.outcome) {
        result = result.filter((e) => e.outcome === filter.outcome);
      }
    }

    return Object.freeze(result) as unknown as AuditEntry[];
  }

  public clear(): void {
    this.entries = [];
  }
}
