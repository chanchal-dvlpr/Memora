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
exports.WorkProvider = exports.ArchitectureProvider = exports.DashboardProvider = exports.MemoraTreeItem = void 0;
const vscode = __importStar(require("vscode"));
class MemoraTreeItem extends vscode.TreeItem {
    label;
    collapsibleState;
    contextValue;
    description;
    constructor(label, collapsibleState, contextValue, description) {
        super(label, collapsibleState);
        this.label = label;
        this.collapsibleState = collapsibleState;
        this.contextValue = contextValue;
        this.description = description;
        this.tooltip = label;
        this.description = description;
    }
}
exports.MemoraTreeItem = MemoraTreeItem;
/**
 * Tree View Provider exposing project registration and health metrics.
 */
class DashboardProvider {
    backendClient;
    _onDidChangeTreeData = new vscode.EventEmitter();
    onDidChangeTreeData = this._onDidChangeTreeData.event;
    activeProjects = [];
    constructor(backendClient) {
        this.backendClient = backendClient;
    }
    refresh() {
        this._onDidChangeTreeData.fire();
    }
    getTreeItem(element) {
        return element;
    }
    async getChildren(element) {
        if (!element) {
            try {
                this.activeProjects = await this.backendClient.getProjects();
                if (this.activeProjects.length === 0) {
                    return [new MemoraTreeItem("No Active Projects", vscode.TreeItemCollapsibleState.None)];
                }
                return this.activeProjects.map(proj => new MemoraTreeItem(proj.name, vscode.TreeItemCollapsibleState.Collapsed, "project", proj.id));
            }
            catch (e) {
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
exports.DashboardProvider = DashboardProvider;
/**
 * Tree View Provider exposing architectural metadata (ADRs, constraints, assumptions).
 */
class ArchitectureProvider {
    constructor() { }
    getTreeItem(element) {
        return element;
    }
    getChildren(element) {
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
exports.ArchitectureProvider = ArchitectureProvider;
/**
 * Tree View Provider exposing workspace activity objects (Features, Tasks, Bugs).
 */
class WorkProvider {
    constructor() { }
    getTreeItem(element) {
        return element;
    }
    getChildren(element) {
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
exports.WorkProvider = WorkProvider;
//# sourceMappingURL=sidebarProvider.js.map