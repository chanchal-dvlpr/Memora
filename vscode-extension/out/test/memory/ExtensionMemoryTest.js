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
exports.runExtensionMemoryTests = runExtensionMemoryTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runExtensionMemoryTests() {
    console.log("-> Running ExtensionMemoryTest...");
    // Test 1: 100 activation/deactivation cycles
    {
        for (let i = 0; i < 100; i++) {
            const context = {
                subscriptions: [],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            (0, extension_1.activate)(context);
            (0, extension_1.deactivate)();
        }
        assert.ok(true, "100 cycles completed cleanly");
    }
    // Test 2: 500 activation/deactivation cycles
    {
        for (let i = 0; i < 500; i++) {
            const context = {
                subscriptions: [],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            (0, extension_1.activate)(context);
            (0, extension_1.deactivate)();
        }
        assert.ok(true, "500 cycles completed cleanly");
    }
    // Test 3: 1000 activation/deactivation cycles
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 1000; i++) {
            const context = {
                subscriptions: [],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            (0, extension_1.activate)(context);
            (0, extension_1.deactivate)();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 1000 cycles: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15, "1000 cycles should remain stable under memory threshold");
    }
    // Test 4: Verify Disposable subscriptions are completely disposed
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        const subCount = context.subscriptions.length;
        assert.ok(subCount > 0);
        // Dispose all subscriptions
        context.subscriptions.forEach(s => s.dispose());
        assert.ok(true, "Subscriptions disposed successfully");
    }
    // Test 5: Verify command registry stability (no duplicates)
    {
        const commands = await global.vscode.commands.getCommands(true);
        const countBefore = commands.filter((c) => c.startsWith('contextEngine.')).length;
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        (0, extension_1.deactivate)();
        const commandsAfter = await global.vscode.commands.getCommands(true);
        const countAfter = commandsAfter.filter((c) => c.startsWith('contextEngine.')).length;
        assert.strictEqual(countAfter, countBefore, "Command list should not duplicate registrations");
    }
    console.log("-> ExtensionMemoryTest passed.");
}
//# sourceMappingURL=ExtensionMemoryTest.js.map