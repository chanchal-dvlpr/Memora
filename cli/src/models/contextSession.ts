export type SessionState = 'IDLE' | 'GENERATING' | 'COMPLETED' | 'FAILED';

export interface ContextSession {
  readonly sessionId: string;
  readonly projectId: string;
  readonly state: SessionState;
  readonly progress: number;
  readonly isCancelled: boolean;
  readonly streamingPlaceholder?: unknown;
  readonly resumablePlaceholder?: unknown;
}
