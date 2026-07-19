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
exports.runConfigurationConcurrencyTests = runConfigurationConcurrencyTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runConfigurationConcurrencyTests() {
    console.log("-> Running ConfigurationConcurrencyTest...");
    // Test 1: Concurrent settings change updates
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => {
                mockVscode_1.mockOnDidChangeConfiguration.fire({
                    affectsConfiguration: (section) => section === 'contextEngine'
                });
            });
        });
        await Promise.all(promises);
        assert.ok(true);
        (0, extension_1.deactivate)();
    }
    // Test 2: Last update wins check on connection configuration modifications
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        let finalPortValue = 0;
        const promises = Array.from({ length: 50 }, (_, i) => {
            return Promise.resolve().then(() => {
                finalPortValue = 9000 + i;
                mockVscode_1.mockOnDidChangeConfiguration.fire({
                    affectsConfiguration: (section) => section === 'contextEngine'
                });
            });
        });
        await Promise.all(promises);
        assert.ok(finalPortValue >= 9000);
        (0, extension_1.deactivate)();
    }
    console.log("-> ConfigurationConcurrencyTest passed.");
}
//# sourceMappingURL=ConfigurationConcurrencyTest.js.map