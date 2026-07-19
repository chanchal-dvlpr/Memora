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
exports.runEditorConcurrencyTests = runEditorConcurrencyTests;
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
            text: "class TestAppController"
        };
    }
    positionAt(index) {
        return { line: 0, character: index };
    }
    getWordRangeAtPosition(_pos) {
        return {};
    }
}
async function runEditorConcurrencyTests() {
    console.log("-> Running EditorConcurrencyTest...");
    // Test 1: Concurrent CodeLens and Hover generation
    {
        const lensProvider = new editorIntegration_1.MemoraCodeLensProvider();
        const hoverProvider = new editorIntegration_1.MemoraHoverProvider();
        const doc = new MockTextDocument("App");
        const promises = Array.from({ length: 50 }, async () => {
            await lensProvider.provideCodeLenses(doc, {});
            await hoverProvider.provideHover(doc, {}, {});
        });
        await Promise.all(promises);
        assert.ok(true);
    }
    // Test 2: Concurrent Diagnostics Manager updates
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        manager.diagnosticCollection = {
            set: () => { },
            clear: () => { }
        };
        const doc = new MockTextDocument("class Main { // TODO: violation }");
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => manager.updateDiagnostics(doc));
        });
        await Promise.all(promises);
        assert.ok(true);
    }
    // Test 3: Multiple documents updates concurrently
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        manager.diagnosticCollection = {
            set: () => { },
            clear: () => { }
        };
        const promises = Array.from({ length: 100 }, (_, i) => {
            const doc = new MockTextDocument(`class Controller_${i} { // FIXME: violation at ${i} }`);
            return Promise.resolve().then(() => manager.updateDiagnostics(doc));
        });
        await Promise.all(promises);
        assert.ok(true);
    }
    console.log("-> EditorConcurrencyTest passed.");
}
//# sourceMappingURL=EditorConcurrencyTest.js.map