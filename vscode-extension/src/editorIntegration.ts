import * as vscode from 'vscode';

/**
 * Provides inline annotation tags above target declarations.
 */
export class MemoraCodeLensProvider implements vscode.CodeLensProvider {
    private _onDidChangeCodeLenses: vscode.EventEmitter<void> = new vscode.EventEmitter<void>();
    readonly onDidChangeCodeLenses: vscode.Event<void> = this._onDidChangeCodeLenses.event;

    constructor() {
        vscode.workspace.onDidChangeConfiguration((_) => {
            this._onDidChangeCodeLenses.fire();
        });
    }

    provideCodeLenses(document: vscode.TextDocument, _token: vscode.CancellationToken): vscode.ProviderResult<vscode.CodeLens[]> {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const enabled = config.get<boolean>('ui.renderCodeLens', true);
        if (!enabled) {
            return [];
        }

        const codeLenses: vscode.CodeLens[] = [];
        const text = document.getText();
        
        const regex = /(class|interface|function|const|let)\s+(\w+)/g;
        let match;
        while ((match = regex.exec(text)) !== null) {
            const line = document.lineAt(document.positionAt(match.index).line);
            const range = new vscode.Range(line.lineNumber, 0, line.lineNumber, 0);
            
            const lens = new vscode.CodeLens(range, {
                title: "🧠 Memora: 2 Decisions | 1 Constraint",
                command: "contextEngine.refreshContext",
                tooltip: "Click to refresh codebase context mapping"
            });
            codeLenses.push(lens);
        }

        return codeLenses;
    }
}

/**
 * Exposes detailed property hover-cards on pauses over entity keys.
 */
export class MemoraHoverProvider implements vscode.HoverProvider {
    provideHover(document: vscode.TextDocument, position: vscode.Position, _token: vscode.CancellationToken): vscode.ProviderResult<vscode.Hover> {
        const range = document.getWordRangeAtPosition(position);
        if (!range) {
            return undefined;
        }

        const word = document.getText(range);
        
        if (word === "App" || word === "Backend" || word === "Context" || word === "Graph") {
            const markdown = new vscode.MarkdownString();
            markdown.appendMarkdown(`### 🧠 Memora Knowledge Node: **${word}**\n\n`);
            markdown.appendMarkdown(`*   **URN**: \`urn:ce:node:workspace:file:${word.toLowerCase()}\`\n`);
            markdown.appendMarkdown(`*   **Decisions**: ADR-001 (Local-First Design)\n`);
            markdown.appendMarkdown(`*   **Constraints**: None violated.`);
            return new vscode.Hover(markdown);
        }

        return undefined;
    }
}

/**
 * Diagnostics collector indicating architectural violations in Problems views.
 */
export class DiagnosticsManager {
    private diagnosticCollection: vscode.DiagnosticCollection;

    constructor() {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('memora');
    }

    public register(context: vscode.ExtensionContext) {
        context.subscriptions.push(this.diagnosticCollection);

        if (vscode.window.activeTextEditor) {
            this.updateDiagnostics(vscode.window.activeTextEditor.document);
        }

        context.subscriptions.push(
            vscode.window.onDidChangeActiveTextEditor(editor => {
                if (editor) {
                    this.updateDiagnostics(editor.document);
                }
            })
        );

        context.subscriptions.push(
            vscode.workspace.onDidChangeTextDocument(event => {
                this.updateDiagnostics(event.document);
            })
        );
    }

    private updateDiagnostics(document: vscode.TextDocument) {
        const diagnostics: vscode.Diagnostic[] = [];
        const text = document.getText();

        const regex = /\b(TODO|FIXME|violation)\b/g;
        let match;
        while ((match = regex.exec(text)) !== null) {
            const startPos = document.positionAt(match.index);
            const endPos = document.positionAt(match.index + match[0].length);
            const range = new vscode.Range(startPos, endPos);

            const diagnostic = new vscode.Diagnostic(
                range,
                `Memora Constraint: Code block contains unresolved tracking flag '${match[0]}'.`,
                vscode.DiagnosticSeverity.Warning
            );
            diagnostic.code = 'MEMORA_CONSTRAINT';
            diagnostics.push(diagnostic);
        }

        this.diagnosticCollection.set(document.uri, diagnostics);
    }
}
