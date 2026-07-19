import '../mockVscode';
import * as assert from 'assert';
import { activate } from '../../extension';

export async function runExtensionPerformanceTests() {
    console.log("-> Running ExtensionPerformanceTest...");

    // Test 1: Cold activation latency check
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        const start = process.hrtime.bigint();
        activate(context as any);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;

        console.log(`   Cold activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000, "Activation latency must be < 1000 ms");
    }

    // Test 2: Warm activation latency check
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        const start = process.hrtime.bigint();
        activate(context as any);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;

        console.log(`   Warm activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 100, "Warm activation latency must be < 100 ms");
    }

    // Test 3: Repeated activations (10 runs)
    {
        const start = process.hrtime.bigint();
        for (let i = 0; i < 10; i++) {
            const context = {
                subscriptions: [] as any[],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            activate(context as any);
        }
        const end = process.hrtime.bigint();
        const avgMs = (Number(end - start) / 1e6) / 10;

        console.log(`   Avg repeated activation latency: ${avgMs.toFixed(2)} ms`);
        assert.ok(avgMs < 50, "Average repeated activation latency must be < 50 ms");
    }

    // Test 4: Activation without workspace folders
    {
        (global as any).vscode.workspace.workspaceFolders = undefined;
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        const start = process.hrtime.bigint();
        activate(context as any);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;

        console.log(`   Without workspace activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000);
    }

    // Test 5: Activation with workspace folders
    {
        (global as any).vscode.workspace.workspaceFolders = [{
            uri: { fsPath: '/path1' },
            name: 'Proj1'
        }];
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        const start = process.hrtime.bigint();
        activate(context as any);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;

        console.log(`   With workspace activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000);
        (global as any).vscode.workspace.workspaceFolders = undefined;
    }

    console.log("-> ExtensionPerformanceTest passed.");
}
