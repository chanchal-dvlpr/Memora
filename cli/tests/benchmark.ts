import { run } from '../src/cli';

/**
 * Performance benchmarking suite measuring latencies and memory usage metrics.
 */
async function runBenchmark() {
  const startCpu = process.cpuUsage();
  const startMemory = process.memoryUsage().heapUsed;
  const startTime = process.hrtime.bigint();

  // Execute a standard subcommand
  const exitCode = await run(['node', 'memora', 'status', '--verbose']);

  const endTime = process.hrtime.bigint();
  const endMemory = process.memoryUsage().heapUsed;
  const endCpu = process.cpuUsage(startCpu);

  const durationMs = Number(endTime - startTime) / 1_000_000;
  const memoryDeltaMb = (endMemory - startMemory) / 1024 / 1024;

  console.log('\n=========================================');
  console.log('       MEMORA CLI BENCHMARK SUMMARY      ');
  console.log('=========================================');
  console.log(`Exit Code:         ${exitCode}`);
  console.log(`Execution Duration: ${durationMs.toFixed(3)} ms (Target <50ms)`);
  console.log(`Heap Memory Delta:  ${memoryDeltaMb.toFixed(3)} MB`);
  console.log(`CPU User Time:      ${(endCpu.user / 1000).toFixed(3)} ms`);
  console.log(`CPU System Time:    ${(endCpu.system / 1000).toFixed(3)} ms`);
  console.log('=========================================\n');
}

runBenchmark().catch((err) => {
  console.error('Benchmark execution failed:', err);
  process.exit(1);
});
