import * as vscode from 'vscode';
import { BackendClient, ProjectResponse } from './backendClient';

export class MemoraTreeItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly contextValue?: string,
        public readonly description?: string
    ) {
        super(label, collapsibleState);
        this.tooltip = label;
        this.description = description;
    }
}

/**
 * Tree View Provider exposing project registration and health metrics.
 */
export class DashboardProvider implements vscode.TreeDataProvider<MemoraTreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<MemoraTreeItem | undefined | null | void> = new vscode.EventEmitter<MemoraTreeItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<MemoraTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;

    private activeProjects: ProjectResponse[] = [];

    constructor(private backendClient: BackendClient) {}

    public refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: MemoraTreeItem): vscode.TreeItem {
        return element;
    }

    async getChildren(element?: MemoraTreeItem): Promise<MemoraTreeItem[]> {
        if (!element) {
            try {
                this.activeProjects = await this.backendClient.getProjects();
                if (this.activeProjects.length === 0) {
                    return [new MemoraTreeItem("No Active Projects", vscode.TreeItemCollapsibleState.None)];
                }
                return this.activeProjects.map(proj => 
                    new MemoraTreeItem(proj.name, vscode.TreeItemCollapsibleState.Collapsed, "project", proj.id)
                );
            } catch (e) {
                return [new MemoraTreeItem("Disconnected (Read-Only Mode)", vscode.TreeItemCollapsibleState.None)];
            }
        }

        if (element.contextValue === "project") {
            const projId = element.description || "";
            return [
                new MemoraTreeItem(`ID: ${projId}`, vscode.TreeItemCollapsibleState.None),
                new MemoraTreeItem("Status: Indexed", vscode.TreeItemCollapsibleState.None),
                new MemoraTreeItem("Connection: Online", vscode.TreeItemCollapsibleState.None)
            ];
        }

        return [];
    }
}

/**
 * Tree View Provider exposing architectural metadata (ADRs, constraints, assumptions).
 */
export class ArchitectureProvider implements vscode.TreeDataProvider<MemoraTreeItem> {
    constructor() {}

    getTreeItem(element: MemoraTreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: MemoraTreeItem): Thenable<MemoraTreeItem[]> {
        if (!element) {
            return Promise.resolve([
                new MemoraTreeItem("📝 Active Design Decisions (ADRs)", vscode.TreeItemCollapsibleState.Collapsed),
                new MemoraTreeItem("⚠️ System Constraints", vscode.TreeItemCollapsibleState.Collapsed),
                new MemoraTreeItem("🧠 Architectural Assumptions", vscode.TreeItemCollapsibleState.Collapsed)
            ]);
        }

        if (element.label.includes("ADRs")) {
            return Promise.resolve([
                new MemoraTreeItem("ADR-001: Local-First Preservation", vscode.TreeItemCollapsibleState.None),
                new MemoraTreeItem("ADR-002: Stdio MCP Transport", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        if (element.label.includes("Constraints")) {
            return Promise.resolve([
                new MemoraTreeItem("Limit: 2000 Tokens Max Budget", vscode.TreeItemCollapsibleState.None),
                new MemoraTreeItem("Enforce Loopback Boundary ONLY", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        if (element.label.includes("Assumptions")) {
            return Promise.resolve([
                new MemoraTreeItem("Workspace matches local scanner identity", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        return Promise.resolve([]);
    }
}

/**
 * Tree View Provider exposing workspace activity objects (Features, Tasks, Bugs).
 */
export class WorkProvider implements vscode.TreeDataProvider<MemoraTreeItem> {
    constructor() {}

    getTreeItem(element: MemoraTreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: MemoraTreeItem): Thenable<MemoraTreeItem[]> {
        if (!element) {
            return Promise.resolve([
                new MemoraTreeItem("🚀 Features", vscode.TreeItemCollapsibleState.Collapsed),
                new MemoraTreeItem("📋 Tasks", vscode.TreeItemCollapsibleState.Collapsed),
                new MemoraTreeItem("🐛 Bugs", vscode.TreeItemCollapsibleState.Collapsed)
            ]);
        }

        if (element.label.includes("Features")) {
            return Promise.resolve([
                new MemoraTreeItem("FEAT-101: Knowledge Graph Integration", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        if (element.label.includes("Tasks")) {
            return Promise.resolve([
                new MemoraTreeItem("TASK-201: Implement VS Code Interface", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        if (element.label.includes("Bugs")) {
            return Promise.resolve([
                new MemoraTreeItem("BUG-301: Fix UUID path variable converter", vscode.TreeItemCollapsibleState.None)
            ]);
        }

        return Promise.resolve([]);
    }
}
