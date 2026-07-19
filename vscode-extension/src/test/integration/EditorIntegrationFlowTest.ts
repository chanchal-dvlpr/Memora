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
            text: "class TestClass"
        };
    }
    positionAt(index: number) {
        return {
            line: 0,
            character: index
        };
    }
    getWordRangeAtPosition(position: any) {
        return {
            start: position,
            end: position
        };
    }
}

export async function runEditorIntegrationFlowTests() {
    console.log("-> Running EditorIntegrationFlowTest...");

    // Test 1: CodeLens resolves declaration headers in TS
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("class AppController {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.ok(lenses !== null);
        assert.strictEqual(lenses?.length, 1);
        assert.strictEqual(lenses?.[0].command?.title, "🧠 Memora: 2 Decisions | 1 Constraint");
    }

    // Test 2: Hover details resolve on registered keywords
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("App");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "App";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.ok(hover !== undefined);
        assert.ok((hover as any).contents.value.includes("App"));
    }

    // Test 3: Warnings diagnostics show on TODO violations in Java files
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const javaDoc = new MockTextDocument("public class Main {\n  // TODO: fix active constraint violation\n}");
        (manager as any).updateDiagnostics(javaDoc);

        assert.strictEqual(diagnosticsList.length, 2, "Should highlight TODO and violation keywords");
        assert.strictEqual(diagnosticsList[0].code, "MEMORA_CONSTRAINT");
    }

    // Test 4: Unsupported files or clean files yield 0 diagnostics
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const doc = new MockTextDocument("const a = 1;");
        (manager as any).updateDiagnostics(doc);
        assert.strictEqual(diagnosticsList.length, 0);
    }

    console.log("-> EditorIntegrationFlowTest passed.");
}
