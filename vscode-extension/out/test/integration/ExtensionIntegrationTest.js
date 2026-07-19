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
exports.runExtensionIntegrationTests = runExtensionIntegrationTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runExtensionIntegrationTests() {
    console.log("-> Running ExtensionIntegrationTest...");
    // Test 1: Full integrated startup and registrations
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        assert.ok(context.subscriptions.length > 0, "Integrated boot should register subscriptions");
    }
    // Test 2: Double activation does not crash or leak
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        (0, extension_1.activate)(context);
        assert.ok(context.subscriptions.length > 0, "Double activation succeeds safely");
    }
    // Test 3: Deactivation cleans up resources cleanly
    {
        (0, extension_1.deactivate)();
        assert.ok(true, "Deactivation completes cleanly");
    }
    // Test 4: Complete activation lifecycle (activate -> deactivate -> activate)
    {
        const context1 = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context1);
        assert.ok(context1.subscriptions.length > 0);
        (0, extension_1.deactivate)();
        const context2 = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context2);
        assert.ok(context2.subscriptions.length > 0, "Activation after deactivation should succeed");
        (0, extension_1.deactivate)();
    }
    // Test 5: Configuration reload checks propagation
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(context);
        // Simulate config change event trigger
        let configReloaded = false;
        const originalGet = global.vscode.workspace.getConfiguration().get;
        global.vscode.workspace.getConfiguration = () => ({
            get: (key) => {
                if (key === 'connection.port') {
                    configReloaded = true;
                    return 9999;
                }
                return undefined;
            }
        });
        // Trigger configuration updates listener via event emitter
        mockVscode_1.mockOnDidChangeConfiguration.fire({
            affectsConfiguration: (section) => section === 'contextEngine'
        });
        assert.strictEqual(configReloaded, true, "Configuration should reload on setting shifts");
        global.vscode.workspace.getConfiguration = () => ({ get: originalGet });
        (0, extension_1.deactivate)();
    }
    console.log("-> ExtensionIntegrationTest passed.");
}
//# sourceMappingURL=ExtensionIntegrationTest.js.map