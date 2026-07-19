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
exports.runOfflineRecoveryIntegrationTests = runOfflineRecoveryIntegrationTests;
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
async function runOfflineRecoveryIntegrationTests() {
    console.log("-> Running OfflineRecoveryIntegrationTest...");
    // Test 1: Backend drops and restores. Client recovers cleanly
    {
        let requestCount = 0;
        mockHttpRequest((_options, req) => {
            requestCount++;
            if (requestCount === 1) {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1' }]));
                req.emit('response-mock', res);
            }
            else if (requestCount >= 2 && requestCount <= 5) {
                req.emit('error', new Error("Connection Refused"));
            }
            else {
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1-Recovered' }]));
                req.emit('response-mock', res);
            }
        });
        const client = new backendClient_1.BackendClient();
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
            }
            else if (requestCount === 2) {
                // 2. Reconnected success returns Proj-New
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj-New' }]));
                req.emit('response-mock', res);
            }
            else {
                // 3. Outage fails subsequent requests
                req.emit('error', new Error("Network Unavailable"));
            }
        });
        const client = new backendClient_1.BackendClient();
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
//# sourceMappingURL=OfflineRecoveryIntegrationTest.js.map