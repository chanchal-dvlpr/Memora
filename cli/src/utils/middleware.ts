import { ExecutionContext } from '../models/context';

/**
 * Formal contract interface for Memora CLI middlewares.
 */
export interface CliMiddleware {
  readonly name: string;
  execute(ctx: ExecutionContext, next: () => Promise<void>): Promise<void> | void;
}

/**
 * Sequential asynchronous execution pipeline framework enforcing the CliMiddleware contract.
 */
export class Pipeline {
  private middlewares: CliMiddleware[] = [];

  public use(middleware: CliMiddleware): this {
    this.middlewares.push(middleware);
    return this;
  }

  public async run(ctx: ExecutionContext, targetAction: () => Promise<void> | void): Promise<void> {
    let index = -1;

    const dispatch = async (i: number): Promise<void> => {
      if (i <= index) {
        throw new Error('next() called multiple times');
      }
      index = i;

      const mw = this.middlewares[i];
      if (i === this.middlewares.length) {
        await targetAction();
        return;
      }

      if (mw) {
        await mw.execute(ctx, () => dispatch(i + 1));
      }
    };

    await dispatch(0);
  }
}
