import '../mockVscode';
import * as assert from 'assert';
import { GraphWebviewPanel } from '../../graphWebview';

export async function runWebviewIntegrationTests() {
    console.log("-> Running WebviewIntegrationTest...");

    // Test 1: Panel creation and visual properties
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined, "Webview panel instance must be cached");

        const html = (panelInstance as any).getHtmlForWebview();
        assert.ok(html.includes("Content-Security-Policy"), "Should contain CSP tags");
        assert.ok(html.includes("var(--vscode-foreground)"), "Should use VS Code theme variables");
    }

    // Test 2: dispose cleans up references and instances
    {
        const panelInstance: any = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        panelInstance.dispose();
        assert.strictEqual(GraphWebviewPanel.currentPanel, undefined, "Disposal clears cache reference");
    }

    console.log("-> WebviewIntegrationTest passed.");
}
