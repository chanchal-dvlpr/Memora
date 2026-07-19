import '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runCommandMemoryTests() {
    console.log("-> Running CommandMemoryTest...");

    // Setup command triggers
    const registeredCommands = new Map<string, any>();
    const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
    (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
        registeredCommands.set(name, callback);
        return { dispose: () => { registeredCommands.delete(name); } };
    };

    const context = {
        subscriptions: [] as any[],
        extensionUri: { scheme: 'file', authority: '', path: '/ext' }
    };
    activate(context as any);

    const refreshCallback = registeredCommands.get("contextEngine.refreshContext");
    const handoffCallback = registeredCommands.get("contextEngine.generateAIHandoff");

    // Test 1: 100 executions
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            await refreshCallback();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 executions: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 2: 500 executions
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            await refreshCallback();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 executions: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 3: 1000 executions (Load stability check)
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 1000; i++) {
            await refreshCallback();
            await handoffCallback();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 1000 executions: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 10, "1000 command executions should remain stable");
    }

    // Restore original command registration method
    (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    deactivate();

    console.log("-> CommandMemoryTest passed.");
}
