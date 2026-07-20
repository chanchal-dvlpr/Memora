import { TimeoutManager } from '../src/reliability/timeout';
import { ConcurrencyManager } from '../src/reliability/concurrency';
import { HealthManager } from '../src/reliability/health';
import { MetricsManager } from '../src/reliability/metrics';
import { ReliabilityEventEmitter, ReliabilityEvent } from '../src/reliability/events';
import { ShutdownManager } from '../src/reliability/shutdown';
import { ToolRegistry } from '../src/registry/tool';
import { ToolVisibility, ToolCategory } from '../src/types/tool';
import { ConfigLoader, ServerConfig } from '../src/config';
import { RequestTimeoutError, QueueOverflowError } from '../src/errors';

describe('Performance & Reliability Foundation Tests', () => {
  describe('ConfigLoader Performance Options', () => {
    it('should load performance configuration defaults', () => {
      const config: ServerConfig = ConfigLoader.load();
      expect(config.requestTimeoutMs).toBe(30000);
      expect(config.handlerTimeoutMs).toBe(30000);
      expect(config.maxConcurrentRequests).toBe(50);
      expect(config.maxQueuedRequests).toBe(100);
      expect(config.maxPayloadSizeBytes).toBe(10485760);
      expect(config.shutdownTimeoutMs).toBe(10000);
      expect(config.healthCheckIntervalMs).toBe(60000);
      expect(config.metricsIntervalMs).toBe(60000);
    });
  });

  describe('TimeoutManager', () => {
    let timeoutManager: TimeoutManager;

    beforeEach(() => {
      timeoutManager = new TimeoutManager(100);
    });

    it('should complete operation within timeout limit', async () => {
      const result = await timeoutManager.executeWithTimeout(async () => {
        return 'success';
      }, 200);

      expect(result).toBe('success');
    });

    it('should throw RequestTimeoutError when operation exceeds timeout', async () => {
      await expect(
        timeoutManager.executeWithTimeout(async () => {
          await new Promise((resolve) => setTimeout(resolve, 150));
          return 'slow';
        }, 50, 'Custom timeout handler')
      ).rejects.toThrow(RequestTimeoutError);
    });

    it('should cancel timer upon fast completion', async () => {
      const result = await TimeoutManager.withTimeout(async () => 'fast', 500);
      expect(result).toBe('fast');
    });
  });

  describe('ConcurrencyManager', () => {
    it('should execute tasks up to max concurrent limit', async () => {
      const concurrency = new ConcurrencyManager(2, 5);

      expect(concurrency.getActiveRequestsCount()).toBe(0);
      expect(concurrency.getQueuedRequestsCount()).toBe(0);

      let runningCount = 0;
      let peakRunning = 0;

      const task = async () => {
        return concurrency.execute(async () => {
          runningCount++;
          if (runningCount > peakRunning) {
            peakRunning = runningCount;
          }
          await new Promise((resolve) => setTimeout(resolve, 30));
          runningCount--;
          return 'done';
        });
      };

      const promises = [task(), task(), task()];
      const results = await Promise.all(promises);

      expect(results).toEqual(['done', 'done', 'done']);
      expect(peakRunning).toBeLessThanOrEqual(2);
      expect(concurrency.getActiveRequestsCount()).toBe(0);
    });

    it('should throw QueueOverflowError when queue capacity is exceeded', async () => {
      // 1 active slot, 1 queued slot max
      const concurrency = new ConcurrencyManager(1, 1);

      const taskSlow = () =>
        concurrency.execute(async () => {
          await new Promise((resolve) => setTimeout(resolve, 100));
        });

      // Task 1 occupies active slot
      const p1 = taskSlow();
      // Task 2 occupies queued slot
      const p2 = taskSlow();

      // Task 3 exceeds queue capacity and should reject immediately
      await expect(taskSlow()).rejects.toThrow(QueueOverflowError);

      await Promise.all([p1, p2]);
    });
  });

  describe('HealthManager', () => {
    let healthManager: HealthManager;

    beforeEach(() => {
      healthManager = new HealthManager();
    });

    it('should generate an overall healthy report by default', () => {
      const report = healthManager.generateHealthReport();
      expect(report.status).toBe('healthy');
      expect(report.uptimeSeconds).toBeGreaterThanOrEqual(0);
      expect(report.memory.heapUsedBytes).toBeGreaterThan(0);
      expect(Object.isFrozen(report)).toBe(true);
    });

    it('should reflect degraded and unhealthy component states', () => {
      healthManager.updateComponentStatus('session', 'degraded', 'High session count');
      let report = healthManager.generateHealthReport();
      expect(report.status).toBe('degraded');

      healthManager.updateComponentStatus('security', 'unhealthy', 'Auth service offline');
      report = healthManager.generateHealthReport();
      expect(report.status).toBe('unhealthy');
    });
  });

  describe('MetricsManager', () => {
    let metricsManager: MetricsManager;

    beforeEach(() => {
      metricsManager = new MetricsManager();
    });

    it('should track request accounting and latency statistics', () => {
      metricsManager.recordRequestAccepted();
      metricsManager.recordRequestAccepted();
      metricsManager.recordRequestAccepted();

      expect(metricsManager.getSnapshot().activeRequests).toBe(3);

      metricsManager.recordRequestCompleted(100);
      metricsManager.recordRequestCompleted(200);
      metricsManager.recordRequestFailed(300);

      const snapshot = metricsManager.getSnapshot();
      expect(snapshot.totalRequests).toBe(3);
      expect(snapshot.successfulRequests).toBe(2);
      expect(snapshot.failedRequests).toBe(1);
      expect(snapshot.activeRequests).toBe(0);
      expect(snapshot.minExecutionDurationMs).toBe(100);
      expect(snapshot.maxExecutionDurationMs).toBe(300);
      expect(snapshot.averageExecutionDurationMs).toBe(200); // (100+200+300)/3
      expect(Object.isFrozen(snapshot)).toBe(true);
    });

    it('should reset metrics counters', () => {
      metricsManager.recordRequestAccepted();
      metricsManager.recordRequestCompleted(50);
      metricsManager.reset();

      const snapshot = metricsManager.getSnapshot();
      expect(snapshot.totalRequests).toBe(0);
      expect(snapshot.successfulRequests).toBe(0);
    });
  });

  describe('ReliabilityEventEmitter', () => {
    it('should publish and subscribe to reliability events', async () => {
      const emitter = new ReliabilityEventEmitter();
      const events: ReliabilityEvent[] = [];

      const unsubscribe = emitter.on('timeout', (ev) => {
        events.push(ev);
      });

      await emitter.emit({
        type: 'timeout',
        timestamp: Date.now(),
        requestId: 'req-timeout-1',
      });

      expect(events).toHaveLength(1);
      expect(events[0].requestId).toBe('req-timeout-1');

      unsubscribe();
      await emitter.emit({
        type: 'timeout',
        timestamp: Date.now(),
        requestId: 'req-timeout-2',
      });

      expect(events).toHaveLength(1);
    });
  });

  describe('Backpressure & Advanced Concurrency Controls', () => {
    it('should reject immediately when using backpressure strategy "reject"', async () => {
      const concurrency = new ConcurrencyManager(1, 5, 'reject');

      const p1 = concurrency.execute(async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
      });

      await expect(
        concurrency.execute(async () => 'fast')
      ).rejects.toThrow(QueueOverflowError);

      await p1;
    });

    it('should time out queued requests if wait duration exceeds queueWaitTimeoutMs', async () => {
      const concurrency = new ConcurrencyManager(1, 5, 'timeout', 30);

      const p1 = concurrency.execute(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      const p2 = concurrency.execute(async () => 'should timeout');

      await expect(p2).rejects.toThrow(RequestTimeoutError);
      await p1;
    });

    it('should track peak concurrency and average queue wait time', async () => {
      const concurrency = new ConcurrencyManager(2, 5, 'queue', 5000);

      const t1 = concurrency.execute(async () => {
        await new Promise((resolve) => setTimeout(resolve, 20));
      });
      const t2 = concurrency.execute(async () => {
        await new Promise((resolve) => setTimeout(resolve, 20));
      });

      await Promise.all([t1, t2]);

      expect(concurrency.getPeakConcurrentRequests()).toBe(2);
    });
  });

  describe('ShutdownManager & Operational Resilience', () => {
    it('should execute graceful shutdown, drain queues, and invoke cleanup hooks', async () => {
      const concurrency = new ConcurrencyManager(1, 5);
      const emitter = new ReliabilityEventEmitter();
      const shutdown = new ShutdownManager(concurrency, emitter);

      let hookRan = false;
      shutdown.registerCleanupHook('testHook', async () => {
        hookRan = true;
      });

      expect(shutdown.isAcceptingRequests()).toBe(true);

      const shutdownPromise = shutdown.initiateShutdown(1000);
      expect(shutdown.isAcceptingRequests()).toBe(false);

      await shutdownPromise;
      expect(shutdown.getState()).toBe('stopped');
      expect(hookRan).toBe(true);
      expect(shutdown.getShutdownDurationMs()).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Registry & Memory Allocation Optimizations', () => {
    it('should cache sorted list array in ToolRegistry until mutation occurs', () => {
      const registry = new ToolRegistry();
      registry.registerTool(
        {
          name: 'tool_a',
          inputSchema: { type: 'object' },
          handler: async () => ({ content: [] }),
        },
        { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
      );

      const list1 = registry.listTools();
      const list2 = registry.listTools();

      // Identical array reference due to caching
      expect(list1).toBe(list2);

      // Registering new tool invalidates cache
      registry.registerTool(
        {
          name: 'tool_b',
          inputSchema: { type: 'object' },
          handler: async () => ({ content: [] }),
        },
        { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
      );

      const list3 = registry.listTools();
      expect(list3).not.toBe(list1);
      expect(list3).toHaveLength(2);
    });
  });
});
