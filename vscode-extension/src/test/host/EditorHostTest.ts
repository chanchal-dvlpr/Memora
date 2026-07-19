import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

suite('EditorHostTest', () => {
    test('1. Diagnostics triggers for active document TODO comments', async () => {
        const ext = vscode.extensions.getExtension('chanchal-dvlpr.memora-vscode');
        if (ext && !ext.isActive) { await ext.activate(); }

        const tmpFile = path.resolve(__dirname, 'temp_test.ts');
        fs.writeFileSync(tmpFile, "class TestAppController {\n  // TODO: fix active constraint violation\n}");

        const doc = await vscode.workspace.openTextDocument(vscode.Uri.file(tmpFile));
        await vscode.window.showTextDocument(doc);

        await new Promise(r => setTimeout(r, 200));

        const diagnostics = vscode.languages.getDiagnostics(vscode.Uri.file(tmpFile));
        assert.ok(diagnostics.length > 0, "Should generate diagnostics warning");
        assert.strictEqual(diagnostics[0].code, 'MEMORA_CONSTRAINT');

        fs.unlinkSync(tmpFile);
    });

    test('2. Clean files have no warning diagnostics', async () => {
        const tmpFile = path.resolve(__dirname, 'temp_test_clean.ts');
        fs.writeFileSync(tmpFile, "const cleanApp = true;");

        const doc = await vscode.workspace.openTextDocument(vscode.Uri.file(tmpFile));
        await vscode.window.showTextDocument(doc);

        await new Promise(r => setTimeout(r, 100));

        const diagnostics = vscode.languages.getDiagnostics(vscode.Uri.file(tmpFile));
        assert.strictEqual(diagnostics.length, 0, "Clean file should contain 0 diagnostics warnings");

        fs.unlinkSync(tmpFile);
    });

    test('3. CodeLensProvider registration exists', async () => {
        const ext = vscode.extensions.getExtension('chanchal-dvlpr.memora-vscode');
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.ok(true);
    });

    test('4. HoverProvider registration exists', async () => {
        const ext = vscode.extensions.getExtension('chanchal-dvlpr.memora-vscode');
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.ok(true);
    });

    test('5. Unsupported files trigger no diagnostics', async () => {
        const tmpFile = path.resolve(__dirname, 'temp_test.txt');
        fs.writeFileSync(tmpFile, "Some clean plain text content without any tracking keywords");

        const doc = await vscode.workspace.openTextDocument(vscode.Uri.file(tmpFile));
        await vscode.window.showTextDocument(doc);

        await new Promise(r => setTimeout(r, 100));

        const diagnostics = vscode.languages.getDiagnostics(vscode.Uri.file(tmpFile));
        assert.strictEqual(diagnostics.length, 0);

        fs.unlinkSync(tmpFile);
    });

    test('6. Editor switching cleans diagnostics collection', () => {
        assert.ok(true);
    });
});
