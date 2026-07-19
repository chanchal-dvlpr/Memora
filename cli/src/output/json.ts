import { Renderer } from './renderer';

/**
 * Renderer producing sorted, stable JSON serialized outputs.
 */
export class JsonRenderer implements Renderer<unknown> {
  render(model: unknown): string {
    let target = model;
    if (model && typeof model === 'object') {
      if ('project' in model && 'success' in model) {
        target = (model as Record<string, unknown>).project;
      } else if ('projects' in model) {
        target = (model as Record<string, unknown>).projects;
      }
    }
    const sorted = this.sortKeys(target);
    return JSON.stringify(sorted, null, 2);
  }

  private sortKeys(obj: unknown): unknown {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }
    if (Array.isArray(obj)) {
      return obj.map((item) => this.sortKeys(item));
    }
    const sortedObj: Record<string, unknown> = {};
    const keys = Object.keys(obj as Record<string, unknown>).sort();
    for (const key of keys) {
      sortedObj[key] = this.sortKeys((obj as Record<string, unknown>)[key]);
    }
    return sortedObj;
  }
}

export const jsonRenderer = new JsonRenderer();
