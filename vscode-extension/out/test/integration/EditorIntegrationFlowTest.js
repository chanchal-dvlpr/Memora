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
exports.runEditorIntegrationFlowTests = runEditorIntegrationFlowTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const editorIntegration_1 = require("../../editorIntegration");
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
async function runEditorIntegrationFlowTests() {
    console.log("-> Running EditorIntegrationFlowTest...");
    // Test 1: CodeLens resolves declaration headers in TS
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("class AppController {}");
        const lenses = await provider.provideCodeLenses(document, {});
        assert.ok(lenses !== null);
        assert.strictEqual(lenses?.length, 1);
        assert.strictEqual(lenses?.[0].command?.title, "🧠 Memora: 2 Decisions | 1 Constraint");
    }
    // Test 2: Hover details resolve on registered keywords
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("App");
        document.getWordRangeAtPosition = () => ({});
        document.getText = () => "App";
        const hover = await provider.provideHover(document, {}, {});
        assert.ok(hover !== undefined);
        assert.ok(hover.contents.value.includes("App"));
    }
    // Test 3: Warnings diagnostics show on TODO violations in Java files
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const javaDoc = new MockTextDocument("public class Main {\n  // TODO: fix active constraint violation\n}");
        manager.updateDiagnostics(javaDoc);
        assert.strictEqual(diagnosticsList.length, 2, "Should highlight TODO and violation keywords");
        assert.strictEqual(diagnosticsList[0].code, "MEMORA_CONSTRAINT");
    }
    // Test 4: Unsupported files or clean files yield 0 diagnostics
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        let diagnosticsList = [];
        manager.diagnosticCollection = {
            set: (_uri, diagnostics) => {
                diagnosticsList = diagnostics;
            }
        };
        const doc = new MockTextDocument("const a = 1;");
        manager.updateDiagnostics(doc);
        assert.strictEqual(diagnosticsList.length, 0);
    }
    console.log("-> EditorIntegrationFlowTest passed.");
}
//# sourceMappingURL=EditorIntegrationFlowTest.js.map