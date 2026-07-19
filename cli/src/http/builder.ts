/**
 * Immutable, fluent builder pattern for constructing structured HTTP requests.
 */
export class RequestBuilder {
  private constructor(
    private readonly method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    private readonly path: string,
    private readonly queryParams: Record<string, string> = {},
    private readonly headers: Record<string, string> = {},
    private readonly body?: unknown,
    private readonly timeoutMs?: number,
  ) {}

  /**
   * Instantiates a GET request builder.
   */
  public static get(path: string): RequestBuilder {
    return new RequestBuilder('GET', path);
  }

  /**
   * Instantiates a POST request builder.
   */
  public static post(path: string, body?: unknown): RequestBuilder {
    return new RequestBuilder('POST', path, {}, {}, body);
  }

  /**
   * Instantiates a PUT request builder.
   */
  public static put(path: string, body?: unknown): RequestBuilder {
    return new RequestBuilder('PUT', path, {}, {}, body);
  }

  /**
   * Instantiates a PATCH request builder.
   */
  public static patch(path: string, body?: unknown): RequestBuilder {
    return new RequestBuilder('PATCH', path, {}, {}, body);
  }

  /**
   * Instantiates a DELETE request builder.
   */
  public static delete(path: string): RequestBuilder {
    return new RequestBuilder('DELETE', path);
  }

  /**
   * Clones builder and appends query parameters.
   */
  public query(params: Record<string, string>): RequestBuilder {
    return new RequestBuilder(
      this.method,
      this.path,
      { ...this.queryParams, ...params },
      this.headers,
      this.body,
      this.timeoutMs,
    );
  }

  /**
   * Clones builder and appends custom header key-value.
   */
  public header(key: string, value: string): RequestBuilder {
    return new RequestBuilder(
      this.method,
      this.path,
      this.queryParams,
      { ...this.headers, [key]: value },
      this.body,
      this.timeoutMs,
    );
  }

  /**
   * Clones builder and sets custom timeout MS limits.
   */
  public timeout(ms: number): RequestBuilder {
    return new RequestBuilder(
      this.method,
      this.path,
      this.queryParams,
      this.headers,
      this.body,
      ms,
    );
  }

  public getMethod(): 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' {
    return this.method;
  }

  public getPath(): string {
    return this.path;
  }

  public getQueryParams(): Record<string, string> {
    return this.queryParams;
  }

  public getHeaders(): Record<string, string> {
    return this.headers;
  }

  public getBody(): unknown {
    return this.body;
  }

  public getTimeoutMs(): number | undefined {
    return this.timeoutMs;
  }
}
export default RequestBuilder;
