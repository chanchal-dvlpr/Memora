import { HttpResponse } from './client';
import { mapHttpError } from './mapper';

/**
 * Interface representing standard fetch responses to bypass TS compiler DOM dependencies.
 */
export interface FetchResponse {
  readonly status: number;
  readonly headers: { forEach(callback: (value: string, key: string) => void): void };
  text(): Promise<string>;
}

/**
 * Evaluates fetch responses, parsing JSON bodies and resolving failures.
 */
export class ResponseHandler {
  public static async handle<T>(
    response: FetchResponse,
    method: string,
    url: string,
  ): Promise<HttpResponse<T>> {
    const status = response.status;
    const headers: Record<string, string> = {};

    response.headers.forEach((value, key) => {
      headers[key.toLowerCase()] = value;
    });

    let data: unknown = null;
    const contentType = headers['content-type'] || '';
    const text = await response.text();

    if (text.length > 0) {
      if (contentType.includes('application/json')) {
        try {
          data = JSON.parse(text) as unknown;
        } catch (err) {
          throw mapHttpError(500, method, url, `Malformed JSON: ${text}`);
        }
      } else {
        data = text;
      }
    }

    if (status >= 200 && status < 300) {
      return { status, headers, data: data as T };
    }

    throw mapHttpError(status, method, url, data);
  }
}
export default ResponseHandler;
