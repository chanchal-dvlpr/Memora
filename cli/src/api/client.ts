import { HttpClientService } from '../http/clientService';

/**
 * Base abstract class encapsulating common API version specs and client service delegations.
 */
export abstract class ApiClient {
  constructor(
    protected readonly clientService: HttpClientService,
    protected readonly apiVersion: string = 'v1',
  ) {}
}
export default ApiClient;
