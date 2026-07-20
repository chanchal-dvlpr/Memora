import { performance } from 'perf_hooks';
import { ToolRegistry } from '../registry/tool';
import { ResourceRegistry } from '../registry/resource';
import { PromptRegistry } from '../registry/prompt';
import { ToolDispatcher } from '../tool/executor';
import { ResourceDispatcher } from '../resource/executor';
import { PromptDispatcher } from '../prompt/executor';
import { ToolCategory, ToolVisibility } from '../types/tool';
import { ResourceCategory, ResourceVisibility } from '../types/resource';
import { PromptCategory, PromptVisibility } from '../types/prompt';
import { SessionManager } from '../session/manager';
import { AuthenticationManager, AuthorizationManager } from '../security';

import { SecurityAction } from '../types/security';

export interface MetricStats {
  readonly averageMs: number;
  readonly medianMs: number;
  readonly p95Ms: number;
  readonly maxMs: number;
  readonly sampleCount: number;
}

export interface BenchmarkReport {
  readonly timestamp: number;
  readonly iterations: number;
  readonly metrics: {
    readonly toolDispatchLatency: MetricStats;
    readonly resourceDispatchLatency: MetricStats;
    readonly promptDispatchLatency: MetricStats;
    readonly sessionMiddlewareOverhead: MetricStats;
    readonly securityMiddlewareOverhead: MetricStats;
    readonly registryLookupLatency: MetricStats;
    readonly contextPropagationOverhead: MetricStats;
  };
}

export class BenchmarkRunner {
  public static calculateStats(samples: number[]): MetricStats {
    if (samples.length === 0) {
      return { averageMs: 0, medianMs: 0, p95Ms: 0, maxMs: 0, sampleCount: 0 };
    }

    const sorted = [...samples].sort((a, b) => a - b);
    const sum = sorted.reduce((acc, v) => acc + v, 0);
    const averageMs = sum / sorted.length;
    const medianMs = sorted[Math.floor(sorted.length * 0.5)] || 0;
    const p95Ms = sorted[Math.floor(sorted.length * 0.95)] || sorted[sorted.length - 1];
    const maxMs = sorted[sorted.length - 1];

    return {
      averageMs,
      medianMs,
      p95Ms,
      maxMs,
      sampleCount: sorted.length,
    };
  }

  /**
   * Runs the full performance benchmark suite across dispatchers, middlewares, and registries.
   */
  public async runBenchmark(iterations = 100): Promise<BenchmarkReport> {
    const toolRegistry = new ToolRegistry();
    toolRegistry.registerTool(
      {
        name: 'bench_tool',
        description: 'bench',
        inputSchema: { type: 'object' },
        handler: async () => ({ content: [{ type: 'text', text: 'bench' }] }),
      },
      { visibility: ToolVisibility.PUBLIC, categories: [ToolCategory.UTILITY] }
    );

    const resourceRegistry = new ResourceRegistry();
    resourceRegistry.registerResource(
      {
        uri: 'memora://bench',
        name: 'bench_res',
        handler: async () => [{ uri: 'memora://bench', text: 'bench' }],
      },
      { visibility: ResourceVisibility.PUBLIC, categories: [ResourceCategory.UTILITY] }
    );

    const promptRegistry = new PromptRegistry();
    promptRegistry.registerPrompt(
      {
        name: 'bench_prompt',
        handler: async () => [{ role: 'user', content: { type: 'text', text: 'bench' } }],
      },
      { visibility: PromptVisibility.PUBLIC, category: PromptCategory.SYSTEM }
    );

    const sessionManager = new SessionManager();
    const authManager = new AuthenticationManager();
    const authzManager = new AuthorizationManager();

    const toolDispatcher = new ToolDispatcher(
      toolRegistry,
      authManager,
      authzManager,
      undefined,
      undefined,
      sessionManager
    );

    const resourceDispatcher = new ResourceDispatcher(
      resourceRegistry,
      authManager,
      authzManager,
      undefined,
      undefined,
      sessionManager
    );

    const promptDispatcher = new PromptDispatcher(
      promptRegistry,
      authManager,
      authzManager,
      undefined,
      undefined,
      sessionManager
    );

    const toolSamples: number[] = [];
    const resourceSamples: number[] = [];
    const promptSamples: number[] = [];
    const sessionSamples: number[] = [];
    const securitySamples: number[] = [];
    const lookupSamples: number[] = [];
    const contextSamples: number[] = [];

    for (let i = 0; i < iterations; i++) {
      const reqId = `bench-${i}`;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const ctx: any = {
        requestId: reqId,
        sessionId: `bench-session-${i % 5}`,
        protocolVersion: '2024-11-05',
        timestamp: Date.now(),
        logger: console,
        params: {},
        metadata: new Map(),
      };

      // 1. Tool Dispatch
      const t1 = performance.now();
      await toolDispatcher.dispatchCall('bench_tool', {}, ctx);
      toolSamples.push(performance.now() - t1);

      // 2. Resource Dispatch
      const t2 = performance.now();
      await resourceDispatcher.dispatchRead('memora://bench', ctx);
      resourceSamples.push(performance.now() - t2);

      // 3. Prompt Dispatch
      const t3 = performance.now();
      await promptDispatcher.dispatchGet('bench_prompt', {}, ctx);
      promptSamples.push(performance.now() - t3);

      // 4. Session Overhead
      const t4 = performance.now();
      await sessionManager.touchSession(`bench-session-${i % 5}`);
      sessionSamples.push(performance.now() - t4);

      // 5. Security Overhead
      const t5 = performance.now();
      await authzManager.authorize(
        { principal: undefined, roles: [], permissions: [], timestamp: Date.now(), metadata: new Map() },
        SecurityAction.READ,
        'memora://bench'
      );
      securitySamples.push(performance.now() - t5);

      // 6. Registry Lookup
      const t6 = performance.now();
      toolRegistry.getTool('bench_tool');
      lookupSamples.push(performance.now() - t6);

      // 7. Context Propagation
      const t7 = performance.now();
      const dummy = { ...ctx, prop: true };
      void dummy;
      contextSamples.push(performance.now() - t7);
    }

    const report: BenchmarkReport = {
      timestamp: Date.now(),
      iterations,
      metrics: {
        toolDispatchLatency: BenchmarkRunner.calculateStats(toolSamples),
        resourceDispatchLatency: BenchmarkRunner.calculateStats(resourceSamples),
        promptDispatchLatency: BenchmarkRunner.calculateStats(promptSamples),
        sessionMiddlewareOverhead: BenchmarkRunner.calculateStats(sessionSamples),
        securityMiddlewareOverhead: BenchmarkRunner.calculateStats(securitySamples),
        registryLookupLatency: BenchmarkRunner.calculateStats(lookupSamples),
        contextPropagationOverhead: BenchmarkRunner.calculateStats(contextSamples),
      },
    };

    return Object.freeze(report);
  }
}
