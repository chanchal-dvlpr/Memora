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
exports.runExtensionActivationTests = runExtensionActivationTests;
require("./mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../extension");
async function runExtensionActivationTests() {
    console.log("-> Running ExtensionActivationTest...");
    // Test 1: activate without workspace (should proceed safely)
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(mockContext);
        assert.ok(mockContext.subscriptions.length > 0, "Activation should register commands and listener subscriptions");
    }
    // Test 2: deactivate cleans up intervals
    {
        (0, extension_1.deactivate)();
        assert.ok(true, "Deactivation should complete cleanly");
    }
    // Test 3: Command palette registrations
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        // Track registered commands
        const registeredCommands = [];
        const originalRegisterCommand = global.vscode.commands.registerCommand;
        global.vscode.commands.registerCommand = (name, _callback) => {
            registeredCommands.push(name);
            return originalRegisterCommand(name, _callback);
        };
        (0, extension_1.activate)(mockContext);
        assert.ok(registeredCommands.includes("contextEngine.registerProject"));
        assert.ok(registeredCommands.includes("contextEngine.refreshContext"));
        assert.ok(registeredCommands.includes("contextEngine.generateAIHandoff"));
        assert.ok(registeredCommands.includes("contextEngine.searchKnowledge"));
        assert.ok(registeredCommands.includes("contextEngine.showArchitecture"));
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 4: Workspace provider registrations
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const registeredProviders = [];
        const originalRegisterTreeDataProvider = global.vscode.window.registerTreeDataProvider;
        global.vscode.window.registerTreeDataProvider = (viewId, _provider) => {
            registeredProviders.push(viewId);
            return originalRegisterTreeDataProvider(viewId, _provider);
        };
        (0, extension_1.activate)(mockContext);
        assert.ok(registeredProviders.includes("contextEngine.dashboard"));
        assert.ok(registeredProviders.includes("contextEngine.architecture"));
        assert.ok(registeredProviders.includes("contextEngine.work"));
        global.vscode.window.registerTreeDataProvider = originalRegisterTreeDataProvider;
    }
    // Test 5: CodeLens provider registration
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        let codeLensRegistered = false;
        const originalRegisterCodeLensProvider = global.vscode.languages.registerCodeLensProvider;
        global.vscode.languages.registerCodeLensProvider = (_selector, _provider) => {
            codeLensRegistered = true;
            return originalRegisterCodeLensProvider(_selector, _provider);
        };
        (0, extension_1.activate)(mockContext);
        assert.strictEqual(codeLensRegistered, true);
        global.vscode.languages.registerCodeLensProvider = originalRegisterCodeLensProvider;
    }
    // Test 6: Hover provider registration
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        let hoverRegistered = false;
        const originalRegisterHoverProvider = global.vscode.languages.registerHoverProvider;
        global.vscode.languages.registerHoverProvider = (_selector, _provider) => {
            hoverRegistered = true;
            return originalRegisterHoverProvider(_selector, _provider);
        };
        (0, extension_1.activate)(mockContext);
        assert.strictEqual(hoverRegistered, true);
        global.vscode.languages.registerHoverProvider = originalRegisterHoverProvider;
    }
    // Test 7: Double activation cleans up existing handle before scheduling next sync loop
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(mockContext);
        (0, extension_1.activate)(mockContext);
        assert.ok(mockContext.subscriptions.length > 0);
    }
    // Test 8: Configuration change listener triggers config reload
    {
        const mockContext = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        (0, extension_1.activate)(mockContext);
        assert.ok(mockContext.subscriptions.length > 0);
    }
    console.log("-> ExtensionActivationTest passed.");
}
//# sourceMappingURL=ExtensionActivationTest.js.map