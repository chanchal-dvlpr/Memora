import { ConcurrencyManager } from '../src/reliability/concurrency';
import { TimeoutManager } from '../src/reliability/timeout';
import { MetricsManager } from '../src/reliability/metrics';
import { HealthManager } from '../src/reliability/health';
import { ShutdownManager } from '../src/reliability/shutdown';
import { ReliabilityEventEmitter } from '../src/reliability/events';

describe('Integration: Reliability Subsystem Workload (Phase 13.9.6)', () => {
  it('should process concurrent workload and accurately update metrics and health state', async () => {
    const concurrency = new ConcurrencyManager(5, 50);
    const metrics = new MetricsManager();
    const health = new HealthManager();
    const emitter = new ReliabilityEventEmitter();
    const shutdown = new ShutdownManager(concurrency, emitter);

    health.updateComponentStatus('server', 'healthy');

    const tasks = Array.from({ length: 25 }).map((_, _i) =>
      concurrency.execute(async () => {
        metrics.recordRequestAccepted();
        await TimeoutManager.withTimeout(async () => {
          await new Promise((resolve) => setTimeout(resolve, 5));
        }, 1000);
        metrics.recordRequestCompleted(10);
      })
    );

    await Promise.all(tasks);

    const snapshot = metrics.getSnapshot();
    expect(snapshot.totalRequests).toBe(25);
    expect(snapshot.successfulRequests).toBe(25);
    expect(concurrency.getPeakConcurrentRequests()).toBeLessThanOrEqual(5);

    await shutdown.initiateShutdown(1000);
    expect(shutdown.getState()).toBe('stopped');
  });
});
