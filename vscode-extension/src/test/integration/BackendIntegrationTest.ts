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

export async function runBackendIntegrationTests() {
    console.log("-> Running BackendIntegrationTest...");

    // Test 1: Successful integrated connection returns list
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
            req.emit('response-mock', res);
        });

        const client = new BackendClient();
        const projects = await client.getProjects();
        assert.strictEqual(projects.length, 1, "Should parse projects array correctly");
        assert.strictEqual(projects[0].name, 'Proj1');
    }

    // Test 2: Timeout triggers retry loop and restores
    {
        let attempts = 0;
        mockHttpRequest((_options, req) => {
            attempts++;
            if (attempts <= 2) {
                req.emit('error', new Error("Timeout"));
            } else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1' }]));
                req.emit('response-mock', res);
            }
        });

        const client = new BackendClient();
        const projects = await client.getProjects();
        assert.strictEqual(projects.length, 1);
        assert.strictEqual(attempts, 3, "Should succeed on 3rd attempt after 2 retries");
    }

    // Test 3: Multiple concurrent requests process in parallel
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1' }]));
            req.emit('response-mock', res);
        });

        const client = new BackendClient();
        const [res1, res2] = await Promise.all([client.getProjects(), client.getProjects()]);
        assert.strictEqual(res1.length, 1);
        assert.strictEqual(res2.length, 1);
    }

    // Test 4: Cache fallback when backend is unavailable
    {
        let attempts = 0;
        mockHttpRequest((_options, req) => {
            attempts++;
            if (attempts === 1) {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
                req.emit('response-mock', res);
            } else {
                req.emit('error', new Error("Network Unavailable"));
            }
        });

        const client = new BackendClient();
        // 1. Initial success fills cache
        await client.getProjects();

        // 2. Call fails and returns cache
        const projects = await client.getProjects();
        assert.strictEqual(projects.length, 1);
        assert.strictEqual(projects[0].name, 'Proj1');
    }

    console.log("-> BackendIntegrationTest passed.");
}
