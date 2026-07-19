import { DiagnosticCheck } from './diagnosticCheck';

export interface HealthDiagnosticResult {
  readonly checks: DiagnosticCheck[];
  readonly allPassed: boolean;
}

export interface ConfigurationDiagnosticResult {
  readonly checks: DiagnosticCheck[];
  readonly allPassed: boolean;
}

export interface ConnectivityDiagnosticResult {
  readonly checks: DiagnosticCheck[];
  readonly allPassed: boolean;
}

export interface EnvironmentDiagnosticResult {
  readonly checks: DiagnosticCheck[];
  readonly allPassed: boolean;
}

export interface DiagnosticsReportResult {
  readonly health: HealthDiagnosticResult;
  readonly configuration: ConfigurationDiagnosticResult;
  readonly connectivity: ConnectivityDiagnosticResult;
  readonly environment: EnvironmentDiagnosticResult;
  readonly allPassed: boolean;
  readonly generatedAt: string;
}
