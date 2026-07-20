import { SecurityPrincipal } from '../types/security';
import { AuthenticationError, InvalidCredentialError } from '../errors';

export interface AuthenticationRequest {
  readonly credentials: Record<string, string>;
  readonly metadata?: Record<string, unknown>;
}

export interface AuthenticationResponse {
  readonly success: boolean;
  readonly principal?: SecurityPrincipal;
  readonly token?: string;
  readonly error?: string;
}

export interface AuthenticationProvider {
  readonly name: string;
  authenticate(request: AuthenticationRequest): Promise<AuthenticationResponse>;
  invalidate(token: string): Promise<void>;
  refresh(token: string): Promise<AuthenticationResponse>;
}

export class AuthenticationManager {
  private providers = new Map<string, AuthenticationProvider>();

  public registerProvider(provider: AuthenticationProvider): void {
    if (this.providers.has(provider.name)) {
      throw new AuthenticationError(`Authentication provider "${provider.name}" is already registered.`);
    }
    this.providers.set(provider.name, provider);
  }

  public async authenticate(providerName: string, request: AuthenticationRequest): Promise<AuthenticationResponse> {
    const provider = this.providers.get(providerName);
    if (!provider) {
      throw new AuthenticationError(`Authentication provider "${providerName}" is not registered.`);
    }
    return provider.authenticate(request);
  }

  public async invalidate(providerName: string, token: string): Promise<void> {
    const provider = this.providers.get(providerName);
    if (!provider) {
      throw new AuthenticationError(`Authentication provider "${providerName}" is not registered.`);
    }
    await provider.invalidate(token);
  }

  public async refresh(providerName: string, token: string): Promise<AuthenticationResponse> {
    const provider = this.providers.get(providerName);
    if (!provider) {
      throw new AuthenticationError(`Authentication provider "${providerName}" is not registered.`);
    }
    return provider.refresh(token);
  }
}

export class MockAuthenticationProvider implements AuthenticationProvider {
  public readonly name = 'mock';
  private validTokens = new Set<string>();

  public async authenticate(request: AuthenticationRequest): Promise<AuthenticationResponse> {
    const username = request.credentials['username'];
    const password = request.credentials['password'];
    const token = request.credentials['token'];

    if (token) {
      if (token === 'valid-mock-token') {
        this.validTokens.add(token);
        return {
          success: true,
          principal: {
            id: 'mock-user-id',
            name: 'mock-user',
            roles: ['developer'],
            metadata: new Map([['source', 'mock-provider']]),
          },
          token,
        };
      }
      throw new InvalidCredentialError('Invalid token provided.');
    }

    if (username === 'admin' && password === 'admin-password') {
      const generatedToken = 'mock-admin-token';
      this.validTokens.add(generatedToken);
      return {
        success: true,
        principal: {
          id: 'admin-id',
          name: 'admin',
          roles: ['admin', 'developer'],
          metadata: new Map([['source', 'mock-provider']]),
        },
        token: generatedToken,
      };
    }

    if (username === 'user' && password === 'user-password') {
      const generatedToken = 'mock-user-token';
      this.validTokens.add(generatedToken);
      return {
        success: true,
        principal: {
          id: 'user-id',
          name: 'user',
          roles: ['developer'],
          metadata: new Map([['source', 'mock-provider']]),
        },
        token: generatedToken,
      };
    }

    throw new InvalidCredentialError('Invalid username or password.');
  }

  public async invalidate(token: string): Promise<void> {
    this.validTokens.delete(token);
  }

  public async refresh(token: string): Promise<AuthenticationResponse> {
    if (this.validTokens.has(token)) {
      const name = token.includes('admin') ? 'admin' : 'user';
      const roles = token.includes('admin') ? ['admin', 'developer'] : ['developer'];
      return {
        success: true,
        principal: {
          id: `${name}-id`,
          name,
          roles,
          metadata: new Map([['source', 'mock-provider']]),
        },
        token,
      };
    }
    throw new InvalidCredentialError('Token session has expired or is invalid.');
  }
}
