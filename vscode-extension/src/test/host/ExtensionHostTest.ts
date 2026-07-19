import * as assert from 'assert';
import * as vscode from 'vscode';

suite('ExtensionHostTest', () => {
    const extId = 'chanchal-dvlpr.memora-vscode';

    test('1. Extension should be discovered by Host', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined, "Extension must be discovered");
    });

    test('2. Extension ID should match manifest', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.id, extId, "Extension ID must match publisher.name");
    });

    test('3. Extension version is registered', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.version, '0.0.1');
    });

    test('4. Extension publisher matches publisher manifest', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.publisher, 'chanchal-dvlpr');
    });

    test('5. Extension category contains Other', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext?.packageJSON.categories.includes("Other"));
    });

    test('6. Extension main entrypoint file exists', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.main, './out/extension.js');
    });

    test('7. Extension description is loaded', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.description, "Local-first AI Context Engine for Visual Studio Code");
    });

    test('8. Extension activationEvents includes onStartupFinished', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext?.packageJSON.activationEvents.includes("onStartupFinished"));
    });
});
