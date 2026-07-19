import { mockHttp } from '../mockVscode';
import * as assert from 'assert';
import * as http from 'http';
import { EventEmitter } from 'events';
import { BackendClient } from '../../backendClient';

class MockClientRequest extends EventEmitter {
    write(_data: string) {}
    end() {}
    destroy() {
        this.emit('close');
    }
}

class MockIncomingMessage extends EventEmitter {
    constructor(public readonly statusCode: number, public readonly data: string) {
        super();
    }
}

function mockHttpRequest(handler: (options: http.RequestOptions, req: MockClientRequest) => void) {
    mockHttp.requestOverride = (options: http.RequestOptions, callback: (res: any) => void) => {
        const req = new MockClientRequest();
        process.nextTick(() => {
            handler(options, req);
        });
        
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

export async function runOfflineRecoveryIntegrationTests() {
    console.log("-> Running OfflineRecoveryIntegrationTest...");

    // Test 1: Backend drops and restores. Client recovers cleanly
    {
        let requestCount = 0;
        mockHttpRequest((_options, req) => {
            requestCount++;
            if (requestCount === 1) {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
                req.emit('response-mock', res);
            } else if (requestCount >= 2 && requestCount <= 5) {
                req.emit('error', new Error("Connection Refused"));
            } else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1-Recovered' }]));
                req.emit('response-mock', res);
            }
        });

        const client = new BackendClient();

        const initial = await client.getProjects();
        assert.strictEqual(initial.length, 1);
        assert.strictEqual(initial[0].name, 'Proj1');

        const degraded = await client.getProjects();
        assert.strictEqual(degraded.length, 1, "Should fallback to cache");
        assert.strictEqual(degraded[0].name, 'Proj1');

        const recovered = await client.getProjects();
        assert.strictEqual(recovered.length, 1);
        assert.strictEqual(recovered[0].name, 'Proj1-Recovered', "Should recover live data from backend");
    }

    // Test 2: Offline cache synchronization after reconnect
    {
        let requestCount = 0;
        mockHttpRequest((_options, req) => {
            requestCount++;
            if (requestCount === 1) {
                // 1. Initial success returns Proj-Initial
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj-Initial' }]));
                req.emit('response-mock', res);
            } else if (requestCount === 2) {
                // 2. Reconnected success returns Proj-New
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj-New' }]));
                req.emit('response-mock', res);
            } else {
                // 3. Outage fails subsequent requests
                req.emit('error', new Error("Network Unavailable"));
            }
        });

        const client = new BackendClient();

        // Step 1: Initial load
        const v1 = await client.getProjects();
        assert.strictEqual(v1[0].name, 'Proj-Initial');

        // Step 2: Reconnection pulls new state, updating cache
        const v2 = await client.getProjects();
        assert.strictEqual(v2[0].name, 'Proj-New');

        // Step 3: Outage falls back to UPDATED cache
        const v3 = await client.getProjects();
        assert.strictEqual(v3[0].name, 'Proj-New', "Should return updated cached value after reconnect sync");
    }

    console.log("-> OfflineRecoveryIntegrationTest passed.");
}
