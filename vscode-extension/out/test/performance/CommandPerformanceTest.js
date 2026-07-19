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
exports.runCommandPerformanceTests = runCommandPerformanceTests;
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
async function runCommandPerformanceTests() {
    console.log("-> Running CommandPerformanceTest...");
    const registeredCommands = new Map();
    const originalRegisterCommand = global.vscode.commands.registerCommand;
    global.vscode.commands.registerCommand = (name, callback) => {
        registeredCommands.set(name, callback);
        return originalRegisterCommand(name, callback);
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
    global.vscode.commands.registerCommand = originalRegisterCommand;
    console.log("-> CommandPerformanceTest passed.");
}
//# sourceMappingURL=CommandPerformanceTest.js.map