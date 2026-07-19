import * as vscode from 'vscode';
import { BackendClient } from './backendClient';
import { DashboardProvider, ArchitectureProvider, WorkProvider } from './sidebarProvider';
import { MemoraCodeLensProvider, MemoraHoverProvider, DiagnosticsManager } from './editorIntegration';
import { GraphWebviewPanel } from './graphWebview';

let syncIntervalHandle: NodeJS.Timeout | undefined;
let isEditorFocused = true;

/**
 * VS Code extension activation lifecycle.
 */
export function activate(context: vscode.ExtensionContext) {
    const startTime = Date.now();
    console.log("Activating Memora VS Code Extension...");

    const backendClient = new BackendClient();

    const dashboardProvider = new DashboardProvider(backendClient);
    const architectureProvider = new ArchitectureProvider();
    const workProvider = new WorkProvider();

    vscode.window.registerTreeDataProvider('contextEngine.dashboard', dashboardProvider);
    vscode.window.registerTreeDataProvider('contextEngine.architecture', architectureProvider);
    vscode.window.registerTreeDataProvider('contextEngine.work', workProvider);

    context.subscriptions.push(
        vscode.languages.registerCodeLensProvider({ scheme: 'file' }, new MemoraCodeLensProvider())
    );
    context.subscriptions.push(
        vscode.languages.registerHoverProvider({ scheme: 'file' }, new MemoraHoverProvider())
    );

    const diagnosticsManager = new DiagnosticsManager();
    diagnosticsManager.register(context);

    context.subscriptions.push(
        vscode.commands.registerCommand('contextEngine.registerProject', async () => {
            const folder = vscode.workspace.workspaceFolders?.[0];
            if (!folder) {
                vscode.window.showWarningMessage("No workspace folder open.");
                return;
            }
            try {
                const response = await backendClient.registerProject(folder.name, folder.uri.fsPath);
                vscode.window.showInformationMessage(`Project '${response.name}' registered successfully.`);
                dashboardProvider.refresh();
            } catch (err: any) {
                if (err && err.statusCode === 409 && err.body && err.body.code === 'PROJECT_ALREADY_REGISTERED') {
                    vscode.window.showInformationMessage("Project is already registered.");
                } else {
                    vscode.window.showErrorMessage(`Failed to register project: ${err.message}`);
                }
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('contextEngine.refreshContext', () => {
            dashboardProvider.refresh();
            vscode.window.showInformationMessage("Memora context cache refreshed.");
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('contextEngine.generateAIHandoff', async () => {
            const folder = vscode.workspace.workspaceFolders?.[0];
            if (!folder) return;
            try {
                const projects = await backendClient.getProjects();
                const currentProj = projects.find(p => p.rootPath === folder.uri.fsPath);
                if (!currentProj) {
                    vscode.window.showErrorMessage("Current workspace project not registered.");
                    return;
                }
                const response = await backendClient.getLatestSnapshot(currentProj.id);
                await vscode.env.clipboard.writeText(response.content);
                vscode.window.showInformationMessage("🧠 Memora: Cryptographic state package copied to clipboard!");
            } catch (err: any) {
                vscode.window.showErrorMessage(`Failed to generate handoff: ${err.message}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('contextEngine.searchKnowledge', async () => {
            const query = await vscode.window.showInputBox({ 
                prompt: "Search Knowledge", 
                placeHolder: "Enter query to search files, directories, snapshots, and symbols..."
            });
            if (!query || query.trim() === "") {
                return;
            }

            vscode.window.showInformationMessage(`Searching for '${query}'...`);

            const folder = vscode.workspace.workspaceFolders?.[0];
            if (!folder) {
                vscode.window.showWarningMessage("No workspace folder open.");
                return;
            }

            const term = query.toLowerCase();
            const results: (vscode.QuickPickItem & { uri?: vscode.Uri, line?: number })[] = [];

            try {
                // 1. Get latest snapshot content and search it
                const projects = await backendClient.getProjects();
                const currentProj = projects.find(p => p.rootPath === folder.uri.fsPath);
                if (currentProj) {
                    try {
                        const snap = await backendClient.getLatestSnapshot(currentProj.id);
                        if (snap && snap.content) {
                            const lines = snap.content.split('\n');
                            lines.forEach((line, index) => {
                                if (line.toLowerCase().includes(term)) {
                                    results.push({
                                        label: line.trim().substring(0, 100),
                                        description: `Latest Snapshot (Line ${index + 1})`,
                                        detail: "Snapshot match",
                                        line: index + 1
                                    });
                                }
                            });
                        }
                    } catch (e) {
                        // Tolerate missing snapshot / error
                    }
                }

                // 2. Search workspace file & directory names, and Markdown content
                const files = await vscode.workspace.findFiles('**/*', '**/node_modules/**');
                for (const file of files) {
                    const relativePath = vscode.workspace.asRelativePath(file);
                    // Match file name or relative path
                    if (relativePath.toLowerCase().includes(term)) {
                        results.push({
                            label: file.path.split('/').pop() || relativePath,
                            description: relativePath,
                            detail: "File Name Match",
                            uri: file
                        });
                    }

                    // Match directory names
                    const dirs = relativePath.split('/');
                    for (let i = 0; i < dirs.length - 1; i++) {
                        if (dirs[i].toLowerCase().includes(term)) {
                            results.push({
                                label: dirs[i],
                                description: relativePath.substring(0, relativePath.indexOf(dirs[i]) + dirs[i].length),
                                detail: "Directory Match",
                                uri: file
                            });
                            break;
                        }
                    }

                    // Match Markdown content inside files
                    if (relativePath.endsWith('.md')) {
                        try {
                            const content = await vscode.workspace.fs.readFile(file);
                            const text = Buffer.from(content).toString('utf-8');
                            const lines = text.split('\n');
                            lines.forEach((line, index) => {
                                if (line.toLowerCase().includes(term)) {
                                    results.push({
                                        label: line.trim().substring(0, 100),
                                        description: relativePath,
                                        detail: `Markdown Match (Line ${index + 1})`,
                                        uri: file,
                                        line: index
                                    });
                                }
                            });
                        } catch (err) {
                            // ignore read errors
                        }
                    }
                }

                // 3. Search Workspace Symbols (if language services are active)
                try {
                    const symbols = await vscode.commands.executeCommand<vscode.SymbolInformation[]>(
                        'vscode.executeWorkspaceSymbolProvider', 
                        query
                    );
                    if (symbols) {
                        for (const sym of symbols) {
                            results.push({
                                label: sym.name,
                                description: `${vscode.SymbolKind[sym.kind]} in ${vscode.workspace.asRelativePath(sym.location.uri)}`,
                                detail: "Symbol Match",
                                uri: sym.location.uri,
                                line: sym.location.range.start.line
                            });
                        }
                    }
                } catch (e) {
                    // ignore if symbol provider fails or is not registered
                }

                if (results.length === 0) {
                    vscode.window.showInformationMessage("No matching search results found.");
                    return;
                }

                const selected = await vscode.window.showQuickPick(results, {
                    placeHolder: `Search Results for '${query}'`
                });

                if (selected && selected.uri) {
                    const doc = await vscode.workspace.openTextDocument(selected.uri);
                    const editor = await vscode.window.showTextDocument(doc);
                    if (selected.line !== undefined) {
                        const position = new vscode.Position(selected.line, 0);
                        editor.selection = new vscode.Selection(position, position);
                        editor.revealRange(new vscode.Range(position, position), vscode.TextEditorRevealType.InCenter);
                    }
                }
            } catch (err: any) {
                vscode.window.showErrorMessage(`Search failed: ${err.message}`);
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('contextEngine.showArchitecture', () => {
            GraphWebviewPanel.createOrShow(context.extensionUri);
        })
    );

    const startSyncLoop = (interval: number) => {
        if (syncIntervalHandle) {
            clearInterval(syncIntervalHandle);
        }
        syncIntervalHandle = setInterval(async () => {
            try {
                dashboardProvider.refresh();
            } catch (err) {
                // heartbeats tolerate disconnected daemon state
            }
        }, interval);
    };

    context.subscriptions.push(
        vscode.window.onDidChangeWindowState((state) => {
            isEditorFocused = state.focused;
            const nextInterval = isEditorFocused ? 5000 : 60000;
            startSyncLoop(nextInterval);
            console.log(`Dynamic sync loop interval set to ${nextInterval} ms`);
        })
    );

    startSyncLoop(5000);

    context.subscriptions.push(
        vscode.workspace.onDidChangeConfiguration(event => {
            if (event.affectsConfiguration('contextEngine')) {
                backendClient.updateConfig();
            }
        })
    );

    const activationDuration = Date.now() - startTime;
    console.log(`Memora VS Code Extension activated in ${activationDuration} ms.`);
}

export function deactivate() {
    if (syncIntervalHandle) {
        clearInterval(syncIntervalHandle);
    }
}
