export interface DiagnosticCheck {
  readonly id: string;
  readonly category: string;
  readonly description: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
  readonly status: 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED';
  readonly result?: string;
  readonly error?: string;
  readonly durationMs?: number;
  readonly asyncPlaceholder?: unknown;
  readonly parallelPlaceholder?: unknown;
}
