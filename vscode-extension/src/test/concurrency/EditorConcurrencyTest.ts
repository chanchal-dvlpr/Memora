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

export async function runEditorConcurrencyTests() {
    console.log("-> Running EditorConcurrencyTest...");

    // Test 1: Concurrent CodeLens and Hover generation
    {
        const lensProvider = new MemoraCodeLensProvider();
        const hoverProvider = new MemoraHoverProvider();
        const doc = new MockTextDocument("App");

        const promises = Array.from({ length: 50 }, async () => {
            await lensProvider.provideCodeLenses(doc as any, {} as any);
            await hoverProvider.provideHover(doc as any, {} as any, {} as any);
        });

        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 2: Concurrent Diagnostics Manager updates
    {
        const manager = new DiagnosticsManager();
        (manager as any).diagnosticCollection = {
            set: () => {},
            clear: () => {}
        };

        const doc = new MockTextDocument("class Main { // TODO: violation }");
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => (manager as any).updateDiagnostics(doc));
        });

        await Promise.all(promises);
        assert.ok(true);
    }

    // Test 3: Multiple documents updates concurrently
    {
        const manager = new DiagnosticsManager();
        (manager as any).diagnosticCollection = {
            set: () => {},
            clear: () => {}
        };

        const promises = Array.from({ length: 100 }, (_, i) => {
            const doc = new MockTextDocument(`class Controller_${i} { // FIXME: violation at ${i} }`);
            return Promise.resolve().then(() => (manager as any).updateDiagnostics(doc));
        });

        await Promise.all(promises);
        assert.ok(true);
    }

    console.log("-> EditorConcurrencyTest passed.");
}
