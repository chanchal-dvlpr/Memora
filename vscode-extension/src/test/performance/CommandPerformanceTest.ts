import { mockHttp } from '../mockVscode';
import * as assert from 'assert';
import * as http from 'http';
import { EventEmitter } from 'events';
import { activate } from '../../extension';

class MockClientRequest extends EventEmitter {
    write(_data: string) {}
    end() {}
    destroy() { this.emit('close'); }
}

class MockIncomingMessage extends EventEmitter {
    constructor(public readonly statusCode: number, public readonly data: string) {
        super();
    }
}

function mockHttpRequest(handler: (options: http.RequestOptions, req: MockClientRequest) => void) {
    mockHttp.requestOverride = (options: http.RequestOptions, callback: (res: any) => void) => {
        const req = new MockClientRequest();
        process.nextTick(() => { handler(options, req); });
        req.on('response-mock', (res: MockIncomingMessage) => {
            callback(res);
            process.nextTick(() => {
                res.emit('data', Buffer.from(res.data));
                res.emit('end');
            });
        });
        return req as any;
    };
}

export async function runCommandPerformanceTests() {
    console.log("-> Running CommandPerformanceTest...");

    const registeredCommands = new Map<string, any>();
    const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
    (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
        registeredCommands.set(name, callback);
        return originalRegisterCommand(name, callback);
    };

    const context = {
        subscriptions: [] as any[],
        extensionUri: { scheme: 'file', authority: '', path: '/ext' }
    };
    activate(context as any);

    // Mock HTTP responses to return quickly
    mockHttpRequest((_options, req) => {
        const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
        req.emit('response-mock', res);
    });

    // Test 1: refreshContext latency (1, 10, and 100 runs)
    {
        const refreshCallback = registeredCommands.get("contextEngine.refreshContext");
        
        // Single Execution
        const start1 = process.hrtime.bigint();
        await refreshCallback();
        const end1 = process.hrtime.bigint();
        const duration1 = Number(end1 - start1) / 1e6;
        console.log(`   refreshContext single execution: ${duration1.toFixed(2)} ms`);
        assert.ok(duration1 < 200, "Command execution must be < 200 ms");

        // 10 Executions
        const start10 = process.hrtime.bigint();
        for (let i = 0; i < 10; i++) {
            await refreshCallback();
        }
        const end10 = process.hrtime.bigint();
        const avg10 = (Number(end10 - start10) / 1e6) / 10;
        console.log(`   refreshContext avg latency (10 runs): ${avg10.toFixed(2)} ms`);
        assert.ok(avg10 < 100);

        // 100 Executions (Load Test)
        const start100 = process.hrtime.bigint();
        for (let i = 0; i < 100; i++) {
            await refreshCallback();
        }
        const end100 = process.hrtime.bigint();
        const avg100 = (Number(end100 - start100) / 1e6) / 100;
        console.log(`   refreshContext avg latency (100 runs): ${avg100.toFixed(2)} ms`);
        assert.ok(avg100 < 50);
    }

    // Clean up
    (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    console.log("-> CommandPerformanceTest passed.");
}
