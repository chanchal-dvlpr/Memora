import './mockVscode';
import * as assert from 'assert';
import { MemoraCodeLensProvider, MemoraHoverProvider, DiagnosticsManager } from '../editorIntegration';

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

export async function runEditorIntegrationTests() {
    console.log("-> Running EditorIntegrationTest...");

    // Test 1: CodeLens provision with empty document (returns empty)
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.ok(lenses !== null);
        assert.strictEqual(lenses?.length, 0, "No CodeLenses should be returned for empty text");
    }

    // Test 2: CodeLens matches class declaration
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("class TestClass {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 1);
        assert.strictEqual(lenses?.[0].command?.title, "🧠 Memora: 2 Decisions | 1 Constraint");
    }

    // Test 3: CodeLens matches interface declaration
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("interface TestInterface {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 1);
    }

    // Test 4: CodeLens matches function declaration
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("function testFunc() {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 1);
    }

    // Test 5: CodeLens matches const declaration
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("const testConst = 1;");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 1);
    }

    // Test 6: CodeLens matches let declaration
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("let testLet = 2;");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 1);
    }

    // Test 7: Multiple CodeLenses are generated for multiple declarations
    {
        const provider = new MemoraCodeLensProvider();
        const document = new MockTextDocument("class TestClass {}\nfunction testFunc() {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 2, "Should return 2 CodeLenses");
    }

    // Test 8: CodeLenses returns empty when disabled in settings
    {
        const provider = new MemoraCodeLensProvider();
        const originalGet = (global as any).vscode.workspace.getConfiguration().get;
        (global as any).vscode.workspace.getConfiguration = () => ({
            get: (key: string) => {
                if (key === 'ui.renderCodeLens') { return false; }
                return true;
            }
        });

        const document = new MockTextDocument("class TestClass {}");
        const lenses = await provider.provideCodeLenses(document as any, {} as any);
        assert.strictEqual(lenses?.length, 0, "Lenses should be disabled");

        (global as any).vscode.workspace.getConfiguration = () => ({ get: originalGet });
    }

    // Test 9: HoverProvider matches 'App' keyword
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("App");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "App";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.ok(hover !== undefined);
        assert.ok((hover as any).contents.value.includes("App"));
    }

    // Test 10: HoverProvider matches 'Backend' keyword
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("Backend");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "Backend";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.ok(hover !== undefined);
        assert.ok((hover as any).contents.value.includes("Backend"));
    }

    // Test 11: HoverProvider matches 'Context' keyword
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("Context");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "Context";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.ok(hover !== undefined);
    }

    // Test 12: HoverProvider matches 'Graph' keyword
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("Graph");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "Graph";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.ok(hover !== undefined);
    }

    // Test 13: HoverProvider returns undefined for unknown keywords
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("RandomKeyword");
        (document as any).getWordRangeAtPosition = () => ({});
        (document as any).getText = () => "RandomKeyword";

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.strictEqual(hover, undefined);
    }

    // Test 14: HoverProvider returns undefined when range is not resolved
    {
        const provider = new MemoraHoverProvider();
        const document = new MockTextDocument("App");
        (document as any).getWordRangeAtPosition = () => undefined;

        const hover = await provider.provideHover(document as any, {} as any, {} as any);
        assert.strictEqual(hover, undefined);
    }

    // Test 15: Diagnostics TODO detection
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const document = new MockTextDocument("// TODO: fix violation");
        (manager as any).updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 2, "Should highlight TODO and violation");
        assert.strictEqual(diagnosticsList[0].code, "MEMORA_CONSTRAINT");
    }

    // Test 16: Diagnostics FIXME detection
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const document = new MockTextDocument("// FIXME: resolve task");
        (manager as any).updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 1);
        assert.ok(diagnosticsList[0].message.includes("FIXME"));
    }

    // Test 17: Diagnostics violation keyword detection
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const document = new MockTextDocument("this is a violation");
        (manager as any).updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 1);
        assert.ok(diagnosticsList[0].message.includes("violation"));
    }

    // Test 18: Clean documents yield zero diagnostics
    {
        const manager = new DiagnosticsManager();
        let diagnosticsList: any[] = [];
        (manager as any).diagnosticCollection = {
            set: (_uri: any, diagnostics: any[]) => {
                diagnosticsList = diagnostics;
            }
        };

        const document = new MockTextDocument("const cleanCode = true;");
        (manager as any).updateDiagnostics(document);
        assert.strictEqual(diagnosticsList.length, 0);
    }

    // Test 19: DiagnosticsManager register registers events
    {
        const manager = new DiagnosticsManager();
        const mockContext = { subscriptions: [] };
        manager.register(mockContext as any);
        assert.ok(mockContext.subscriptions.length > 0, "Registration should subscribe event listeners");
    }

    console.log("-> EditorIntegrationTest passed.");
}
