import {
  JsonRpcId,
  JsonRpcMessage,
  JsonRpcSuccessResponse,
  JsonRpcErrorResponse,
} from '../types/jsonrpc';

export class MessageSerializer {
  /**
   * Serializes a JsonRpcMessage object into a JSON string payload.
   */
  public static serialize(message: JsonRpcMessage): string {
    return JSON.stringify(message);
  }

  /**
   * Helper to format a successful JSON-RPC 2.0 response.
   */
  public static success(id: JsonRpcId, result: unknown): JsonRpcSuccessResponse {
    return {
      jsonrpc: '2.0',
      id,
      result,
    };
  }

  /**
   * Helper to format an error JSON-RPC 2.0 response.
   */
  public static error(id: JsonRpcId | null, code: number, message: string, data?: unknown): JsonRpcErrorResponse {
    const errorObj: { code: number; message: string; data?: unknown } = {
      code,
      message,
    };
    if (data !== undefined) {
      errorObj.data = data;
    }
    return {
      jsonrpc: '2.0',
      id,
      error: errorObj,
    };
  }
}
