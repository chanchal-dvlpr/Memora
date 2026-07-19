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
exports.runBackendIntegrationTests = runBackendIntegrationTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const events_1 = require("events");
const backendClient_1 = require("../../backendClient");
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
async function runBackendIntegrationTests() {
    console.log("-> Running BackendIntegrationTest...");
    // Test 1: Successful integrated connection returns list
    {
        mockHttpRequest((_options, req) => {
            const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
            req.emit('response-mock', res);
        });
        const client = new backendClient_1.BackendClient();
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
            }
            else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1' }]));
                req.emit('response-mock', res);
            }
        });
        const client = new backendClient_1.BackendClient();
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
        const client = new backendClient_1.BackendClient();
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
            }
            else {
                req.emit('error', new Error("Network Unavailable"));
            }
        });
        const client = new backendClient_1.BackendClient();
        // 1. Initial success fills cache
        await client.getProjects();
        // 2. Call fails and returns cache
        const projects = await client.getProjects();
        assert.strictEqual(projects.length, 1);
        assert.strictEqual(projects[0].name, 'Proj1');
    }
    console.log("-> BackendIntegrationTest passed.");
}
//# sourceMappingURL=BackendIntegrationTest.js.map