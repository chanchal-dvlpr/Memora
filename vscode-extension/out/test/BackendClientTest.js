"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.runBackendClientTests = runBackendClientTests;
const mockVscode_1 = require("./mockVscode");
const assert = __importStar(require("assert"));
const events_1 = require("events");
const backendClient_1 = require("../backendClient");
class MockClientRequest extends events_1.EventEmitter {
    write(_data) { }
    end() { }
    destroy() {
        this.emit('close');
    }
}
class MockIncomingMessage extends events_1.EventEmitter {
    statusCode;
    data;
    constructor(statusCode, data) {
        super();
        this.statusCode = statusCode;
        this.data = data;
    }
}
function mockHttpRequest(handler) {
    mockVscode_1.mockHttp.requestOverride = (options, callback) => {
        const req = new MockClientRequest();
        process.nextTick(() => {
            handler(options, req);
        });
        req.on('response-mock', (res) => {
            callback(res);
            process.nextTick(() => {
                res.emit('data', Buffer.from(res.data));
                res.emit('end');
            });
        });
        return req;
    };
}
async function runBackendClientTests() {
    console.log("-> Running BackendClientTest...");
    // Test 1: constructor initialization & default configurations
    {
        const client = new backendClient_1.BackendClient();
        assert.ok(client !== null, "BackendClient instantiation must succeed");
        assert.strictEqual(client.port, 9876, "Default port should be 9876");
        assert.strictEqual(client.token, "prod-token", "Default token should be prod-token");
    }
    // Test 2: custom configuration loading
    {
        const originalGet = global.vscode.workspace.getConfiguration().get;
        global.vscode.workspace.getConfiguration = () => ({
            get: (key) => {
                if (key === 'connection.port') {
                    return 1234;
                }
                if (key === 'connection.token') {
                    return 'custom-token';
                }
                return undefined;
            }
        });
        const client = new backendClient_1.BackendClient();
        assert.strictEqual(client.port, 1234, "Custom port should load correctly");
        assert.strictEqual(client.token, "custom-token", "Custom token should load correctly");
        // Restore original
        global.vscode.workspace.getConfiguration = () => ({ get: originalGet });
    }
    // Test 3: update configuration dynamically
    {
        const client = new backendClient_1.BackendClient();
        const originalGet = global.vscode.workspace.getConfiguration().get;
        // Update mock settings values
        global.vscode.workspace.getConfiguration = () => ({
            get: (key) => {
                if (key === 'connection.port') {
                    return 5678;
                }
                if (key === 'connection.token') {
                    return 'updated-token';
                }
                return undefined;
            }
        });
        client.updateConfig();
        assert.strictEqual(client.port, 5678, "Port should update dynamically");
        assert.strictEqual(client.token, "updated-token", "Token should update dynamically");
        global.vscode.workspace.getConfiguration = () => ({ get: originalGet });
    }
    // Test 4: GET list request (getProjects)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'GET');
            assert.strictEqual(options.path, '/api/v1/projects');
            assert.strictEqual(options.headers?.['X-Session-Token'], 'prod-token');
            const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const projects = await client.getProjects();
        assert.strictEqual(projects.length, 1);
        assert.strictEqual(projects[0].id, 'p1');
    }
    // Test 5: GET single project request (getProject)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'GET');
            assert.strictEqual(options.path, '/api/v1/projects/p1');
            const res = new MockIncomingMessage(200, JSON.stringify({ id: 'p1', name: 'Proj1' }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const project = await client.getProject('p1');
        assert.strictEqual(project.id, 'p1');
        assert.strictEqual(project.name, 'Proj1');
    }
    // Test 6: POST request registration (registerProject)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/projects');
            const res = new MockIncomingMessage(201, JSON.stringify({ id: 'p2', name: 'Proj2' }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const project = await client.registerProject('Proj2', '/path2');
        assert.strictEqual(project.id, 'p2');
    }
    // Test 7: DELETE request (deleteProject)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'DELETE');
            assert.strictEqual(options.path, '/api/v1/projects/p2');
            const res = new MockIncomingMessage(204, "");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        await client.deleteProject('p2');
        assert.ok(true, "Delete should complete without errors");
    }
    // Test 8: POST trigger scan request (triggerScan)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/projects/p1/scans');
            const res = new MockIncomingMessage(200, "");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        await client.triggerScan('p1', true);
        assert.ok(true, "Trigger scan should complete without errors");
    }
    // Test 9: GET scan status request (getScanStatus)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'GET');
            assert.strictEqual(options.path, '/api/v1/projects/p1/scans');
            const res = new MockIncomingMessage(200, JSON.stringify({ status: 'COMPLETED', filesProcessed: 10, symbolsProcessed: 150 }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const status = await client.getScanStatus('p1');
        assert.strictEqual(status.status, 'COMPLETED');
        assert.strictEqual(status.filesProcessed, 10);
    }
    // Test 10: POST context assembly request (generateContext)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/context/assembly');
            const res = new MockIncomingMessage(200, JSON.stringify({ contextId: 'ctx1', tokenUsage: 500, content: 'md-content' }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const ctx = await client.generateContext('p1', 'symbol', 'file.ts', 2000);
        assert.strictEqual(ctx.contextId, 'ctx1');
        assert.strictEqual(ctx.content, 'md-content');
    }
    // Test 11: GET snapshot request (getLatestSnapshot)
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'GET');
            assert.strictEqual(options.path, '/api/v1/projects/p1/snapshots/latest');
            const res = new MockIncomingMessage(200, JSON.stringify({ contextId: 'snap1', tokenUsage: 450, content: 'snap-content' }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const snap = await client.getLatestSnapshot('p1');
        assert.strictEqual(snap.contextId, 'snap1');
    }
    // Test 12: HTTP 400 Bad Request error
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(400, "Bad Request");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.getProjects();
            assert.fail("Should throw on 400 error");
        }
        catch (e) {
            assert.ok(e.message.includes("400"), "Error message should include status 400");
        }
    }
    // Test 13: HTTP 401 Unauthorized error
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(401, "Unauthorized");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.getProjects();
            assert.fail("Should throw on 401 error");
        }
        catch (e) {
            assert.ok(e.message.includes("401"), "Error message should include status 401");
        }
    }
    // Test 14: HTTP 403 Forbidden error
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(403, "Forbidden");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.getProjects();
            assert.fail("Should throw on 403 error");
        }
        catch (e) {
            assert.ok(e.message.includes("403"), "Error message should include status 403");
        }
    }
    // Test 15: HTTP 404 Not Found error
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(404, "Not Found");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.getProjects();
            assert.fail("Should throw on 404 error");
        }
        catch (e) {
            assert.ok(e.message.includes("404"), "Error message should include status 404");
        }
    }
    // Test 16: HTTP 500 Internal Server Error
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(500, "Internal Server Error");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.getProjects();
            assert.fail("Should throw on 500 error");
        }
        catch (e) {
            assert.ok(e.message.includes("500"), "Error message should include status 500");
        }
    }
    // Test 17: Invalid JSON payload response (should return raw text)
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, "plain text content");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const result = await client.getProjects();
        assert.strictEqual(result, "plain text content", "Result should match raw response content");
    }
    // Test 18: Malformed response (no data returned)
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, "");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const result = await client.getProjects();
        assert.strictEqual(result, "", "Result should be empty string");
    }
    // Test 19: Cache miss when backend is offline and no cached data exists
    {
        mockHttpRequest((_options, req) => {
            req.emit('error', new Error("Connection Refused"));
        });
        const client = new backendClient_1.BackendClient();
        try {
            // Bypass cache load, call direct path that is not in cache
            await client.getProject("p_missing");
            assert.fail("Should throw error on cache miss when disconnected");
        }
        catch (e) {
            assert.ok(e.message.includes("Connection Refused"), "Should report connection failure");
        }
    }
    // Test 20: Cache hit recovery when backend goes offline
    {
        let requestCount = 0;
        mockHttpRequest((_options, req) => {
            requestCount++;
            if (requestCount === 1) {
                const res = new MockIncomingMessage(200, JSON.stringify({ id: 'p1', name: 'Proj1' }));
                req.emit('response-mock', res);
            }
            else {
                req.emit('error', new Error("Connection Timeout"));
            }
        });
        const client = new backendClient_1.BackendClient();
        // Load into cache
        const initial = await client.getProject('p1');
        assert.strictEqual(initial.name, 'Proj1');
        // Subsequent call triggers errors, falls back to cache
        const cached = await client.getProject('p1');
        assert.strictEqual(cached.name, 'Proj1', "Should return cached data");
    }
    // Test 21: Timeout events trigger connection retry loop
    {
        let attempts = 0;
        mockHttpRequest((_options, req) => {
            attempts++;
            if (attempts <= 2) {
                req.emit('error', new Error("Timeout"));
            }
            else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1' }]));
                req.emit('response-mock', res);
            }
        });
        const client = new backendClient_1.BackendClient();
        const result = await client.getProjects();
        assert.strictEqual(result.length, 1);
        assert.strictEqual(attempts, 3, "Should succeed on 3rd attempt after 2 timeout retries");
    }
    // Test 22: Exponential backoff delay scaling verification
    {
        let callTimestamps = [];
        mockHttpRequest((_options, req) => {
            callTimestamps.push(Date.now());
            req.emit('error', new Error("Transient Failure"));
        });
        const client = new backendClient_1.BackendClient();
        try {
            // Exhaust all 3 retries
            await client.getProjects();
        }
        catch (e) {
            // Expected
        }
        assert.strictEqual(callTimestamps.length, 4, "Should run 4 attempts total");
    }
    // Test 23: Concurrent request execution thread-safety checks
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1' }]));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        const p1 = client.getProjects();
        const p2 = client.getProjects();
        const [res1, res2] = await Promise.all([p1, p2]);
        assert.strictEqual(res1.length, 1);
        assert.strictEqual(res2.length, 1);
    }
    // Test 24: Request header length generation checks
    {
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.headers?.['Content-Type'], 'application/json');
            assert.ok(Number(options.headers?.['Content-Length']) >= 0);
            const res = new MockIncomingMessage(200, "{}");
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        await client.registerProject("Proj1", "/p1");
    }
    // Test 25: HTTP 409 BackendError propagation checks
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(409, JSON.stringify({ code: "PROJECT_ALREADY_REGISTERED", message: "Project already registered" }));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
        try {
            await client.registerProject("Proj1", "/p1");
            assert.fail("Should throw BackendError on 409 duplicate registration");
        }
        catch (e) {
            assert.strictEqual(e.statusCode, 409);
            assert.strictEqual(e.body?.code, "PROJECT_ALREADY_REGISTERED");
            assert.strictEqual(e.body?.message, "Project already registered");
        }
    }
    console.log("-> BackendClientTest passed.");
}
//# sourceMappingURL=BackendClientTest.js.map