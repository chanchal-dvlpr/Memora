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
exports.GraphWebviewPanel = void 0;
const vscode = __importStar(require("vscode"));
/**
 * Sandboxed WebView panel displaying the topological project module knowledge graph.
 */
class GraphWebviewPanel {
    static currentPanel;
    panel;
    disposables = [];
    static createOrShow(extensionUri) {
        const column = vscode.window.activeTextEditor
            ? vscode.window.activeTextEditor.viewColumn
            : undefined;
        if (GraphWebviewPanel.currentPanel) {
            GraphWebviewPanel.currentPanel.panel.reveal(column);
            return;
        }
        const panel = vscode.window.createWebviewPanel('memoraGraph', '🕸️ Knowledge Graph Navigator', column || vscode.ViewColumn.One, {
            enableScripts: true,
            localResourceRoots: [extensionUri]
        });
        GraphWebviewPanel.currentPanel = new GraphWebviewPanel(panel, extensionUri);
    }
    constructor(panel, _extensionUri) {
        this.panel = panel;
        this.panel.webview.html = this.getHtmlForWebview();
        this.panel.onDidDispose(() => this.dispose(), null, this.disposables);
    }
    getHtmlForWebview() {
        const webview = this.panel.webview;
        const cspSource = webview.cspSource;
        const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${cspSource} 'unsafe-inline'; script-src ${cspSource} 'unsafe-inline';">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Knowledge Graph Navigator</title>
    <style>
        body {
            font-family: var(--vscode-font-family, sans-serif);
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            padding: 20px;
        }
        .canvas {
            border: 1px solid var(--vscode-panel-border);
            height: 400px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-top: 20px;
            background-color: var(--vscode-sideBar-background);
        }
    </style>
</head>
<body>
    <h2>🕸️ Knowledge Graph Navigator</h2>
    <p>Topography visualization of project modules, features, and active constraint bounds.</p>
    <div class="canvas">
        <p>Interactive Network Graph Loading...</p>
    </div>
</body>
</html>`;
        return html;
    }
    dispose() {
        GraphWebviewPanel.currentPanel = undefined;
        this.panel.dispose();
        while (this.disposables.length) {
            const x = this.disposables.pop();
            if (x) {
                x.dispose();
            }
        }
    }
}
exports.GraphWebviewPanel = GraphWebviewPanel;
//# sourceMappingURL=graphWebview.js.map