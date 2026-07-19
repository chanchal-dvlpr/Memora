import '../mockVscode';
import * as assert from 'assert';
import { MemoraCodeLensProvider, MemoraHoverProvider, DiagnosticsManager } from '../../editorIntegration';

class MockTextDocument {
    constructor(
        public readonly text: string,
        public readonly uri: any = { fsPath: 'test.ts' }
    ) {}
    getText() { return this.text; }
    lineCount = 10;
    lineAt(line: number) {
        return {
            lineNumber: line,
            text: "class TestAppController"
        };
    }
    positionAt(index: number) {
        return { line: 0, character: index };
    }
    getWordRangeAtPosition(_pos: any) {
        return {};
    }
}

export async function runEditorMemoryTests() {
    console.log("-> Running EditorMemoryTest...");

    // Test 1: 100 diagnostics update cycles
    {
        const manager = new DiagnosticsManager();
        (manager as any).diagnosticCollection = {
            set: () => {},
            clear: () => {}
        };

        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            const doc = new MockTextDocument(`class Main { // TODO: active violation at line ${i} }`);
            (manager as any).updateDiagnostics(doc);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 diagnostics: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 2: 500 diagnostics update cycles
    {
        const manager = new DiagnosticsManager();
        (manager as any).diagnosticCollection = {
            set: () => {},
            clear: () => {}
        };

        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            const doc = new MockTextDocument(`class Main { // TODO: active violation at line ${i} }`);
            (manager as any).updateDiagnostics(doc);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 diagnostics: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 3: 1000 CodeLens and Hover generation requests (Load check)
    {
        const lensProvider = new MemoraCodeLensProvider();
        const hoverProvider = new MemoraHoverProvider();
        const doc = new MockTextDocument("App");

        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 1000; i++) {
            await lensProvider.provideCodeLenses(doc as any, {} as any);
            await hoverProvider.provideHover(doc as any, {} as any, {} as any);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 1000 CodeLens/Hover: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 10);
    }

    console.log("-> EditorMemoryTest passed.");
}
