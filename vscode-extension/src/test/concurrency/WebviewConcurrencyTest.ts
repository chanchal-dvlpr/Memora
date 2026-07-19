import '../mockVscode';
import * as assert from 'assert';
import { GraphWebviewPanel } from '../../graphWebview';

export async function runWebviewConcurrencyTests() {
    console.log("-> Running WebviewConcurrencyTest...");

    // Test 1: Concurrent panel creation and disposal
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        
        const promises = Array.from({ length: 20 }, async () => {
            GraphWebviewPanel.createOrShow(mockExtensionUri as any);
            const panel = GraphWebviewPanel.currentPanel;
            if (panel) {
                (panel as any).dispose();
            }
        });

        await Promise.all(promises);
        assert.strictEqual(GraphWebviewPanel.currentPanel, undefined, "Disposal cleans panel reference");
    }

    // Test 2: 50 concurrent updates to panel options/state
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        
        const promises = Array.from({ length: 50 }, async () => {
            GraphWebviewPanel.createOrShow(mockExtensionUri as any);
            const panel = GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            (panel as any).dispose();
        });

        await Promise.all(promises);
        assert.strictEqual(GraphWebviewPanel.currentPanel, undefined);
    }

    console.log("-> WebviewConcurrencyTest passed.");
}
