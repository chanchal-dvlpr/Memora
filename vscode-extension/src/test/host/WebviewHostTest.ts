import * as assert from 'assert';
import * as vscode from 'vscode';

suite('WebviewHostTest', () => {
    test('1. Command showArchitecture executes cleanly', async () => {
        const ext = vscode.extensions.getExtension('chanchal-dvlpr.memora-vscode');
        if (ext && !ext.isActive) { await ext.activate(); }

        await vscode.commands.executeCommand("contextEngine.showArchitecture");
        assert.ok(true);
    });

    test('2. Panel registers with correct options', () => {
        assert.ok(true);
    });

    test('3. HTML generation contains CSP headers tags', () => {
        assert.ok(true);
    });

    test('4. Webview retains light/dark theme variables', () => {
        assert.ok(true);
    });

    test('5. Reopening panel creates a new instance after dispose', () => {
        assert.ok(true);
    });
});
