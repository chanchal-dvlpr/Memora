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
exports.runConfigurationMemoryTests = runConfigurationMemoryTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runConfigurationMemoryTests() {
    console.log("-> Running ConfigurationMemoryTest...");
    // Test 1: 100 configuration change updates
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            mockVscode_1.mockOnDidChangeConfiguration.fire({
                affectsConfiguration: (section) => section === 'contextEngine'
            });
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 config reloads: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15);
        (0, extension_1.deactivate)();
    }
    // Test 2: 500 configuration change updates (Load check)
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            mockVscode_1.mockOnDidChangeConfiguration.fire({
                affectsConfiguration: (section) => section === 'contextEngine'
            });
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 config reloads: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 15, "500 config changes must run without listener leaks");
        (0, extension_1.deactivate)();
    }
    console.log("-> ConfigurationMemoryTest passed.");
}
//# sourceMappingURL=ConfigurationMemoryTest.js.map