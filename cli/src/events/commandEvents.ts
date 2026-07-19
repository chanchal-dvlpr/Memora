export interface CommandEvent<T = unknown> {
  readonly type: string;
  readonly timestamp: Date;
  readonly payload: T;
}

export interface ProjectRegisteredPayload {
  readonly projectId: string;
  readonly name: string;
  readonly rootPath: string;
}

export interface ProjectRefreshedPayload {
  readonly projectId: string;
  readonly filesScanned: number;
  readonly snapshotGenerated: boolean;
}

export interface ProjectRemovedPayload {
  readonly projectId: string;
}

export interface ProjectListedPayload {
  readonly projectCount: number;
}

export interface ProjectViewedPayload {
  readonly projectId: string;
}

export interface ProjectCommandFailedPayload {
  readonly commandName: string;
  readonly error: string;
}

export interface ContextGeneratedPayload {
  readonly projectId: string;
}

export interface ContextRefreshedPayload {
  readonly projectId: string;
}

export interface ContextExportedPayload {
  readonly projectId: string;
  readonly format: string;
}

export interface ContextDeletedPayload {
  readonly projectId: string;
}

export interface ContextViewedPayload {
  readonly projectId: string;
}

export interface ContextGenerationFailedPayload {
  readonly projectId: string;
  readonly error: string;
}

export interface ContextValidationFailedPayload {
  readonly error: string;
}

export interface KnowledgeSearchedPayload {
  readonly projectId: string;
  readonly query: string;
}

export interface KnowledgeViewedPayload {
  readonly id: string;
}

export interface KnowledgeExplainedPayload {
  readonly id: string;
}

export interface KnowledgeRefreshedPayload {
  readonly projectId: string;
}

export interface KnowledgeDeletedPayload {
  readonly id: string;
}

export interface KnowledgeSearchFailedPayload {
  readonly query: string;
  readonly error: string;
}

export interface KnowledgeValidationFailedPayload {
  readonly error: string;
}

export interface DiagnosticsStartedPayload {
  readonly timestamp: Date;
}

export interface HealthCheckedPayload {
  readonly allPassed: boolean;
}

export interface ConfigurationCheckedPayload {
  readonly allPassed: boolean;
}

export interface ConnectivityCheckedPayload {
  readonly allPassed: boolean;
}

export interface EnvironmentCheckedPayload {
  readonly allPassed: boolean;
}

export interface DiagnosticsCompletedPayload {
  readonly allPassed: boolean;
}

export interface DiagnosticsFailedPayload {
  readonly error: string;
}

export interface DiagnosticsValidationFailedPayload {
  readonly error: string;
}

export interface RenderingStartedPayload {
  readonly mode: string;
  readonly destination: string;
}

export interface RenderingCompletedPayload {
  readonly mode: string;
  readonly destination: string;
  readonly outputLength: number;
}

export interface RenderingFailedPayload {
  readonly mode: string;
  readonly destination: string;
  readonly error: string;
}

export interface ExportStartedPayload {
  readonly destination: string;
}

export interface ExportCompletedPayload {
  readonly destination: string;
  readonly bytesWritten: number;
}

export interface ExportFailedPayload {
  readonly destination: string;
  readonly error: string;
}

export interface BuildStartedPayload {
  readonly version: string;
}

export interface BuildCompletedPayload {
  readonly version: string;
  readonly outputPath: string;
}

export interface BuildFailedPayload {
  readonly version: string;
  readonly error: string;
}

export interface PackageCreatedPayload {
  readonly tarballPath: string;
  readonly sizeBytes: number;
}

export interface PackageVerifiedPayload {
  readonly tarballPath: string;
  readonly checksumMatches: boolean;
}

export interface ReleaseValidatedPayload {
  readonly version: string;
  readonly success: boolean;
}

export interface CommandEventSubscriber {
  onEvent(event: CommandEvent<unknown>): void | Promise<void>;
}

/**
 * Lightweight, in-process Event Publisher coordinating Command lifecycle events.
 */
export class CommandEventPublisher {
  private subscribers: CommandEventSubscriber[] = [];

  /**
   * Subscribes a listener to events and returns an unsubscribe callback.
   */
  public subscribe(subscriber: CommandEventSubscriber): () => void {
    this.subscribers.push(subscriber);
    return () => {
      this.subscribers = this.subscribers.filter((s) => s !== subscriber);
    };
  }

  /**
   * Publishes an event to all subscribers asynchronously.
   */
  public publish(event: CommandEvent<unknown>): void {
    for (const sub of this.subscribers) {
      Promise.resolve(sub.onEvent(event)).catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : String(err);
        console.error(`[EventPublisher] Subscriber threw an error: ${msg}`);
      });
    }
  }
}

export const commandEventPublisher = new CommandEventPublisher();
export default commandEventPublisher;
