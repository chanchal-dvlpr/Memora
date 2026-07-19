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
exports.runEditorPerformanceTests = runEditorPerformanceTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const editorIntegration_1 = require("../../editorIntegration");
class MockTextDocument {
    text;
    lineCount;
    uri;
    constructor(text, lineCount = 10, uri = { fsPath: 'test.ts' }) {
        this.text = text;
        this.lineCount = lineCount;
        this.uri = uri;
    }
    getText() { return this.text; }
    lineAt(line) {
        return {
            lineNumber: line,
            text: "class LargeAppController"
        };
    }
    positionAt(index) {
        return { line: Math.floor(index / 100), character: index % 100 };
    }
    getWordRangeAtPosition(_pos) {
        return {};
    }
}
async function runEditorPerformanceTests() {
    console.log("-> Running EditorPerformanceTest...");
    // Test 1: CodeLens generation latency
    {
        const provider = new editorIntegration_1.MemoraCodeLensProvider();
        const document = new MockTextDocument("class MainController {}");
        const start = process.hrtime.bigint();
        await provider.provideCodeLenses(document, {});
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   CodeLens generation latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "CodeLens generation latency must be < 100 ms");
    }
    // Test 2: Hover lookup latency
    {
        const provider = new editorIntegration_1.MemoraHoverProvider();
        const document = new MockTextDocument("App");
        const start = process.hrtime.bigint();
        await provider.provideHover(document, {}, {});
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   Hover lookup latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 50, "Hover lookup latency must be < 50 ms");
    }
    // Test 3: Diagnostics refresh latency on large document (10,000 lines, 1MB size)
    {
        const manager = new editorIntegration_1.DiagnosticsManager();
        manager.diagnosticCollection = {
            set: () => { },
            clear: () => { }
        };
        // Construct 10,000 lines of mock content with violation triggers
        const lines = Array.from({ length: 10000 }, (_, i) => {
            if (i % 200 === 0)
                return `// TODO: unresolved active violation at line ${i}`;
            return `const val_${i} = ${i};`;
        });
        const largeText = lines.join('\n');
        const document = new MockTextDocument(largeText, 10000);
        const start = process.hrtime.bigint();
        manager.updateDiagnostics(document);
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   Large file diagnostics refresh latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 150, "Large file diagnostics refresh latency must be < 150 ms");
    }
    console.log("-> EditorPerformanceTest passed.");
}
//# sourceMappingURL=EditorPerformanceTest.js.map