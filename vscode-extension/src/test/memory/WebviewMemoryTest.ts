import '../mockVscode';
import * as assert from 'assert';
import { GraphWebviewPanel } from '../../graphWebview';

export async function runWebviewMemoryTests() {
    console.log("-> Running WebviewMemoryTest...");

    const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };

    // Test 1: 100 Webview panel creation/dispose cycles
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            GraphWebviewPanel.createOrShow(mockExtensionUri as any);
            const panel: any = GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            panel.dispose();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 webview panels: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 2: 500 Webview panel creation/dispose cycles (Long run check)
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            GraphWebviewPanel.createOrShow(mockExtensionUri as any);
            const panel: any = GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            panel.dispose();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 webview panels: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5, "500 panel creations must release memory clean without leaks");
    }

    console.log("-> WebviewMemoryTest passed.");
}
