import { mockOnDidChangeConfiguration } from '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runConfigurationMemoryTests() {
    console.log("-> Running ConfigurationMemoryTest...");

    // Test 1: 100 configuration change updates
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            mockOnDidChangeConfiguration.fire({
                affectsConfiguration: (section: string) => section === 'contextEngine'
            });
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 config reloads: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15);

        deactivate();
    }

    // Test 2: 500 configuration change updates (Load check)
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            mockOnDidChangeConfiguration.fire({
                affectsConfiguration: (section: string) => section === 'contextEngine'
            });
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 config reloads: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15, "500 config changes must run without listener leaks");

        deactivate();
    }

    console.log("-> ConfigurationMemoryTest passed.");
}
