import { JsonRpcMessage } from '../types/jsonrpc';
import { JsonRpcInvalidRequestError } from '../errors';

export class MessageValidator {
  /**
   * Validates if a parsed object conforms to the JSON-RPC 2.0 protocol specifications.
   * Throws a JsonRpcInvalidRequestError if validation fails.
   */
  public static validate(message: unknown): asserts message is JsonRpcMessage {
    if (typeof message !== 'object' || message === null || Array.isArray(message)) {
      throw new JsonRpcInvalidRequestError('Message must be a single JSON-RPC 2.0 object');
    }

    const msg = message as Record<string, unknown>;

    // 1. Validate version string
    if (msg.jsonrpc !== '2.0') {
      throw new JsonRpcInvalidRequestError('Invalid or missing "jsonrpc" version string, must be exactly "2.0"');
    }

    // 2. Classify: Request, Notification, or Response
    const hasId = 'id' in msg;
    const hasMethod = 'method' in msg;
    const hasResult = 'result' in msg;
    const hasError = 'error' in msg;

    if (hasMethod) {
      if (typeof msg.method !== 'string') {
        throw new JsonRpcInvalidRequestError('The "method" field must be a string');
      }

      if (hasId) {
        // Request validation
        if (typeof msg.id !== 'string' && typeof msg.id !== 'number') {
          throw new JsonRpcInvalidRequestError('Request ID must be a string or number');
        }
      } else {
        // Notification validation
        // Notifications are allowed to not have an ID field
      }
    } else if (hasResult || hasError) {
      // Response validation
      if (hasId && msg.id !== null && typeof msg.id !== 'string' && typeof msg.id !== 'number') {
        throw new JsonRpcInvalidRequestError('Response ID must be a string, number, or null');
      }

      if (hasError) {
        const err = msg.error;
        if (typeof err !== 'object' || err === null || Array.isArray(err)) {
          throw new JsonRpcInvalidRequestError('The "error" field must be an object');
        }
        const errObj = err as Record<string, unknown>;
        if (typeof errObj.code !== 'number') {
          throw new JsonRpcInvalidRequestError('The "error.code" field must be a number');
        }
        if (typeof errObj.message !== 'string') {
          throw new JsonRpcInvalidRequestError('The "error.message" field must be a string');
        }
      }
    } else {
      throw new JsonRpcInvalidRequestError('Message must be a Request, Notification, or Response');
    }
  }
}
