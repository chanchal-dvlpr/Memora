import { JsonRpcMessage } from '../types/jsonrpc';
import { JsonRpcParseError } from '../errors';

export class MessageParser {
  /**
   * Parses a raw JSON string into a JsonRpcMessage object.
   * Throws a JsonRpcParseError if the JSON structure is malformed.
   */
  public static parse(payload: string): JsonRpcMessage {
    if (payload === undefined || payload === null || payload.trim() === '') {
      throw new JsonRpcParseError('Empty message payload');
    }
    try {
      return JSON.parse(payload) as JsonRpcMessage;
    } catch (err) {
      throw new JsonRpcParseError(err instanceof Error ? err.message : String(err));
    }
  }
}
