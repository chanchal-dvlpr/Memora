import * as assert from 'assert';
import * as vscode from 'vscode';

suite('CommandHostTest', () => {
    test('1. Command contextEngine.registerProject exists', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.registerProject"));
    });

    test('2. Command contextEngine.refreshContext exists', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.refreshContext"));
    });

    test('3. Command contextEngine.generateAIHandoff exists', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.generateAIHandoff"));
    });

    test('4. Command contextEngine.searchKnowledge exists', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.searchKnowledge"));
    });

    test('5. Command contextEngine.showArchitecture exists', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.showArchitecture"));
    });

    test('6. Command refreshContext executes cleanly', async () => {
        await vscode.commands.executeCommand("contextEngine.refreshContext");
        assert.ok(true);
    });

    test('7. Command palette presence verify', async () => {
        const cmds = await vscode.commands.getCommands(true);
        const memoraCmds = cmds.filter(c => c.startsWith('contextEngine.'));
        assert.ok(memoraCmds.length >= 5, "Should register at least 5 main command routes");
    });

    test('8. Command executions do not throw errors', async () => {
        try {
            await vscode.commands.executeCommand("contextEngine.refreshContext");
            assert.ok(true);
        } catch (err) {
            assert.fail("refreshContext execution threw error");
        }
    });
});
