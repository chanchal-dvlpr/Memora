import '../mockVscode';
import * as assert from 'assert';
import { GraphWebviewPanel } from '../../graphWebview';

export async function runWebviewPerformanceTests() {
    console.log("-> Running WebviewPerformanceTest...");

    // Test 1: HTML generation latency
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        GraphWebviewPanel.createOrShow(mockExtensionUri as any);
        const panelInstance = GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);

        const start = process.hrtime.bigint();
        const html = (panelInstance as any).getHtmlForWebview();
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   Webview HTML generation: ${duration.toFixed(2)} ms`);
        assert.ok(html.length > 0);
        assert.ok(duration < 300, "Webview HTML generation must be < 300 ms");

        (panelInstance as any).dispose();
    }

    console.log("-> WebviewPerformanceTest passed.");
}
