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
exports.runExtensionPerformanceTests = runExtensionPerformanceTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runExtensionPerformanceTests() {
    console.log("-> Running ExtensionPerformanceTest...");
    // Test 1: Cold activation latency check
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const start = process.hrtime.bigint();
        (0, extension_1.activate)(context);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;
        console.log(`   Cold activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000, "Activation latency must be < 1000 ms");
    }
    // Test 2: Warm activation latency check
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const start = process.hrtime.bigint();
        (0, extension_1.activate)(context);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;
        console.log(`   Warm activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 100, "Warm activation latency must be < 100 ms");
    }
    // Test 3: Repeated activations (10 runs)
    {
        const start = process.hrtime.bigint();
        for (let i = 0; i < 10; i++) {
            const context = {
                subscriptions: [],
                extensionUri: { scheme: 'file', authority: '', path: '/ext' }
            };
            (0, extension_1.activate)(context);
        }
        const end = process.hrtime.bigint();
        const avgMs = (Number(end - start) / 1e6) / 10;
        console.log(`   Avg repeated activation latency: ${avgMs.toFixed(2)} ms`);
        assert.ok(avgMs < 50, "Average repeated activation latency must be < 50 ms");
    }
    // Test 4: Activation without workspace folders
    {
        global.vscode.workspace.workspaceFolders = undefined;
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const start = process.hrtime.bigint();
        (0, extension_1.activate)(context);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;
        console.log(`   Without workspace activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000);
    }
    // Test 5: Activation with workspace folders
    {
        global.vscode.workspace.workspaceFolders = [{
                uri: { fsPath: '/path1' },
                name: 'Proj1'
            }];
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const start = process.hrtime.bigint();
        (0, extension_1.activate)(context);
        const end = process.hrtime.bigint();
        const durationMs = Number(end - start) / 1e6;
        console.log(`   With workspace activation latency: ${durationMs.toFixed(2)} ms`);
        assert.ok(durationMs < 1000);
        global.vscode.workspace.workspaceFolders = undefined;
    }
    console.log("-> ExtensionPerformanceTest passed.");
}
//# sourceMappingURL=ExtensionPerformanceTest.js.map