import {
  JsonRpcInvalidRequestError,
} from '../errors';
import { ClientCapabilities, ImplementationInfo } from '../types/protocol';

export enum ProtocolState {
  NOT_INITIALIZED = 'NOT_INITIALIZED',
  INITIALIZING = 'INITIALIZING',
  INITIALIZED = 'INITIALIZED',
  SHUTDOWN_REQUESTED = 'SHUTDOWN_REQUESTED',
  EXITED = 'EXITED',
}

export class ProtocolSession {
  public readonly sessionId: string;
  private state: ProtocolState = ProtocolState.NOT_INITIALIZED;
  private clientInfo?: ImplementationInfo;
  private protocolVersion?: string;
  private clientCapabilities?: ClientCapabilities;

  constructor() {
    this.sessionId = Math.random().toString(36).substring(2, 15);
  }

  /**
   * Returns the current lifecycle state of the protocol session.
   */
  public getState(): ProtocolState {
    return this.state;
  }

  /**
   * Transitions the session state according to the MCP specifications.
   * Throws a JsonRpcInvalidRequestError if the transition is invalid.
   */
  public transitionTo(newState: ProtocolState): void {
    const current = this.state;

    if (current === ProtocolState.EXITED) {
      throw new JsonRpcInvalidRequestError('Server has already exited.');
    }

    if (newState === ProtocolState.INITIALIZING) {
      if (current !== ProtocolState.NOT_INITIALIZED) {
        throw new JsonRpcInvalidRequestError('Server is already initialized or initializing.');
      }
    } else if (newState === ProtocolState.INITIALIZED) {
      if (current !== ProtocolState.INITIALIZING) {
        throw new JsonRpcInvalidRequestError('Must receive "initialize" request before "initialized" notification.');
      }
    } else if (newState === ProtocolState.SHUTDOWN_REQUESTED) {
      if (current !== ProtocolState.INITIALIZED) {
        throw new JsonRpcInvalidRequestError('Must be initialized before requesting shutdown.');
      }
    } else if (newState === ProtocolState.EXITED) {
      if (current !== ProtocolState.SHUTDOWN_REQUESTED) {
        throw new JsonRpcInvalidRequestError('Must request shutdown before exit notification.');
      }
    }

    this.state = newState;
  }

  /**
   * Sets the client session metadata.
   */
  public setClientInfo(info: ImplementationInfo, version: string, capabilities: ClientCapabilities): void {
    this.clientInfo = info;
    this.protocolVersion = version;
    this.clientCapabilities = capabilities;
  }

  public getClientInfo(): ImplementationInfo | undefined {
    return this.clientInfo;
  }

  public getProtocolVersion(): string | undefined {
    return this.protocolVersion;
  }

  public getClientCapabilities(): ClientCapabilities | undefined {
    return this.clientCapabilities;
  }
}
