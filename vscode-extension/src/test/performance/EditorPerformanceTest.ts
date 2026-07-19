import '../mockVscode';
import * as assert from 'assert';
import { MemoraCodeLensProvider, MemoraHoverProvider, DiagnosticsManager } from '../../editorIntegration';

class MockTextDocument {
    constructor(
        public readonly text: string,
        public readonly lineCount: number = 10,
        public readonly uri: any = { fsPath: 'test.ts' }
    ) {}
    getText() { return this.text; }
    lineAt(line: number) {
        return {
            lineNumber: line,
            text: "class LargeAppController"
        };
    }
    positionAt(index: number) {
        return { line: Math.floor(index / 100), character: index % 100 };
    }
    getWordRangeAtPosition(_pos: any) {
        return {};
    }
}

export async function runEditorPerformanceTests() {
    console.log("-> Running EditorPerformanceTest...");

    // Test 1: CodeLens generation latency
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("class MainController {}");

        const start = process.hrtime.bigint();
        await provider.provideCodeLenses(document as any, {} as any);
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   CodeLens generation latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "CodeLens generation latency must be < 100 ms");
    }

    // Test 2: Hover lookup latency
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("App");

        const start = process.hrtime.bigint();
        await provider.provideHover(document as any, {} as any, {} as any);
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   Hover lookup latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 50, "Hover lookup latency must be < 50 ms");
    }

    // Test 3: Diagnostics refresh latency on large document (10,000 lines, 1MB size)
    {
        const manager = new DiagnosticsManager();
        (manager as any).diagnosticCollection = {
            set: () => {},
            clear: () => {}
        };

        // Construct 10,000 lines of mock content with violation triggers
        const lines = Array.from({ length: 10000 }, (_, i) => {
            if (i % 200 === 0) return `// TODO: unresolved active violation at line ${i}`;
            return `const val_${i} = ${i};`;
        });
        const largeText = lines.join('\n');
        const document = new MockTextDocument(largeText, 10000);

        const start = process.hrtime.bigint();
        (manager as any).updateDiagnostics(document);
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   Large file diagnostics refresh latency: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 150, "Large file diagnostics refresh latency must be < 150 ms");
    }

    console.log("-> EditorPerformanceTest passed.");
}
