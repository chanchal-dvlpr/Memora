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
exports.runConfigurationPerformanceTests = runConfigurationPerformanceTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runConfigurationPerformanceTests() {
    console.log("-> Running ConfigurationPerformanceTest...");
    // Test 1: Configuration update propagation latency
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        const start = process.hrtime.bigint();
        mockVscode_1.mockOnDidChangeConfiguration.fire({
            affectsConfiguration: (section) => section === 'contextEngine'
        });
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   Configuration reload latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "Configuration reload latency must be < 100 ms");
        (0, extension_1.deactivate)();
    }
    console.log("-> ConfigurationPerformanceTest passed.");
}
//# sourceMappingURL=ConfigurationPerformanceTest.js.map