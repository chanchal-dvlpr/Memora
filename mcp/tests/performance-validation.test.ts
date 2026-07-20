import { BenchmarkRunner } from '../src/reliability/benchmark';
import { ConcurrencyManager } from '../src/reliability/concurrency';
import { ShutdownManager } from '../src/reliability/shutdown';
import { SessionManager } from '../src/session/manager';
import { ContextStore } from '../src/session/store';
import { ToolRegistry } from '../src/registry/tool';
import { ToolVisibility, ToolCategory } from '../src/types/tool';
import { ReliabilityEventEmitter } from '../src/reliability/events';

describe('Performance Validation & Completion Tests (Phase 13.8.9–13.8.10)', () => {
  describe('Benchmark Framework Validation', () => {
    it('should calculate accurate percentile statistics for latency samples', () => {
      const samples = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      const stats = BenchmarkRunner.calculateStats(samples);

      expect(stats.averageMs).toBe(5.5);
      expect(stats.medianMs).toBe(6);
      expect(stats.maxMs).toBe(10);
      expect(stats.sampleCount).toBe(10);
    });

    it('should execute benchmark suite and return a complete report', async () => {
      const runner = new BenchmarkRunner();
      const report = await runner.runBenchmark(20);

      expect(report.iterations).toBe(20);
      expect(report.metrics.toolDispatchLatency.sampleCount).toBe(20);
      expect(report.metrics.resourceDispatchLatency.sampleCount).toBe(20);
      expect(report.metrics.promptDispatchLatency.sampleCount).toBe(20);
      expect(report.metrics.sessionMiddlewareOverhead.sampleCount).toBe(20);
      expect(report.metrics.securityMiddlewareOverhead.sampleCount).toBe(20);
      expect(report.metrics.registryLookupLatency.sampleCount).toBe(20);
      expect(report.metrics.contextPropagationOverhead.sampleCount).toBe(20);
      expect(Object.isFrozen(report)).toBe(true);
    });
  });

  describe('Stress & Reliability Validation Under Load', () => {
    it('should handle sustained concurrent requests without failure', async () => {
      const concurrency = new ConcurrencyManager(10, 50);

      const tasks = Array.from({ length: 40 }).map((_, i) =>
        concurrency.execute(async () => {
          await new Promise((resolve) => setTimeout(resolve, 10));
          return `task-${i}`;
        })
      );

      const results = await Promise.all(tasks);
      expect(results).toHaveLength(40);
      expect(concurrency.getPeakConcurrentRequests()).toBe(10);
      expect(concurrency.getActiveRequestsCount()).toBe(0);
    });

    it('should support repeated session creation and cleanup cycles without leaking memory', async () => {
      const store = new ContextStore();
      const manager = new SessionManager(undefined, undefined, {
        expirationPolicy: { expirationType: 'absolute', absoluteTimeoutMs: 10 },
      });
      const cleanup = manager.getCleanupManager(store);

      for (let i = 0; i < 50; i++) {
        const id = `session-cycle-${i}`;
        await manager.openSession(id);
        store.put(id, 'key', `value-${i}`);
      }

      expect(manager.getRegistry().listSessions()).toHaveLength(50);

      // Wait for expiration
      await new Promise((resolve) => setTimeout(resolve, 30));

      const purged = await cleanup.cleanup();
      expect(purged.removedSessionsCount).toBe(50);
      expect(purged.removedContextsCount).toBe(50);
      expect(manager.getRegistry().listSessions()).toHaveLength(0);
    });

    it('should complete graceful shutdown under active concurrent load', async () => {
      const concurrency = new ConcurrencyManager(5, 20);
      const emitter = new ReliabilityEventEmitter();
      const shutdown = new ShutdownManager(concurrency, emitter);

      let eventsEmitted = 0;
      emitter.on('shutdownStarted', () => {
        eventsEmitted++;
      });
      emitter.on('shutdownCompleted', () => {
        eventsEmitted++;
      });

      // Launch background active tasks
      const activeTasks = Array.from({ length: 5 }).map(() =>
        concurrency.execute(async () => {
          await new Promise((resolve) => setTimeout(resolve, 100));
        })
      );

      // Initiate shutdown
      const shutdownPromise = shutdown.initiateShutdown(2000);

      await Promise.all([shutdownPromise, ...activeTasks]);

      expect(shutdown.getState()).toBe('stopped');
      expect(eventsEmitted).toBe(2);
    });
  });

  describe('Memory Optimization Invariants', () => {
    it('should preserve cache reference for tool listings across multiple reads', () => {
      const registry = new ToolRegistry();
      registry.registerTool(
        {
          name: 'tool_cached',
          inputSchema: { type: 'object' },
          handler: async () => ({ content: [] }),
        },
        { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
      );

      const r1 = registry.listTools();
      const r2 = registry.listTools();
      const r3 = registry.listTools();

      expect(r1).toBe(r2);
      expect(r2).toBe(r3);
    });
  });
});
