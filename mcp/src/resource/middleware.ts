import { ResourceExecutionContext, ResourceReadResult, ResourceDefinition } from '../types/resource';

export type ResourceMiddlewareNext = () => Promise<ResourceReadResult>;

export type ResourceMiddleware = (
  context: ResourceExecutionContext,
  resource: ResourceDefinition,
  next: ResourceMiddlewareNext
) => Promise<ResourceReadResult>;

export class ResourceMiddlewarePipeline {
  private readonly middlewares: ResourceMiddleware[] = [];

  /**
   * Registers a middleware hook in the resource execution chain.
   */
  public use(middleware: ResourceMiddleware): void {
    this.middlewares.push(middleware);
  }

  /**
   * Triggers Koa/Onion-style sequential execution of all registered resource middlewares.
   */
  public async execute(
    context: ResourceExecutionContext,
    resource: ResourceDefinition,
    finalHandler: () => Promise<ResourceReadResult>
  ): Promise<ResourceReadResult> {
    let index = -1;

    const dispatch = async (i: number): Promise<ResourceReadResult> => {
      if (i <= index) {
        throw new Error('next() called multiple times');
      }
      index = i;
      const middleware = this.middlewares[i];
      if (middleware) {
        return middleware(context, resource, () => dispatch(i + 1));
      }
      return finalHandler();
    };

    return dispatch(0);
  }
}
