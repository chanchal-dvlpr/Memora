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
exports.runEditorMemoryTests = runEditorMemoryTests;
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
async function runEditorMemoryTests() {
    console.log("-> Running EditorMemoryTest...");
    // Test 1: 100 diagnostics update cycles
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        manager.diagnosticCollection = {
            set: () => { },
            clear: () => { }
        };
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            const doc = new MockTextDocument(`class Main { // TODO: active violation at line ${i} }`);
            manager.updateDiagnostics(doc);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 diagnostics: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }
    // Test 2: 500 diagnostics update cycles
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        manager.diagnosticCollection = {
            set: () => { },
            clear: () => { }
        };
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            const doc = new MockTextDocument(`class Main { // TODO: active violation at line ${i} }`);
            manager.updateDiagnostics(doc);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 diagnostics: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }
    // Test 3: 1000 CodeLens and Hover generation requests (Load check)
    {
        const lensProvider = new editorIntegration_1.MemoraCodeLensProvider();
        const hoverProvider = new editorIntegration_1.MemoraHoverProvider();
        const doc = new MockTextDocument("App");
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 1000; i++) {
            await lensProvider.provideCodeLenses(doc, {});
            await hoverProvider.provideHover(doc, {}, {});
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 1000 CodeLens/Hover: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 10);
    }
    console.log("-> EditorMemoryTest passed.");
}
//# sourceMappingURL=EditorMemoryTest.js.map