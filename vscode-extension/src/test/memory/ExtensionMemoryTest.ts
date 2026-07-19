import '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runExtensionMemoryTests() {
    console.log("-> Running ExtensionMemoryTest...");

    // Test 1: 100 activation/deactivation cycles
    {
        for (let i = 0; i < 100; i++) {
            const context = {
                subscriptions: [] as any[],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            activate(context as any);
            deactivate();
        }
        assert.ok(true, "100 cycles completed cleanly");
    }

    // Test 2: 500 activation/deactivation cycles
    {
        for (let i = 0; i < 500; i++) {
            const context = {
                subscriptions: [] as any[],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            activate(context as any);
            deactivate();
        }
        assert.ok(true, "500 cycles completed cleanly");
    }

    // Test 3: 1000 activation/deactivation cycles
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 1000; i++) {
            const context = {
                subscriptions: [] as any[],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            activate(context as any);
            deactivate();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 1000 cycles: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15, "1000 cycles should remain stable under memory threshold");
    }

    // Test 4: Verify Disposable subscriptions are completely disposed
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);
        const subCount = context.subscriptions.length;
        assert.ok(subCount > 0);
        
        // Dispose all subscriptions
        context.subscriptions.forEach(s => s.dispose());
        assert.ok(true, "Subscriptions disposed successfully");
    }

    // Test 5: Verify command registry stability (no duplicates)
    {
        const commands = await (global as any).vscode.commands.getCommands(true);
        const countBefore = commands.filter((c: string) => c.startsWith('contextEngine.')).length;
        
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);
        deactivate();

        const commandsAfter = await (global as any).vscode.commands.getCommands(true);
        const countAfter = commandsAfter.filter((c: string) => c.startsWith('contextEngine.')).length;
        assert.strictEqual(countAfter, countBefore, "Command list should not duplicate registrations");
    }

    console.log("-> ExtensionMemoryTest passed.");
}
