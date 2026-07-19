import { ApiClient } from './client';
import { RequestBuilder } from '../http/builder';
import { ExecutionContext } from '../models/context';

export interface CreateProjectRequest {
  readonly name: string;
  readonly rootPath: string;
}

export interface ProjectResponse {
  readonly id: string;
  readonly name: string;
  readonly rootPath: string;
}

/**
 * Resource-oriented client managing projects endpoint routes.
 */
export class ProjectApiClient extends ApiClient {
  public async createProject(
    req: CreateProjectRequest,
    ctx: ExecutionContext,
  ): Promise<ProjectResponse> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/projects`, req);
    const res = await this.clientService.execute<ProjectResponse>(builder, ctx);
    return res.data;
  }

  public async listProjects(
    ctx: ExecutionContext,
  ): Promise<ProjectResponse[]> {
    const builder = RequestBuilder.get(`/api/${this.apiVersion}/projects`);
    const res = await this.clientService.execute<ProjectResponse[]>(builder, ctx);
    return res.data;
  }

  public async refreshProject(
    id: string,
    ctx: ExecutionContext,
  ): Promise<{ projectId: string; filesScanned: number; snapshotGenerated: boolean }> {
    const builder = RequestBuilder.post(`/api/${this.apiVersion}/projects/${id}/refresh`).timeout(60000);
    const res = await this.clientService.execute<{ projectId: string; filesScanned: number; snapshotGenerated: boolean }>(
      builder,
      ctx,
    );
    return res.data;
  }

  public async removeProject(
    id: string,
    ctx: ExecutionContext,
  ): Promise<{ projectId: string }> {
    const builder = RequestBuilder.delete(`/api/${this.apiVersion}/projects/${id}`);
    const res = await this.clientService.execute<{ projectId: string }>(builder, ctx);
    return res.data || { projectId: id };
  }
}
export default ProjectApiClient;
