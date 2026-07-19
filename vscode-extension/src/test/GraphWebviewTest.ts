import './mockVscode';
import * as assert from 'assert';
import { GraphWebviewPanel } from '../graphWebview';

export async function runGraphWebviewTests() {
    console.log("-> Running GraphWebviewTest...");

    // Test 1: Panel creation and registration
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined, "Panel should be created and cached");
    }

    // Test 2: HTML generation incorporates strict CSP
    {
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        const html = (panelInstance as any).getHtmlForWebview();
        assert.ok(html.includes("Content-Security-Policy"), "Should contain Content Security Policy meta tag");
        assert.ok(html.includes("default-src 'none'"), "CSP should restrict default loading to none");
        assert.ok(html.includes("vscode-resource"), "CSP should allow local resource schemes");
    }

    // Test 3: HTML inherits theme css variables
    {
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        const html = (panelInstance as any).getHtmlForWebview();
        assert.ok(html.includes("var(--vscode-foreground)"), "Should use VS Code theme variables");
        assert.ok(html.includes("var(--vscode-editor-background)"), "Should use VS Code theme background");
        assert.ok(html.includes("var(--vscode-panel-border)"), "Should use VS Code border colors");
    }

    // Test 4: Offline placeholder loading message is present
    {
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        const html = (panelInstance as any).getHtmlForWebview();
        assert.ok(html.includes("Interactive Network Graph Loading..."), "Should display loading placeholder");
    }

    // Test 5: Reopening panel reveals existing instance
    {
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        let revealCalled = false;
        const originalPanel = (panelInstance as any).panel;
        (panelInstance as any).panel = {
            reveal: () => { revealCalled = true; }
        };

        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        assert.strictEqual(revealCalled, true, "Reopen should reveal existing webview instance");

        (panelInstance as any).panel = originalPanel;
    }

    // Test 6: dispose clears panel reference
    {
        const panelInstance: any = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        panelInstance.dispose();
        assert.strictEqual(GraphWebviewPanel.currentPanel, undefined, "Disposal should clear panel static caching");
    }

    // Test 7: Reopening after disposal creates a new panel
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        const panelInstance: any = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        panelInstance.dispose();
    }

    // Test 8: WebView contains title header
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        const panelInstance: any = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        const html = (panelInstance as any).getHtmlForWebview();
        assert.ok(html.includes("<h2>🕸️ Knowledge Graph Navigator</h2>"));
        panelInstance.dispose();
    }

    console.log("-> GraphWebviewTest passed.");
}
