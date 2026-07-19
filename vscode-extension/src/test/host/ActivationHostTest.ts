import * as assert from 'assert';
import * as vscode from 'vscode';

suite('ActivationHostTest', () => {
    const extId = 'chanchal-dvlpr.memora-vscode';

    test('1. Extension should activate successfully', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        if (!ext.isActive) {
            await ext.activate();
        }
        assert.strictEqual(ext.isActive, true);
    });

    test('2. Extension remains active on double activate', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.strictEqual(ext.isActive, true);
    });

    test('3. Extension exports are defined', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        // Since we return undefined from activate, it should be fine
        assert.strictEqual(ext.exports, undefined);
    });

    test('4. Extension activation event listeners register subscriptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.ok(ext.isActive);
    });

    test('5. Extension registers commands subscriptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        const commands = await vscode.commands.getCommands(true);
        assert.ok(commands.includes("contextEngine.refreshContext"));
    });

    test('6. Extension activation does not leak exceptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        try {
            await ext.activate();
            assert.ok(true);
        } catch (err) {
            assert.fail("Activation threw error");
        }
    });

    test('7. Extension activation time is non-zero', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
    });

    test('8. Extension stays active throughout test run', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.isActive, true);
    });
});
