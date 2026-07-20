export class MemoraMcpError extends Error {
  constructor(message: string) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

export class ConfigurationValidationError extends MemoraMcpError {
  constructor(message: string) {
    super(`Configuration Validation Error: ${message}`);
  }
}

export class TransportInitializationError extends MemoraMcpError {
  constructor(message: string) {
    super(`Transport Initialization Error: ${message}`);
  }
}

export class RegistryInitializationError extends MemoraMcpError {
  constructor(message: string) {
    super(`Registry Initialization Error: ${message}`);
  }
}

export class LifecycleTransitionError extends MemoraMcpError {
  constructor(message: string) {
    super(`Lifecycle Transition Error: ${message}`);
  }
}

export class JsonRpcError extends MemoraMcpError {
  public readonly code: number;
  public readonly data?: unknown;

  constructor(code: number, message: string, data?: unknown) {
    super(message);
    this.code = code;
    this.data = data;
  }
}

export class JsonRpcParseError extends JsonRpcError {
  constructor(message = 'Parse error') {
    super(-32700, message);
  }
}

export class JsonRpcInvalidRequestError extends JsonRpcError {
  constructor(message = 'Invalid Request') {
    super(-32600, message);
  }
}

export class JsonRpcMethodNotFoundError extends JsonRpcError {
  constructor(method: string) {
    super(-32601, `Method not found: ${method}`);
  }
}

export class JsonRpcInvalidParamsError extends JsonRpcError {
  constructor(message = 'Invalid params') {
    super(-32602, message);
  }
}

export class JsonRpcInternalError extends JsonRpcError {
  constructor(message = 'Internal error', data?: unknown) {
    super(-32603, message, data);
  }
}

export class ToolError extends JsonRpcError {
  constructor(message: string, code = -32603, data?: unknown) {
    super(code, message, data);
    this.name = 'ToolError';
  }
}

export class ToolValidationError extends ToolError {
  constructor(message: string) {
    super(message, -32602);
    this.name = 'ToolValidationError';
  }
}

export class ToolNotFoundError extends ToolError {
  constructor(message: string) {
    super(message, -32601);
    this.name = 'ToolNotFoundError';
  }
}

export class ToolExecutionError extends ToolError {
  constructor(message: string, data?: unknown) {
    super(message, -32603, data);
    this.name = 'ToolExecutionError';
  }
}

export class ToolOutputValidationError extends ToolError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'ToolOutputValidationError';
  }
}

export class ToolRegistrationError extends ToolError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'ToolRegistrationError';
  }
}

export class ResourceError extends JsonRpcError {
  constructor(message: string, code = -32603, data?: unknown) {
    super(code, message, data);
    this.name = 'ResourceError';
  }
}

export class ResourceValidationError extends ResourceError {
  constructor(message: string) {
    super(message, -32602);
    this.name = 'ResourceValidationError';
  }
}

export class ResourceNotFoundError extends ResourceError {
  constructor(message: string) {
    super(message, -32601);
    this.name = 'ResourceNotFoundError';
  }
}

export class ResourceExecutionError extends ResourceError {
  constructor(message: string, data?: unknown) {
    super(message, -32603, data);
    this.name = 'ResourceExecutionError';
  }
}

export class ResourceOutputValidationError extends ResourceError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'ResourceOutputValidationError';
  }
}

export class ResourceRegistrationError extends ResourceError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'ResourceRegistrationError';
  }
}

export class PromptError extends JsonRpcError {
  constructor(message: string, code = -32603, data?: unknown) {
    super(code, message, data);
    this.name = 'PromptError';
  }
}

export class PromptValidationError extends PromptError {
  constructor(message: string) {
    super(message, -32602);
    this.name = 'PromptValidationError';
  }
}

export class PromptNotFoundError extends PromptError {
  constructor(message: string) {
    super(message, -32601);
    this.name = 'PromptNotFoundError';
  }
}

export class PromptExecutionError extends PromptError {
  constructor(message: string, data?: unknown) {
    super(message, -32603, data);
    this.name = 'PromptExecutionError';
  }
}

export class PromptOutputValidationError extends PromptError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'PromptOutputValidationError';
  }
}

export class PromptRegistrationError extends PromptError {
  constructor(message: string) {
    super(message, -32603);
    this.name = 'PromptRegistrationError';
  }
}

export class SecurityError extends JsonRpcError {
  constructor(message: string, code = -32603, data?: unknown) {
    super(code, message, data);
    this.name = 'SecurityError';
  }
}

export class AuthenticationError extends SecurityError {
  constructor(message: string, data?: unknown) {
    super(message, -32602, data);
    this.name = 'AuthenticationError';
  }
}

export class AuthorizationError extends SecurityError {
  constructor(message: string, data?: unknown) {
    super(message, -32602, data);
    this.name = 'AuthorizationError';
  }
}

export class PermissionDeniedError extends SecurityError {
  constructor(message: string, data?: unknown) {
    super(message, -32602, data);
    this.name = 'PermissionDeniedError';
  }
}

export class InvalidCredentialError extends AuthenticationError {
  constructor(message: string, data?: unknown) {
    super(message, data);
    this.name = 'InvalidCredentialError';
  }
}

export class SecurityConfigurationError extends SecurityError {
  constructor(message: string, data?: unknown) {
    super(message, -32603, data);
    this.name = 'SecurityConfigurationError';
  }
}

export class SessionError extends MemoraMcpError {
  constructor(message: string) {
    super(message);
    this.name = 'SessionError';
  }
}

export class SessionNotFoundError extends SessionError {
  constructor(message: string) {
    super(`Session Not Found: ${message}`);
    this.name = 'SessionNotFoundError';
  }
}

export class DuplicateSessionError extends SessionError {
  constructor(message: string) {
    super(`Duplicate Session: ${message}`);
    this.name = 'DuplicateSessionError';
  }
}

export class SessionExpiredError extends SessionError {
  constructor(message: string) {
    super(`Session Expired: ${message}`);
    this.name = 'SessionExpiredError';
  }
}

export class SessionValidationError extends SessionError {
  constructor(message: string) {
    super(`Session Validation Error: ${message}`);
    this.name = 'SessionValidationError';
  }
}

export class ReliabilityError extends MemoraMcpError {
  constructor(message: string) {
    super(message);
    this.name = 'ReliabilityError';
  }
}

export class RequestTimeoutError extends ReliabilityError {
  constructor(message: string) {
    super(`Request Timeout: ${message}`);
    this.name = 'RequestTimeoutError';
  }
}

export class QueueOverflowError extends ReliabilityError {
  constructor(message: string) {
    super(`Queue Overflow: ${message}`);
    this.name = 'QueueOverflowError';
  }
}

export class ShutdownError extends ReliabilityError {
  constructor(message: string) {
    super(`Server Shutdown: ${message}`);
    this.name = 'ShutdownError';
  }
}


