import { ContextApiClient } from '../api/context';
import { httpClientService } from '../http/clientService';
import { ExecutionContext } from '../models/context';
import { ContextSession } from '../models/contextSession';
import {
  GeneratedContextResult,
  ContextDetailsResult,
  RefreshContextResult,
  DeleteContextResult,
} from '../models/contextResult';
import { validateProjectId, validateApiResponse } from '../validators/context';
import { commandEventPublisher } from '../events/commandEvents';
import * as crypto from 'crypto';

/**
 * Service layer coordinating context generation processes, format mapping, and session trackers.
 */
export class ContextApplicationService {
  private activeSessions = new Map<string, ContextSession>();

  constructor(private readonly contextApiClient: ContextApiClient) {}

  /**
   * Initializes a ContextSession tracker.
   */
  public createSession(projectId: string): ContextSession {
    const session: ContextSession = {
      sessionId: crypto.randomUUID(),
      projectId,
      state: 'IDLE',
      progress: 0,
      isCancelled: false,
    };
    this.activeSessions.set(session.sessionId, session);
    return session;
  }

  /**
   * Returns an active context session by ID.
   */
  public getSession(sessionId: string): ContextSession | undefined {
    return this.activeSessions.get(sessionId);
  }

  /**
   * Orchestrates context generation and tracks state progress transitions.
   */
  public async generateContext(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<{ result: GeneratedContextResult; session: ContextSession }> {
    try {
      validateProjectId(projectId);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'ContextValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const initialSession = this.createSession(projectId);
    const generatingSession: ContextSession = {
      ...initialSession,
      state: 'GENERATING',
      progress: 50,
    };
    this.activeSessions.set(generatingSession.sessionId, generatingSession);

    try {
      const res = await this.contextApiClient.generateContext(projectId, ctx);
      validateApiResponse(res);

      const completedSession: ContextSession = {
        ...generatingSession,
        state: 'COMPLETED',
        progress: 100,
      };
      this.activeSessions.set(completedSession.sessionId, completedSession);

      const result: GeneratedContextResult = {
        projectId: res.projectId,
        content: res.content,
        updatedAt: res.updatedAt,
      };

      commandEventPublisher.publish({
        type: 'ContextGenerated',
        timestamp: new Date(),
        payload: { projectId: result.projectId },
      });

      return { result, session: completedSession };
    } catch (err: unknown) {
      const failedSession: ContextSession = {
        ...generatingSession,
        state: 'FAILED',
      };
      this.activeSessions.set(failedSession.sessionId, failedSession);

      commandEventPublisher.publish({
        type: 'ContextGenerationFailed',
        timestamp: new Date(),
        payload: {
          projectId,
          error: err instanceof Error ? err.message : String(err),
        },
      });

      throw err;
    }
  }

  /**
   * Returns context information for a specific project.
   */
  public async getContext(projectId: string, ctx: ExecutionContext): Promise<ContextDetailsResult> {
    try {
      validateProjectId(projectId);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'ContextValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.contextApiClient.getContext({ projectId }, ctx);
    validateApiResponse(res);

    const result: ContextDetailsResult = {
      projectId: res.projectId,
      content: res.content,
      updatedAt: res.updatedAt,
    };

    commandEventPublisher.publish({
      type: 'ContextViewed',
      timestamp: new Date(),
      payload: { projectId: result.projectId },
    });

    return result;
  }

  /**
   * Triggers a context scan refresh.
   */
  public async refreshContext(projectId: string, ctx: ExecutionContext): Promise<RefreshContextResult> {
    try {
      validateProjectId(projectId);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'ContextValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.contextApiClient.refreshContext(projectId, ctx);
    validateApiResponse(res);

    const result: RefreshContextResult = {
      projectId: res.projectId,
      content: res.content,
      updatedAt: res.updatedAt,
    };

    commandEventPublisher.publish({
      type: 'ContextRefreshed',
      timestamp: new Date(),
      payload: { projectId: result.projectId },
    });

    return result;
  }

  /**
   * Deletes context registration.
   */
  public async deleteContext(projectId: string, ctx: ExecutionContext): Promise<DeleteContextResult> {
    try {
      validateProjectId(projectId);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'ContextValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.contextApiClient.deleteContext(projectId, ctx);

    const result: DeleteContextResult = {
      type: 'DeleteContextResult',
      projectId: res.projectId,
      success: res.deleted,
    };

    commandEventPublisher.publish({
      type: 'ContextDeleted',
      timestamp: new Date(),
      payload: { projectId: result.projectId },
    });

    return result;
  }
}

export const contextApiClient = new ContextApiClient(httpClientService);
export const contextApplicationService = new ContextApplicationService(contextApiClient);
export default contextApplicationService;
