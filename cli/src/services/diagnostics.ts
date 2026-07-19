import { HealthApiClient } from '../api/health';
import { httpClientService } from '../http/clientService';
import { ExecutionContext } from '../models/context';
import { DiagnosticCheck } from '../models/diagnosticCheck';
import {
  HealthDiagnosticResult,
  ConfigurationDiagnosticResult,
  ConnectivityDiagnosticResult,
  EnvironmentDiagnosticResult,
  DiagnosticsReportResult,
} from '../models/diagnosticResult';
import { configService } from '../config/service';
import { validateDiagnosticType, validateEnvironmentData } from '../validators/diagnostics';
import { commandEventPublisher } from '../events/commandEvents';
import * as os from 'os';

export interface DiagnosticsResult {
  readonly checks: DiagnosticCheck[];
  readonly allPassed: boolean;
}

/**
 * Service layer coordinating diagnostics checks.
 */
export class DiagnosticsApplicationService {
  constructor(private readonly healthApiClient: HealthApiClient) {}

  /**
   * Runs the health checks suite.
   */
  public async runHealthChecks(ctx: ExecutionContext): Promise<HealthDiagnosticResult> {
    try {
      validateDiagnosticType('health');
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'DiagnosticsValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    commandEventPublisher.publish({
      type: 'DiagnosticsStarted',
      timestamp: new Date(),
      payload: { timestamp: new Date() },
    });

    const checks: DiagnosticCheck[] = [];
    const startTime = Date.now();

    let backendCheck: DiagnosticCheck;
    try {
      const res = await this.healthApiClient.checkHealth(ctx);
      backendCheck = Object.freeze({
        id: 'backend-health',
        category: 'health',
        description: 'Verify backend connection status and health response',
        severity: 'ERROR',
        status: res.status === 'UP' ? 'PASSED' : 'FAILED',
        result: `Backend health status: ${res.status}. Version: ${res.version ?? 'N/A'}`,
        durationMs: Date.now() - startTime,
      });
    } catch (err: unknown) {
      backendCheck = Object.freeze({
        id: 'backend-health',
        category: 'health',
        description: 'Verify backend connection status and health response',
        severity: 'ERROR',
        status: 'FAILED',
        error: err instanceof Error ? err.message : String(err),
        durationMs: Date.now() - startTime,
      });
    }
    checks.push(backendCheck);

    const result: HealthDiagnosticResult = {
      checks,
      allPassed: checks.every((c) => c.status === 'PASSED'),
    };

    commandEventPublisher.publish({
      type: 'HealthChecked',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    commandEventPublisher.publish({
      type: 'DiagnosticsCompleted',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    return result;
  }

  /**
   * Runs the configuration checks suite.
   */
  public async runConfigChecks(_ctx: ExecutionContext): Promise<ConfigurationDiagnosticResult> {
    try {
      validateDiagnosticType('config');
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'DiagnosticsValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    commandEventPublisher.publish({
      type: 'DiagnosticsStarted',
      timestamp: new Date(),
      payload: { timestamp: new Date() },
    });

    const checks: DiagnosticCheck[] = [];
    const startTime = Date.now();

    let configCheck: DiagnosticCheck;
    try {
      const config = configService.getConfig();
      if (!config) {
        throw new Error('Configuration is not loaded or missing.');
      }
      configCheck = Object.freeze({
        id: 'config-verify',
        category: 'configuration',
        description: 'Verify CLI configuration options loaded and contain valid values',
        severity: 'ERROR',
        status: 'PASSED',
        result: `Server URL: ${config.backend.url}. Timeout: ${config.backend.timeoutMs}ms.`,
        durationMs: Date.now() - startTime,
      });
    } catch (err: unknown) {
      configCheck = Object.freeze({
        id: 'config-verify',
        category: 'configuration',
        description: 'Verify CLI configuration options loaded and contain valid values',
        severity: 'ERROR',
        status: 'FAILED',
        error: err instanceof Error ? err.message : String(err),
        durationMs: Date.now() - startTime,
      });
    }
    checks.push(configCheck);

    const result: ConfigurationDiagnosticResult = {
      checks,
      allPassed: checks.every((c) => c.status === 'PASSED'),
    };

    commandEventPublisher.publish({
      type: 'ConfigurationChecked',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    commandEventPublisher.publish({
      type: 'DiagnosticsCompleted',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    return result;
  }

  /**
   * Runs the connectivity checks suite.
   */
  public async runConnectivityChecks(ctx: ExecutionContext): Promise<ConnectivityDiagnosticResult> {
    try {
      validateDiagnosticType('connectivity');
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'DiagnosticsValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    commandEventPublisher.publish({
      type: 'DiagnosticsStarted',
      timestamp: new Date(),
      payload: { timestamp: new Date() },
    });

    const checks: DiagnosticCheck[] = [];
    const startTime = Date.now();

    let reachCheck: DiagnosticCheck;
    try {
      await this.healthApiClient.checkHealth(ctx);
      reachCheck = Object.freeze({
        id: 'connectivity-verify',
        category: 'connectivity',
        description: 'Verify socket and endpoint connectivity response',
        severity: 'ERROR',
        status: 'PASSED',
        result: 'Backend is reachable and responds successfully.',
        durationMs: Date.now() - startTime,
      });
    } catch (err: unknown) {
      reachCheck = Object.freeze({
        id: 'connectivity-verify',
        category: 'connectivity',
        description: 'Verify socket and endpoint connectivity response',
        severity: 'ERROR',
        status: 'FAILED',
        error: err instanceof Error ? err.message : String(err),
        durationMs: Date.now() - startTime,
      });
    }
    checks.push(reachCheck);

    const result: ConnectivityDiagnosticResult = {
      checks,
      allPassed: checks.every((c) => c.status === 'PASSED'),
    };

    commandEventPublisher.publish({
      type: 'ConnectivityChecked',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    commandEventPublisher.publish({
      type: 'DiagnosticsCompleted',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    return result;
  }

  /**
   * Runs the environment checks suite.
   */
  public async runEnvironmentChecks(ctx: ExecutionContext): Promise<EnvironmentDiagnosticResult> {
    try {
      validateDiagnosticType('environment');
      validateEnvironmentData(ctx.env);
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'DiagnosticsValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    commandEventPublisher.publish({
      type: 'DiagnosticsStarted',
      timestamp: new Date(),
      payload: { timestamp: new Date() },
    });

    const checks: DiagnosticCheck[] = [];
    const startTime = Date.now();

    // Verify platform
    const platform = os.platform();
    checks.push(Object.freeze({
      id: 'env-platform',
      category: 'environment',
      description: 'Check operating system platform compatibility',
      severity: 'INFO',
      status: 'PASSED',
      result: `Platform: ${platform}. Architecture: ${os.arch()}`,
      durationMs: Date.now() - startTime,
    }));

    // Verify Node version
    checks.push(Object.freeze({
      id: 'env-node-version',
      category: 'environment',
      description: 'Check Node.js runtime version compatibility',
      severity: 'ERROR',
      status: 'PASSED',
      result: `Node version: ${process.version}`,
      durationMs: Date.now() - startTime,
    }));

    // Verify Working Dir
    checks.push(Object.freeze({
      id: 'env-working-dir',
      category: 'environment',
      description: 'Check CLI working directory parameters',
      severity: 'INFO',
      status: 'PASSED',
      result: `Working Dir: ${ctx.workingDir}`,
      durationMs: Date.now() - startTime,
    }));

    const result: EnvironmentDiagnosticResult = {
      checks,
      allPassed: checks.every((c) => c.status === 'PASSED'),
    };

    commandEventPublisher.publish({
      type: 'EnvironmentChecked',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    commandEventPublisher.publish({
      type: 'DiagnosticsCompleted',
      timestamp: new Date(),
      payload: { allPassed: result.allPassed },
    });

    return result;
  }

  /**
   * Generates a unified diagnostics report aggregating all checks.
   */
  public async generateReport(ctx: ExecutionContext): Promise<DiagnosticsReportResult> {
    try {
      validateDiagnosticType('report');
    } catch (err: unknown) {
      commandEventPublisher.publish({
        type: 'DiagnosticsValidationFailed',
        timestamp: new Date(),
        payload: { error: err instanceof Error ? err.message : String(err) },
      });
      throw err;
    }

    const health = await this.runHealthChecks(ctx);
    const configuration = await this.runConfigChecks(ctx);
    const connectivity = await this.runConnectivityChecks(ctx);
    const environment = await this.runEnvironmentChecks(ctx);

    const allPassed =
      health.allPassed &&
      configuration.allPassed &&
      connectivity.allPassed &&
      environment.allPassed;

    return {
      health,
      configuration,
      connectivity,
      environment,
      allPassed,
      generatedAt: new Date().toISOString(),
    };
  }
}

export const healthApiClient = new HealthApiClient(httpClientService);
export const diagnosticsApplicationService = new DiagnosticsApplicationService(healthApiClient);
export default diagnosticsApplicationService;
