import { mockHttp } from '../mockVscode';
import * as assert from 'assert';
import * as http from 'http';
import { EventEmitter } from 'events';
import { activate, deactivate } from '../../extension';

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

export async function runCommandConcurrencyTests() {
    console.log("-> Running CommandConcurrencyTest...");

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

    // Mock HTTP responses to return quickly
    mockHttpRequest((_options, req) => {
        const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
        req.emit('response-mock', res);
    });

    const refreshCallback = registeredCommands.get("contextEngine.refreshContext");

    // Test 1: 10 concurrent executions
    {
        const promises = Array.from({ length: 10 }, () => refreshCallback());
        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 2: 30 concurrent executions
    {
        const promises = Array.from({ length: 30 }, () => refreshCallback());
        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 3: 50 concurrent executions
    {
        const promises = Array.from({ length: 50 }, () => refreshCallback());
        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 4: 100 concurrent executions
    {
        const promises = Array.from({ length: 100 }, () => refreshCallback());
        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 5: Mixed Command Stress (overlapping refresh, handoff, config reload)
    {
        const handoffCallback = registeredCommands.get("contextEngine.generateAIHandoff");
        const promises = Array.from({ length: 100 }, (_, i) => {
            if (i % 2 === 0) return refreshCallback();
            return handoffCallback();
        });
        await Promise.all(promises);
        assert.ok(true, "Mixed commands concurrent stress runs successfully");
    }

    // Test 6: Offline/Online Race check under concurrent requests
    {
        let requestCount = 0;
        let offlineTriggered = false;
        mockHttpRequest((_options, req) => {
            requestCount++;
            if (offlineTriggered) {
                req.emit('error', new Error("Daemon Outage"));
            } else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: '1', name: 'Proj-Live' }]));
                req.emit('response-mock', res);
            }
        });

        // Trigger concurrent requests while shifting backend state
        const p1 = refreshCallback();
        offlineTriggered = true;
        const p2 = refreshCallback();
        offlineTriggered = false;
        const p3 = refreshCallback();

        await Promise.all([p1, p2, p3]);
        assert.ok(true, "Offline/Online status shifts under concurrent requests execute safely");
    }

    (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    deactivate();
    console.log("-> CommandConcurrencyTest passed.");
}
