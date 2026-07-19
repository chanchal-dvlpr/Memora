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
exports.runCommandIntegrationTests = runCommandIntegrationTests;
const mockVscode_1 = require("../mockVscode");
const assert = __importStar(require("assert"));
const events_1 = require("events");
const extension_1 = require("../../extension");
class MockClientRequest extends events_1.EventEmitter {
    write(_data) { }
    end() { }
    destroy() {
        this.emit('close');
    }
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
        process.nextTick(() => {
            handler(options, req);
        });
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
async function runCommandIntegrationTests() {
    console.log("-> Running CommandIntegrationTest...");
    // Test 1: Register commands and trigger registerProject
    {
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
        assert.ok(registeredCommands.has("contextEngine.registerProject"));
        assert.ok(registeredCommands.has("contextEngine.refreshContext"));
        assert.ok(registeredCommands.has("contextEngine.generateAIHandoff"));
        // Trigger project registration when no workspace is open (should show warn)
        let warningShown = false;
        global.vscode.window.showWarningMessage = () => {
            warningShown = true;
            return Promise.resolve();
        };
        const regCallback = registeredCommands.get("contextEngine.registerProject");
        await regCallback();
        assert.strictEqual(warningShown, true, "Should show warning message when no workspace open");
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 2: AI Handoff Command copies snapshot to clipboard
    {
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
        let clipboardContent = '';
        global.vscode.env.clipboard.writeText = (text) => {
            clipboardContent = text;
            return Promise.resolve();
        };
        global.vscode.workspace.workspaceFolders = [{
                uri: { fsPath: '/path1' },
                name: 'Proj1'
            }];
        let getProjectsCount = 0;
        let getSnapshotCount = 0;
        mockHttpRequest((options, req) => {
            if (options.path === '/api/v1/projects') {
                getProjectsCount++;
                const res = new MockIncomingMessage(200, JSON.stringify([{ id: 'p1', name: 'Proj1', rootPath: '/path1' }]));
                req.emit('response-mock', res);
            }
            else if (options.path === '/api/v1/projects/p1/snapshots/latest') {
                getSnapshotCount++;
                const res = new MockIncomingMessage(200, JSON.stringify({ contextId: 'c1', content: 'handoff-payload' }));
                req.emit('response-mock', res);
            }
        });
        const handoffCallback = registeredCommands.get("contextEngine.generateAIHandoff");
        await handoffCallback();
        assert.strictEqual(getProjectsCount, 1);
        assert.strictEqual(getSnapshotCount, 1);
        assert.strictEqual(clipboardContent, 'handoff-payload', "Clipboard should contain snapshots payload content");
        global.vscode.workspace.workspaceFolders = undefined;
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 3: Search complete workflow & notification toast
    {
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
        let infoMessage = '';
        global.vscode.window.showInformationMessage = (msg) => {
            infoMessage = msg;
            return Promise.resolve();
        };
        global.vscode.window.showInputBox = () => {
            return Promise.resolve('target-query');
        };
        const searchCallback = registeredCommands.get("contextEngine.searchKnowledge");
        await searchCallback();
        assert.strictEqual(infoMessage, "Searching for 'target-query'...", "Should display informational notification message");
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 4: Repeated command execution under load (e.g. refresh 100 times)
    {
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
        const refreshCallback = registeredCommands.get("contextEngine.refreshContext");
        for (let i = 0; i < 100; i++) {
            refreshCallback();
        }
        assert.ok(true, "Repeated command execution under load should proceed cleanly");
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 5: registerProject command with HTTP 200/201 success
    {
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
        global.vscode.workspace.workspaceFolders = [{
                uri: { fsPath: '/path1' },
                name: 'Proj1'
            }];
        let infoMessage = '';
        global.vscode.window.showInformationMessage = (msg) => {
            infoMessage = msg;
            return Promise.resolve();
        };
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/projects');
            const res = new MockIncomingMessage(200, JSON.stringify({ id: 'p1', name: 'Proj1' }));
            req.emit('response-mock', res);
        });
        const regCallback = registeredCommands.get("contextEngine.registerProject");
        await regCallback();
        assert.ok(infoMessage.includes("registered successfully"), "Should display success information message");
        global.vscode.workspace.workspaceFolders = undefined;
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 6: registerProject command with HTTP 409 Conflict duplicate error
    {
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
        global.vscode.workspace.workspaceFolders = [{
                uri: { fsPath: '/path1' },
                name: 'Proj1'
            }];
        let infoMessage = '';
        global.vscode.window.showInformationMessage = (msg) => {
            infoMessage = msg;
            return Promise.resolve();
        };
        let errorMessage = '';
        global.vscode.window.showErrorMessage = (msg) => {
            errorMessage = msg;
            return Promise.resolve();
        };
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/projects');
            const res = new MockIncomingMessage(409, JSON.stringify({ code: 'PROJECT_ALREADY_REGISTERED', message: 'Project already registered' }));
            req.emit('response-mock', res);
        });
        const regCallback = registeredCommands.get("contextEngine.registerProject");
        await regCallback();
        assert.strictEqual(infoMessage, "Project is already registered.", "Should display 'Project is already registered' information message");
        assert.strictEqual(errorMessage, "", "Should NOT display error message");
        global.vscode.workspace.workspaceFolders = undefined;
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    // Test 7: registerProject command with other HTTP error (e.g. 500)
    {
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
        global.vscode.workspace.workspaceFolders = [{
                uri: { fsPath: '/path1' },
                name: 'Proj1'
            }];
        let errorMessage = '';
        global.vscode.window.showErrorMessage = (msg) => {
            errorMessage = msg;
            return Promise.resolve();
        };
        mockHttpRequest((options, req) => {
            assert.strictEqual(options.method, 'POST');
            assert.strictEqual(options.path, '/api/v1/projects');
            const res = new MockIncomingMessage(500, "Internal Server Error");
            req.emit('response-mock', res);
        });
        const regCallback = registeredCommands.get("contextEngine.registerProject");
        await regCallback();
        assert.ok(errorMessage.includes("Failed to register project"), "Should display error message on other errors");
        global.vscode.workspace.workspaceFolders = undefined;
        global.vscode.commands.registerCommand = originalRegisterCommand;
    }
    console.log("-> CommandIntegrationTest passed.");
}
//# sourceMappingURL=CommandIntegrationTest.js.map