import * as vscode from 'vscode';

/**
 * Sandboxed WebView panel displaying the topological project module knowledge graph.
 */
export class GraphWebviewPanel {
    public static currentPanel: GraphWebviewPanel | undefined;
    private readonly panel: vscode.WebviewPanel;
    private disposables: vscode.Disposable[] = [];

    public static createOrShow(extensionUri: vscode.Uri) {
        const column = vscode.window.activeTextEditor
            ? vscode.window.activeTextEditor.viewColumn
            : undefined;

        if (GraphWebviewPanel.currentPanel) {
            GraphWebviewPanel.currentPanel.panel.reveal(column);
            return;
        }

        const panel = vscode.window.createWebviewPanel(
            'memoraGraph',
            '🕸️ Knowledge Graph Navigator',
            column || vscode.ViewColumn.One,
            {
                enableScripts: true,
                localResourceRoots: [extensionUri]
            }
        );

        GraphWebviewPanel.currentPanel = new GraphWebviewPanel(panel, extensionUri);
    }

    private constructor(panel: vscode.WebviewPanel, _extensionUri: vscode.Uri) {
        this.panel = panel;

        this.panel.webview.html = this.getHtmlForWebview();

        this.panel.onDidDispose(() => this.dispose(), null, this.disposables);
    }

    private getHtmlForWebview(): string {
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

    public dispose() {
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
