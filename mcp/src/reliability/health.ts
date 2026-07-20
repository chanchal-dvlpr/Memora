import {
  HealthReport,
  HealthStatus,
  HealthComponentStatus,
  MemorySnapshot,
} from '../types/reliability';

export class HealthManager {
  private readonly componentStatuses = new Map<string, HealthComponentStatus>();

  constructor() {
    this.updateComponentStatus('server', 'healthy', 'Server runtime operational');
    this.updateComponentStatus('registry', 'healthy', 'Registries initialized');
    this.updateComponentStatus('session', 'healthy', 'Session store operational');
    this.updateComponentStatus('security', 'healthy', 'Security framework operational');
  }

  public updateComponentStatus(
    name: string,
    status: HealthStatus,
    message?: string,
    details?: Record<string, unknown>
  ): void {
    this.componentStatuses.set(name, {
      name,
      status,
      message,
      timestamp: Date.now(),
      details: details ? Object.freeze({ ...details }) : undefined,
    });
  }

  public getMemorySnapshot(): MemorySnapshot {
    const mem = process.memoryUsage();
    return Object.freeze({
      heapUsedBytes: mem.heapUsed,
      heapTotalBytes: mem.heapTotal,
      rssBytes: mem.rss,
      externalBytes: mem.external,
      arrayBuffersBytes: mem.arrayBuffers || 0,
    });
  }

  public generateHealthReport(): HealthReport {
    const components = Array.from(this.componentStatuses.values());
    
    // Overall status is degraded if any component is degraded, unhealthy if any component is unhealthy
    let overallStatus: HealthStatus = 'healthy';
    if (components.some((c) => c.status === 'unhealthy')) {
      overallStatus = 'unhealthy';
    } else if (components.some((c) => c.status === 'degraded')) {
      overallStatus = 'degraded';
    }

    const report: HealthReport = {
      status: overallStatus,
      timestamp: Date.now(),
      uptimeSeconds: process.uptime(),
      components: Object.freeze(components),
      memory: this.getMemorySnapshot(),
    };

    return Object.freeze(report);
  }
}
