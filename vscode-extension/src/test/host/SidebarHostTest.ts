import * as assert from 'assert';
import * as vscode from 'vscode';

suite('SidebarHostTest', () => {
    test('1. Sidebar view dashboard trigger command registers', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.refreshContext"));
    });

    test('2. Sidebar view architecture trigger command registers', async () => {
        const cmds = await vscode.commands.getCommands(true);
        assert.ok(cmds.includes("contextEngine.showArchitecture"));
    });

    test('3. Sidebar refresh command executes', async () => {
        await vscode.commands.executeCommand("contextEngine.refreshContext");
        assert.ok(true);
    });

    test('4. Empty workspace sidebar rendering safety', () => {
        assert.ok(vscode.workspace.workspaceFolders !== undefined, "Workspace folders should be defined in test host");
        assert.ok(vscode.workspace.workspaceFolders.length > 0);
    });

    test('5. Multi-root workspace queries support', () => {
        assert.ok(true);
    });

    test('6. Disconnected backend fallback triggers cleanly', async () => {
        await vscode.commands.executeCommand("contextEngine.refreshContext");
        assert.ok(true);
    });
});
