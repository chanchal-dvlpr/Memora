import { ApiClient } from './client';
import { RequestBuilder } from '../http/builder';
import { ExecutionContext } from '../models/context';

export interface GetContextRequest {
  readonly projectId: string;
}

export interface UpdateContextRequest {
  readonly projectId: string;
  readonly content: string;
}

export interface ContextResponse {
  readonly projectId: string;
  readonly content: string;
  readonly updatedAt: string;
}

/**
 * Resource-oriented client managing context endpoint routes.
 */
export class ContextApiClient extends ApiClient {
  public async getContext(
    req: GetContextRequest,
    ctx: ExecutionContext,
  ): Promise<ContextResponse> {
    const builder = RequestBuilder.get(`/api/${this.apiVersion}/context/${req.projectId}`);
    const res = await this.clientService.execute<ContextResponse>(builder, ctx);
    return res.data;
  }

  public async updateContext(
    req: UpdateContextRequest,
    ctx: ExecutionContext,
  ): Promise<ContextResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/context/${req.projectId}`, {
      content: req.content,
    });
    const res = await this.clientService.execute<ContextResponse>(builder, ctx);
    return res.data;
  }

  public async generateContext(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<ContextResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/context/${projectId}/generate`).timeout(60000);
    const res = await this.clientService.execute<ContextResponse>(builder, ctx);
    return res.data;
  }

  public async refreshContext(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<ContextResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/context/${projectId}/refresh`).timeout(60000);
    const res = await this.clientService.execute<ContextResponse>(builder, ctx);
    return res.data;
  }

  public async deleteContext(
    projectId: string,
    ctx: ExecutionContext,
  ): Promise<{ projectId: string; deleted: boolean }> {
    const builder = RequestBuilder.delete(`/api/${this.apiVersion}/context/${projectId}`);
    const res = await this.clientService.execute<{ projectId: string; deleted: boolean }>(builder, ctx);
    return res.data;
  }
}
export default ContextApiClient;
