import { PromptExecutionContext, PromptInvocationResult, PromptDefinition } from '../types/prompt';

export type PromptMiddlewareNext = () => Promise<PromptInvocationResult>;

export type PromptMiddleware = (
  context: PromptExecutionContext,
  prompt: PromptDefinition,
  next: PromptMiddlewareNext
) => Promise<PromptInvocationResult>;

export class PromptMiddlewarePipeline {
  private readonly middlewares: PromptMiddleware[] = [];

  /**
   * Registers a middleware hook in the prompt execution chain.
   */
  public use(middleware: PromptMiddleware): void {
    this.middlewares.push(middleware);
  }

  /**
   * Triggers sequential Koa/onion-style execution.
   */
  public async execute(
    context: PromptExecutionContext,
    prompt: PromptDefinition,
    finalHandler: () => Promise<PromptInvocationResult>
  ): Promise<PromptInvocationResult> {
    let index = -1;

    const dispatch = async (i: number): Promise<PromptInvocationResult> => {
      if (i <= index) {
        throw new Error('next() called multiple times');
      }
      index = i;
      const middleware = this.middlewares[i];
      if (middleware) {
        return middleware(context, prompt, () => dispatch(i + 1));
      }
      return finalHandler();
    };

    return dispatch(0);
  }
}
