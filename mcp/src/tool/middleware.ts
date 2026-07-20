import { ToolExecutionContext, ToolExecutionResult, ToolDefinition } from '../types/tool';

export type ToolMiddlewareNext = () => Promise<ToolExecutionResult>;

export type ToolMiddleware = (
  context: ToolExecutionContext,
  tool: ToolDefinition,
  next: ToolMiddlewareNext
) => Promise<ToolExecutionResult>;

export class ToolMiddlewarePipeline {
  private readonly middlewares: ToolMiddleware[] = [];

  /**
   * Registers a middleware hook in the execution chain.
   */
  public use(middleware: ToolMiddleware): void {
    this.middlewares.push(middleware);
  }

  /**
   * Triggers Koa/Onion-style sequential execution of all registered middlewares.
   */
  public async execute(
    context: ToolExecutionContext,
    tool: ToolDefinition,
    finalHandler: () => Promise<ToolExecutionResult>
  ): Promise<ToolExecutionResult> {
    let index = -1;

    const dispatch = async (i: number): Promise<ToolExecutionResult> => {
      if (i <= index) {
        throw new Error('next() called multiple times');
      }
      index = i;
      const middleware = this.middlewares[i];
      if (middleware) {
        return middleware(context, tool, () => dispatch(i + 1));
      }
      return finalHandler();
    };

    return dispatch(0);
  }
}
