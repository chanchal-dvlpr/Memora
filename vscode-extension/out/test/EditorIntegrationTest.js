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
exports.runEditorIntegrationTests = runEditorIntegrationTests;
require("./mockVscode");
const assert = __importStar(require("assert"));
const editorIntegration_1 = require("../editorIntegration");
class MockTextDocument {
    text;
    uri;
    constructor(text, uri = { fsPath: 'test.ts' }) {
        this.text = text;
        this.uri = uri;
    }
    getText() { return this.text; }
    lineCount = 10;
    lineAt(line) {
        return {
            lineNumber: line,
            text: "class TestClass"
        };
    }
    positionAt(index) {
        return {
            line: 0,
            character: index
        };
    }
    getWordRangeAtPosition(position) {
        return {
            start: position,
            end: position
        };
    }
}
async function runEditorIntegrationTests() {
    console.log("-> Running EditorIntegrationTest...");
    // Test 1: CodeLens provision with empty document (returns empty)
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.ok(lenses !== null);
        assert.strictEqual(lenses?.length, 0, "No CodeLenses should be returned for empty text");
    }
    // Test 2: CodeLens matches class declaration
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("class TestClass {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 1);
        assert.strictEqual(lenses?.[0].command?.title, "🧠 Memora: 2 Decisions | 1 Constraint");
    }
    // Test 3: CodeLens matches interface declaration
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("interface TestInterface {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 1);
    }
    // Test 4: CodeLens matches function declaration
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("function testFunc() {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 1);
    }
    // Test 5: CodeLens matches const declaration
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("const testConst = 1;");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 1);
    }
    // Test 6: CodeLens matches let declaration
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("let testLet = 2;");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 1);
    }
    // Test 7: Multiple CodeLenses are generated for multiple declarations
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("class TestClass {}\nfunction testFunc() {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 2, "Should return 2 CodeLenses");
    }
    // Test 8: CodeLenses returns empty when disabled in settings
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const originalGet = global.vscode.workspace.getConfiguration().get;
        global.vscode.workspace.getConfiguration = () => ({
            get: (key) => {
                if (key === 'ui.renderCodeLens') {
                    return false;
                }
                return true;
            }
        });
        const document = new MockTextDocument("class TestClass {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.strictEqual(lenses?.length, 0, "Lenses should be disabled");
        global.vscode.workspace.getConfiguration = () => ({ get: originalGet });
    }
    // Test 9: HoverProvider matches 'App' keyword
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("App");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "App";
        const hover = await provider.provideHover(document, {}, {});
        assert.ok(hover !== undefined);
        assert.ok(hover.contents.value.includes("App"));
    }
    // Test 10: HoverProvider matches 'Backend' keyword
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("Backend");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "Backend";
        const hover = await provider.provideHover(document, {}, {});
        assert.ok(hover !== undefined);
        assert.ok(hover.contents.value.includes("Backend"));
    }
    // Test 11: HoverProvider matches 'Context' keyword
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("Context");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "Context";
        const hover = await provider.provideHover(document, {}, {});
        assert.ok(hover !== undefined);
    }
    // Test 12: HoverProvider matches 'Graph' keyword
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("Graph");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "Graph";
        const hover = await provider.provideHover(document, {}, {});
        assert.ok(hover !== undefined);
    }
    // Test 13: HoverProvider returns undefined for unknown keywords
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("RandomKeyword");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "RandomKeyword";
        const hover = await provider.provideHover(document, {}, {});
        assert.strictEqual(hover, undefined);
    }
    // Test 14: HoverProvider returns undefined when range is not resolved
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("App");
        document.getWordRangeAtPosition = () => undefined;
        const hover = await provider.provideHover(document, {}, {});
        assert.strictEqual(hover, undefined);
    }
    // Test 15: Diagnostics TODO detection
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const document = new MockTextDocument("// TODO: fix violation");
        manager.updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 2, "Should highlight TODO and violation");
        assert.strictEqual(diagnosticsList[0].code, "MEMORA_CONSTRAINT");
    }
    // Test 16: Diagnostics FIXME detection
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const document = new MockTextDocument("// FIXME: resolve task");
        manager.updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 1);
        assert.ok(diagnosticsList[0].message.includes("FIXME"));
    }
    // Test 17: Diagnostics violation keyword detection
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const document = new MockTextDocument("this is a violation");
        manager.updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 1);
        assert.ok(diagnosticsList[0].message.includes("violation"));
    }
    // Test 18: Clean documents yield zero diagnostics
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const document = new MockTextDocument("const cleanCode = true;");
        manager.updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 0);
    }
    // Test 19: DiagnosticsManager register registers events
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        const mockContext = { subscriptions: [] };
        manager.register(mockContext);
        assert.ok(mockContext.subscriptions.length > 0, "Registration should subscribe event listeners");
    }
    console.log("-> EditorIntegrationTest passed.");
}
//# sourceMappingURL=EditorIntegrationTest.js.map