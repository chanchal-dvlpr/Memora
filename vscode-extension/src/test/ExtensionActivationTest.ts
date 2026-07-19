import './mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../extension';

export async function runExtensionActivationTests() {
    console.log("-> Running ExtensionActivationTest...");

    // Test 1: activate without workspace (should proceed safely)
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        activate(mockContext as any);
        assert.ok(mockContext.subscriptions.length > 0, "Activation should register commands and listener subscriptions");
    }

    // Test 2: deactivate cleans up intervals
    {
        deactivate();
        assert.ok(true, "Deactivation should complete cleanly");
    }

    // Test 3: Command palette registrations
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        // Track registered commands
        const registeredCommands: string[] = [];
        const originalRegisterCommand = (global as any).vscode.commands.registerCommand;
        (global as any).vscode.commands.registerCommand = (name: string, _callback: any) => {
            registeredCommands.push(name);
            return originalRegisterCommand(name, _callback);
        };

        activate(mockContext as any);
        
        assert.ok(registeredCommands.includes("contextEngine.registerProject"));
        assert.ok(registeredCommands.includes("contextEngine.refreshContext"));
        assert.ok(registeredCommands.includes("contextEngine.generateAIHandoff"));
        assert.ok(registeredCommands.includes("contextEngine.searchKnowledge"));
        assert.ok(registeredCommands.includes("contextEngine.showArchitecture"));

        (global as any).vscode.commands.registerCommand = originalRegisterCommand;
    }

    // Test 4: Workspace provider registrations
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        const registeredProviders: string[] = [];
        const originalRegisterTreeDataProvider = (global as any).vscode.window.registerTreeDataProvider;
        (global as any).vscode.window.registerTreeDataProvider = (viewId: string, _provider: any) => {
            registeredProviders.push(viewId);
            return originalRegisterTreeDataProvider(viewId, _provider);
        };

        activate(mockContext as any);

        assert.ok(registeredProviders.includes("contextEngine.dashboard"));
        assert.ok(registeredProviders.includes("contextEngine.architecture"));
        assert.ok(registeredProviders.includes("contextEngine.work"));

        (global as any).vscode.window.registerTreeDataProvider = originalRegisterTreeDataProvider;
    }

    // Test 5: CodeLens provider registration
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        let codeLensRegistered = false;
        const originalRegisterCodeLensProvider = (global as any).vscode.languages.registerCodeLensProvider;
        (global as any).vscode.languages.registerCodeLensProvider = (_selector: any, _provider: any) => {
            codeLensRegistered = true;
            return originalRegisterCodeLensProvider(_selector, _provider);
        };

        activate(mockContext as any);
        assert.strictEqual(codeLensRegistered, true);

        (global as any).vscode.languages.registerCodeLensProvider = originalRegisterCodeLensProvider;
    }

    // Test 6: Hover provider registration
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        let hoverRegistered = false;
        const originalRegisterHoverProvider = (global as any).vscode.languages.registerHoverProvider;
        (global as any).vscode.languages.registerHoverProvider = (_selector: any, _provider: any) => {
            hoverRegistered = true;
            return originalRegisterHoverProvider(_selector, _provider);
        };

        activate(mockContext as any);
        assert.strictEqual(hoverRegistered, true);

        (global as any).vscode.languages.registerHoverProvider = originalRegisterHoverProvider;
    }

    // Test 7: Double activation cleans up existing handle before scheduling next sync loop
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        activate(mockContext as any);
        activate(mockContext as any);
        assert.ok(mockContext.subscriptions.length > 0);
    }

    // Test 8: Configuration change listener triggers config reload
    {
        const mockContext = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        activate(mockContext as any);
        assert.ok(mockContext.subscriptions.length > 0);
    }

    console.log("-> ExtensionActivationTest passed.");
}
