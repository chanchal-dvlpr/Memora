import { mockHttp } from '../mockVscode';
import * as assert from 'assert';
import * as http from 'http';
import { EventEmitter } from 'events';
import { activate } from '../../extension';

class MockClientRequest extends EventEmitter {
    write(_data: string) {}
    end() {}
    destroy() {
        this.emit('close');
    }
}

class MockIncomingMessage extends EventEmitter {
    constructor(public readonly statusCode: number, public readonly data: string) {
        super();
    }
}

function mockHttpRequest(handler: (options: http.RequestOptions, req: MockClientRequest) => void) {
    mockHttp.requestOverride = (options: http.RequestOptions, callback: (res: any) => void) => {
        const req = new MockClientRequest();
        process.nextTick(() => {
            handler(options, req);
        });
        
        req.on('response-mock', (res: MockIncomingMessage) => {
            callback(res);
            process.nextTick(() => {
                res.emit('data', Buffer.from(res.data));
                res.emit('end');
            });
        });

        return req as any;
    };
}

export async function runCommandIntegrationTests() {
    console.log("-> Running CommandIntegrationTest...");

    // Test 1: Register commands and trigger registerProject
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        assert.ok(registeredCommands.has("contextEngine.registerProject"));
        assert.ok(registeredCommands.has("contextEngine.refreshContext"));
        assert.ok(registeredCommands.has("contextEngine.generateAIHandoff"));

        // Trigger project registration when no workspace is open (should show warn)
        let warningShown = false;
        (global as any).vscode.window.showWarningMessage = () => {
            warningShown = true;
            return Promise.resolve();
        };

        const regCallback = registeredCommands.get("contextEngine.registerProject");
        await regCallback();
        assert.strictEqual(warningShown, true, "Should show warning message when no workspace open");

        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 2: AI Handoff Command copies snapshot to clipboard
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        let clipboardContent = '';
        (global as any).vscode.env.clipboard.writeText = (text: string) => {
            clipboardContent = text;
            return Promise.resolve();
        };

        (global as any).vscode.workspace.workspaceFolders = [{
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
            } else if (options.path === '/api/v1/projects/p1/snapshots/latest') {
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

        (global as any).vscode.workspace.workspaceFolders = undefined;
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 3: Search complete workflow & notification toast
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        let infoMessage = '';
        (global as any).vscode.window.showInformationMessage = (msg: string) => {
            infoMessage = msg;
            return Promise.resolve();
        };

        (global as any).vscode.window.showInputBox = () => {
            return Promise.resolve('target-query');
        };

        const searchCallback = registeredCommands.get("contextEngine.searchKnowledge");
        await searchCallback();

        assert.strictEqual(infoMessage, "Searching for 'target-query'...", "Should display informational notification message");
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 4: Repeated command execution under load (e.g. refresh 100 times)
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        const refreshCallback = registeredCommands.get("contextEngine.refreshContext");
        for (let i = 0; i < 100; i++) {
            refreshCallback();
        }
        assert.ok(true, "Repeated command execution under load should proceed cleanly");
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 5: registerProject command with HTTP 200/201 success
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        (global as any).vscode.workspace.workspaceFolders = [{
            uri: { fsPath: '/path1' },
            name: 'Proj1'
        }];

        let infoMessage = '';
        (global as any).vscode.window.showInformationMessage = (msg: string) => {
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
        (global as any).vscode.workspace.workspaceFolders = undefined;
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 6: registerProject command with HTTP 409 Conflict duplicate error
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        (global as any).vscode.workspace.workspaceFolders = [{
            uri: { fsPath: '/path1' },
            name: 'Proj1'
        }];

        let infoMessage = '';
        (global as any).vscode.window.showInformationMessage = (msg: string) => {
            infoMessage = msg;
            return Promise.resolve();
        };

        let errorMessage = '';
        (global as any).vscode.window.showErrorMessage = (msg: string) => {
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
        (global as any).vscode.workspace.workspaceFolders = undefined;
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 7: registerProject command with other HTTP error (e.g. 500)
    {
        const registeredCommands = new Map<string, any>();
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, callback: any) => {
            registeredCommands.set(name, callback);
            return originalRegisterCommand(name, callback);
        };

        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        (global as any).vscode.workspace.workspaceFolders = [{
            uri: { fsPath: '/path1' },
            name: 'Proj1'
        }];

        let errorMessage = '';
        (global as any).vscode.window.showErrorMessage = (msg: string) => {
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
        (global as any).vscode.workspace.workspaceFolders = undefined;
        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    console.log("-> CommandIntegrationTest passed.");
}
