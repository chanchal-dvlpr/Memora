"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const assert = __importStar(require("assert"));
const vscode = __importStar(require("vscode"));
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
        }
        catch (err) {
            assert.fail("refreshContext execution threw error");
        }
    });
});
//# sourceMappingURL=CommandHostTest.js.map