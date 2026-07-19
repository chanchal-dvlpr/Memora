import { ApiClient } from './client';
import { RequestBuilder } from '../http/builder';
import { ExecutionContext } from '../models/context';

export interface QueryKnowledgeRequest {
  readonly projectId: string;
  readonly query: string;
  readonly limit?: number;
}

export interface KnowledgeDocument {
  readonly id: string;
  readonly title: string;
  readonly content: string;
  readonly score?: number;
}

export interface QueryKnowledgeResponse {
  readonly documents: KnowledgeDocument[];
}

/**
 * Resource-oriented client managing knowledge query endpoint routes.
 */
export class KnowledgeApiClient extends ApiClient {
  public async queryKnowledge(
    req: QueryKnowledgeRequest,
    ctx: ExecutionContext,
  ): Promise<QueryKnowledgeResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/knowledge/query`, req);
    const res = await this.clientService.execute<QueryKnowledgeResponse>(builder, ctx);
    return res.data;
  }

  public async getKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<KnowledgeDocument> {
    const builder = RequestBuilder.get(`/api/${this.apiVersion}/knowledge/${id}`);
    const res = await this.clientService.execute<KnowledgeDocument>(builder, ctx);
    return res.data;
  }

  public async explainKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<{ id: string; explanation: string }> {
    const builder = RequestBuilder.get(`/api/${this.apiVersion}/knowledge/${id}/explain`);
    const res = await this.clientService.execute<{ id: string; explanation: string }>(builder, ctx);
    return res.data;
  }

  public async refreshKnowledge(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<QueryKnowledgeResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/knowledge/refresh`, { projectId });
    const res = await this.clientService.execute<QueryKnowledgeResponse>(builder, ctx);
    return res.data;
  }

  public async deleteKnowledge(
    id: string,
    ctx: ExecutionContext,
  ): Promise<{ id: string; deleted: boolean }> {
    const builder = RequestBuilder.delete(`/api/${this.apiVersion}/knowledge/${id}`);
    const res = await this.clientService.execute<{ id: string; deleted: boolean }>(builder, ctx);
    return res.data;
  }
}
export default KnowledgeApiClient;
