import { JsonRpcMethodNotFoundError } from '../errors';

export type MethodHandler = (params?: unknown) => Promise<unknown>;

export class MessageRouter {
  private readonly routes = new Map<string, MethodHandler>();

  /**
   * Registers a handler for a given JSON-RPC method name.
   */
  public register(method: string, handler: MethodHandler): void {
    if (this.routes.has(method)) {
      throw new Error(`Route for method "${method}" is already registered.`);
    }
    this.routes.set(method, handler);
  }

  /**
   * Routes a method call to its registered handler.
   * Throws a JsonRpcMethodNotFoundError if the method does not exist.
   */
  public async route(method: string, params?: unknown): Promise<unknown> {
    const handler = this.routes.get(method);
    if (!handler) {
      throw new JsonRpcMethodNotFoundError(method);
    }
    return handler(params);
  }

  /**
   * Checks if a route exists.
   */
  public hasRoute(method: string): boolean {
    return this.routes.has(method);
  }

  /**
   * Clears all routes.
   */
  public clear(): void {
    this.routes.clear();
  }
}
