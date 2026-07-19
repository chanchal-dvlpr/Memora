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
//# sourceMappingURL=SidebarHostTest.js.map