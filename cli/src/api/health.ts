import { ApiClient } from './client';
import { RequestBuilder } from '../http/builder';
import { ExecutionContext } from '../models/context';

export interface HealthResponse {
  readonly status: 'UP' | 'DOWN';
  readonly version?: string;
}

/**
 * Resource-oriented client handling daemon health endpoint routes.
 */
export class HealthApiClient extends ApiClient {
  public async checkHealth(
    ctx: ExecutionContext,
  ): Promise<HealthResponse> {
    const builder = RequestBuilder.get('/health');
    const res = await this.clientService.execute<HealthResponse>(builder, ctx);
    return res.data;
  }
}
export default HealthApiClient;
