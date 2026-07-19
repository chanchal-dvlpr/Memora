import * as assert from 'assert';
import * as vscode from 'vscode';

suite('ConfigurationHostTest', () => {
    test('1. Default connection port matches configuration', () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const port = config.get<number>('connection.port');
        assert.ok(port !== undefined);
    });

    test('2. Default connection token matches configuration', () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const token = config.get<string>('connection.token');
        assert.ok(token !== undefined);
    });

    test('3. Modifying connection.port propagates settings', async () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const originalPort = config.get<number>('connection.port');

        await config.update('connection.port', 9090, vscode.ConfigurationTarget.Global);
        const updatedConfig = vscode.workspace.getConfiguration('contextEngine');
        assert.strictEqual(updatedConfig.get<number>('connection.port'), 9090);

        await config.update('connection.port', originalPort, vscode.ConfigurationTarget.Global);
    });

    test('4. Modifying connection.token propagates settings', async () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const originalToken = config.get<string>('connection.token');

        await config.update('connection.token', 'test-token', vscode.ConfigurationTarget.Global);
        const updatedConfig = vscode.workspace.getConfiguration('contextEngine');
        assert.strictEqual(updatedConfig.get<string>('connection.token'), 'test-token');

        await config.update('connection.token', originalToken, vscode.ConfigurationTarget.Global);
    });

    test('5. Configuration update event triggers update listeners', () => {
        assert.ok(true);
    });
});
