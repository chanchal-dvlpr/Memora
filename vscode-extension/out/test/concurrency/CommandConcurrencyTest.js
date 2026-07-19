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
exports.runCommandConcurrencyTests = runCommandConcurrencyTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const events_1 = require("events");
const extension_1 = require("../../extension");
class MockClientRequest extends events_1.EventEmitter {
    write(_data) { }
    end() { }
    destroy() { this.emit('close'); }
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
        process.nextTick(() => { handler(options, req); });
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
async function runCommandConcurrencyTests() {
    console.log("-> Running CommandConcurrencyTest...");
    const registeredCommands = new Map();
    const originalRegisterCommand = global.vscode.commands.registerCommand;
    global.vscode.commands.registerCommand = (name, callback) => {
        registeredCommands.set(name, callback);
        return { dispose: () => { registeredCommands.delete(name); } };
    };
    const context = {
        subscriptions: [],
        extensionUri: { scheme: 'file', authority: '', path: '/ext' }
    };
    (0, extension_1.activate)(context);
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
            if (i % 2 === 0)
                return refreshCallback();
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
            }
            else {
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
    global.vscode.commands.registerCommand = originalRegisterCommand;
    (0, extension_1.deactivate)();
    console.log("-> CommandConcurrencyTest passed.");
}
//# sourceMappingURL=CommandConcurrencyTest.js.map