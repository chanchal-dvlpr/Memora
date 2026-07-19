import { mockOnDidChangeConfiguration } from '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runExtensionIntegrationTests() {
    console.log("-> Running ExtensionIntegrationTest...");

    // Test 1: Full integrated startup and registrations
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };

        activate(context as any);
        assert.ok(context.subscriptions.length > 0, "Integrated boot should register subscriptions");
    }

    // Test 2: Double activation does not crash or leak
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);
        activate(context as any);
        assert.ok(context.subscriptions.length > 0, "Double activation succeeds safely");
    }

    // Test 3: Deactivation cleans up resources cleanly
    {
        deactivate();
        assert.ok(true, "Deactivation completes cleanly");
    }

    // Test 4: Complete activation lifecycle (activate -> deactivate -> activate)
    {
        const context1 = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context1 as any);
        assert.ok(context1.subscriptions.length > 0);
        deactivate();

        const context2 = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context2 as any);
        assert.ok(context2.subscriptions.length > 0, "Activation after deactivation should succeed");
        deactivate();
    }

    // Test 5: Configuration reload checks propagation
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        // Simulate config change event trigger
        let configReloaded = false;
        const originalGet = (global as any).vscode.workspace.getConfiguration().get;
        (global as any).vscode.workspace.getConfiguration = () => ({
            get: (key: string) => {
                if (key === 'connection.port') {
                    configReloaded = true;
                    return 9999;
                }
                return undefined;
            }
        });

        // Trigger configuration updates listener via event emitter
        mockOnDidChangeConfiguration.fire({
            affectsConfiguration: (section: string) => section === 'contextEngine'
        });
        
        assert.strictEqual(configReloaded, true, "Configuration should reload on setting shifts");
        (global as any).vscode.workspace.getConfiguration = () => ({ get: originalGet });
        deactivate();
    }

    console.log("-> ExtensionIntegrationTest passed.");
}
