import { SessionContext, Session } from '../types/session';
import { SecurityContext } from '../types/security';

export class SessionContextBuilder {
  private requestId?: string;
  private correlationId?: string;
  private requestMetadata: Record<string, unknown> = {};
  private protocolMetadata: Record<string, unknown> = {};
  private conversationMetadata: Record<string, unknown> = {};
  private clientMetadata: Record<string, unknown> = {};
  private clientInformation: Record<string, unknown> = {};
  private runtimeMetadata: Record<string, unknown> = {};
  private executionMetadata: Record<string, unknown> = {};
  private securityContext?: SecurityContext;

  public setRequestId(requestId: string): this {
    this.requestId = requestId;
    return this;
  }

  public setCorrelationId(correlationId: string): this {
    this.correlationId = correlationId;
    return this;
  }

  public setRequestMetadata(metadata: Record<string, unknown>): this {
    this.requestMetadata = { ...metadata };
    return this;
  }

  public addRequestMetadata(key: string, value: unknown): this {
    this.requestMetadata[key] = value;
    return this;
  }

  public setProtocolMetadata(metadata: Record<string, unknown>): this {
    this.protocolMetadata = { ...metadata };
    return this;
  }

  public addProtocolMetadata(key: string, value: unknown): this {
    this.protocolMetadata[key] = value;
    return this;
  }

  public setConversationMetadata(metadata: Record<string, unknown>): this {
    this.conversationMetadata = { ...metadata };
    return this;
  }

  public addConversationMetadata(key: string, value: unknown): this {
    this.conversationMetadata[key] = value;
    return this;
  }

  public setClientMetadata(metadata: Record<string, unknown>): this {
    this.clientMetadata = { ...metadata };
    return this;
  }

  public addClientMetadata(key: string, value: unknown): this {
    this.clientMetadata[key] = value;
    return this;
  }

  public setClientInformation(info: Record<string, unknown>): this {
    this.clientInformation = { ...info };
    return this;
  }

  public addClientInformation(key: string, value: unknown): this {
    this.clientInformation[key] = value;
    return this;
  }

  public setRuntimeMetadata(metadata: Record<string, unknown>): this {
    this.runtimeMetadata = { ...metadata };
    return this;
  }

  public addRuntimeMetadata(key: string, value: unknown): this {
    this.runtimeMetadata[key] = value;
    return this;
  }

  public setExecutionMetadata(metadata: Record<string, unknown>): this {
    this.executionMetadata = { ...metadata };
    return this;
  }

  public addExecutionMetadata(key: string, value: unknown): this {
    this.executionMetadata[key] = value;
    return this;
  }

  public setSecurityContext(securityContext: SecurityContext): this {
    this.securityContext = securityContext;
    return this;
  }

  public build(): SessionContext {
    const context: SessionContext = {
      requestId: this.requestId,
      correlationId: this.correlationId,
      requestMetadata: Object.freeze({ ...this.requestMetadata }),
      protocolMetadata: Object.freeze({ ...this.protocolMetadata }),
      conversationMetadata: Object.freeze({ ...this.conversationMetadata }),
      clientMetadata: Object.freeze({ ...this.clientMetadata }),
      clientInformation: Object.freeze({ ...this.clientInformation }),
      runtimeMetadata: Object.freeze({ ...this.runtimeMetadata }),
      executionMetadata: Object.freeze({ ...this.executionMetadata }),
      securityContext: this.securityContext,
    };
    return Object.freeze(context);
  }
}

export class SessionContextResolver {
  public static resolve(session: Session): SessionContext {
    return session.context;
  }

  public static resolveSecurityContext(session: Session) {
    return session.context.securityContext;
  }

  public static resolveCorrelationId(session: Session): string | undefined {
    return session.context.correlationId;
  }

  public static resolveRequestMetadata(session: Session): Record<string, unknown> {
    return session.context.requestMetadata || {};
  }

  public static resolveProtocolMetadata(session: Session): Record<string, unknown> {
    return session.context.protocolMetadata || {};
  }

  public static resolveClientMetadata(session: Session): Record<string, unknown> {
    return session.context.clientMetadata || {};
  }

  public static resolveClientInformation(session: Session): Record<string, unknown> {
    return session.context.clientInformation || {};
  }

  public static resolveConversationMetadata(session: Session): Record<string, unknown> {
    return session.context.conversationMetadata || {};
  }

  public static resolveRuntimeMetadata(session: Session): Record<string, unknown> {
    return session.context.runtimeMetadata || {};
  }

  public static resolveExecutionMetadata(session: Session): Record<string, unknown> {
    return session.context.executionMetadata || {};
  }
}
