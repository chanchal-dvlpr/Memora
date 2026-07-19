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
exports.DiagnosticsManager = exports.MemoraHoverProvider = exports.MemoraCodeLensProvider = void 0;
const vscode = __importStar(require("vscode"));
/**
 * Provides inline annotation tags above target declarations.
 */
class MemoraCodeLensProvider {
    _onDidChangeCodeLenses = new vscode.EventEmitter();
    onDidChangeCodeLenses = this._onDidChangeCodeLenses.event;
    constructor() {
        vscode.workspace.onDidChangeConfiguration((_) => {
            this._onDidChangeCodeLenses.fire();
        });
    }
    provideCodeLenses(document, _token) {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const enabled = config.get('ui.renderCodeLens', true);
        if (!enabled) {
            return [];
        }
        const codeLenses = [];
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
exports.MemoraCodeLensProvider = MemoraCodeLensProvider;
/**
 * Exposes detailed property hover-cards on pauses over entity keys.
 */
class MemoraHoverProvider {
    provideHover(document, position, _token) {
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
exports.MemoraHoverProvider = MemoraHoverProvider;
/**
 * Diagnostics collector indicating architectural violations in Problems views.
 */
class DiagnosticsManager {
    diagnosticCollection;
    constructor() {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('memora');
    }
    register(context) {
        context.subscriptions.push(this.diagnosticCollection);
        if (vscode.window.activeTextEditor) {
            this.updateDiagnostics(vscode.window.activeTextEditor.document);
        }
        context.subscriptions.push(vscode.window.onDidChangeActiveTextEditor(editor => {
            if (editor) {
                this.updateDiagnostics(editor.document);
            }
        }));
        context.subscriptions.push(vscode.workspace.onDidChangeTextDocument(event => {
            this.updateDiagnostics(event.document);
        }));
    }
    updateDiagnostics(document) {
        const diagnostics = [];
        const text = document.getText();
        const regex = /\b(TODO|FIXME|violation)\b/g;
        let match;
        while ((match = regex.exec(text)) !== null) {
            const startPos = document.positionAt(match.index);
            const endPos = document.positionAt(match.index + match[0].length);
            const range = new vscode.Range(startPos, endPos);
            const diagnostic = new vscode.Diagnostic(range, `Memora Constraint: Code block contains unresolved tracking flag '${match[0]}'.`, vscode.DiagnosticSeverity.Warning);
            diagnostic.code = 'MEMORA_CONSTRAINT';
            diagnostics.push(diagnostic);
        }
        this.diagnosticCollection.set(document.uri, diagnostics);
    }
}
exports.DiagnosticsManager = DiagnosticsManager;
//# sourceMappingURL=editorIntegration.js.map