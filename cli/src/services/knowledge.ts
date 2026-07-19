import { KnowledgeApiClient } from '../api/knowledge';
import { httpClientService } from '../http/clientService';
import { ExecutionContext } from '../models/context';
import { KnowledgeQuery } from '../models/knowledgeQuery';
import {
  KnowledgeSearchResult,
  KnowledgeDetailsResult,
  KnowledgeExplanationResult,
  KnowledgeRefreshResult,
  KnowledgeDeleteResult,
} from '../models/knowledgeResult';
import {
  validateKnowledgeId,
  validateProjectId,
  validateSearchQuery,
  validateApiResponse,
} from '../validators/knowledge';
import { commandEventPublisher } from '../events/commandEvents';

/**
 * Service layer coordinating knowledge search queries, details mapping, and explanation flows.
 */
export class KnowledgeApplicationService {
  constructor(private readonly apiClient: KnowledgeApiClient) {}

  /**
   * Constructs an immutable KnowledgeQuery search request.
   */
  public createQuery(
    queryText: string,
    projectId: string,
    limit?: number,
  ): KnowledgeQuery {
    return Object.freeze({
      queryText,
      projectId,
      limit,
    });
  }

  /**
   * Coordinates knowledge document search.
   */
  public async searchKnowledge(
    query: KnowledgeQuery,
    ctx: ExecutionContext,
  ): Promise<KnowledgeSearchResult> {
    try {
      validateProjectId(query.projectId);
      validateSearchQuery(query.queryText);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    try {
      const res = await this.apiClient.queryKnowledge({
        projectId: query.projectId,
        query: query.queryText,
        limit: query.limit,
      }, ctx);
      validateApiResponse(res);

      const result: KnowledgeSearchResult = {
        type: 'KnowledgeSearchResult',
        projectId: query.projectId,
        documents: res.documents,
      };

      commandEventPublisher.publish({
        type: 'KnowledgeSearched',
        timestamp: new Date(),
        payload: { projectId: query.projectId, query: query.queryText },
      });

      return result;
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeSearchFailed',
        timestamp: new Date(),
        payload: {
          query: query.queryText,
          error: err instanceof Error ? err.message : String(err),
        },
      });
      throw err;
    }
  }

  /**
   * Retrieves detailed information of a specific knowledge item.
   */
  public async showKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<KnowledgeDetailsResult> {
    try {
      validateKnowledgeId(id);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.apiClient.getKnowledge(id, ctx);
    validateApiResponse(res);

    const result: KnowledgeDetailsResult = {
      id: res.id,
      title: res.title,
      content: res.content,
    };

    commandEventPublisher.publish({
      type: 'KnowledgeViewed',
      timestamp: new Date(),
      payload: { id: result.id },
    });

    return result;
  }

  /**
   * Fetches the structured explanation of a knowledge item.
   */
  public async explainKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<KnowledgeExplanationResult> {
    try {
      validateKnowledgeId(id);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.apiClient.explainKnowledge(id, ctx);
    validateApiResponse(res);

    const result: KnowledgeExplanationResult = {
      id: res.id,
      explanation: res.explanation,
    };

    commandEventPublisher.publish({
      type: 'KnowledgeExplained',
      timestamp: new Date(),
      payload: { id: result.id },
    });

    return result;
  }

  /**
   * Triggers a index refresh of project knowledge base.
   */
  public async refreshKnowledge(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<KnowledgeRefreshResult> {
    try {
      validateProjectId(projectId);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.apiClient.refreshKnowledge(projectId, ctx);
    validateApiResponse(res);

    const result: KnowledgeRefreshResult = {
      type: 'KnowledgeRefreshResult',
      projectId,
      documents: res.documents,
    };

    commandEventPublisher.publish({
      type: 'KnowledgeRefreshed',
      timestamp: new Date(),
      payload: { projectId },
    });

    return result;
  }

  /**
   * Deletes a knowledge base item.
   */
  public async deleteKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<KnowledgeDeleteResult> {
    try {
      validateKnowledgeId(id);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'KnowledgeValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const res = await this.apiClient.deleteKnowledge(id, ctx);

    const result: KnowledgeDeleteResult = {
      id: res.id,
      success: res.deleted,
    };

    commandEventPublisher.publish({
      type: 'KnowledgeDeleted',
      timestamp: new Date(),
      payload: { id: result.id },
    });

    return result;
  }
}

export const knowledgeApiClient = new KnowledgeApiClient(httpClientService);
export const knowledgeApplicationService = new KnowledgeApplicationService(knowledgeApiClient);
export default knowledgeApplicationService;
