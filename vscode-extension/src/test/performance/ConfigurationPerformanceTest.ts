import { mockOnDidChangeConfiguration } from '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runConfigurationPerformanceTests() {
    console.log("-> Running ConfigurationPerformanceTest...");

    // Test 1: Configuration update propagation latency
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        const start = process.hrtime.bigint();
        mockOnDidChangeConfiguration.fire({
            affectsConfiguration: (section: string) => section === 'contextEngine'
        });
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   Configuration reload latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "Configuration reload latency must be < 100 ms");

        deactivate();
    }

    console.log("-> ConfigurationPerformanceTest passed.");
}
