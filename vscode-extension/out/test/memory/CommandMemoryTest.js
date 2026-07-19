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
exports.runCommandMemoryTests = runCommandMemoryTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runCommandMemoryTests() {
    console.log("-> Running CommandMemoryTest...");
    // Setup command triggers
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
    global.vscode.commands.registerCommand = originalRegisterCommand;
    (0, extension_1.deactivate)();
    console.log("-> CommandMemoryTest passed.");
}
//# sourceMappingURL=CommandMemoryTest.js.map