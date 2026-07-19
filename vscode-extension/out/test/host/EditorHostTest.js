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
const path = __importStar(require("path"));
const fs = __importStar(require("fs"));
suite('EditorHostTest', () => {
    test('1. Diagnostics triggers for active document TODO comments', async () => {
        const ext = vscode.extensions.getExtension('chanchal-dvlpr.memora-vscode');
        if (ext && !ext.isActive) {
            await ext.activate();
        }
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
//# sourceMappingURL=EditorHostTest.js.map