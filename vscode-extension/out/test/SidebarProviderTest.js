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
exports.runSidebarProviderTests = runSidebarProviderTests;
require("./mockVscode");
const assert = __importStar(require("assert"));
const sidebarProvider_1 = require("../sidebarProvider");
async function runSidebarProviderTests() {
    console.log("-> Running SidebarProviderTest...");
    // Test 1: TreeItem instantiation with parameters
    {
        const item = new sidebarProvider_1.MemoraTreeItem("Label1", 1, "projectContext", "Description1");
        assert.strictEqual(item.label, "Label1");
        assert.strictEqual(item.collapsibleState, 1);
        assert.strictEqual(item.contextValue, "projectContext");
        assert.strictEqual(item.description, "Description1");
    }
    // Test 2: TreeItem defaults to none collapsible state
    {
        const item = new sidebarProvider_1.MemoraTreeItem("Label2", 0);
        assert.strictEqual(item.collapsibleState, 0);
    }
    // Test 3: DashboardProvider getTreeItem returns exact node
    {
        const provider = new sidebarProvider_1.DashboardProvider({});
        const item = new sidebarProvider_1.MemoraTreeItem("Label3", 1);
        const returnedItem = provider.getTreeItem(item);
        assert.strictEqual(returnedItem, item);
    }
    // Test 4: DashboardProvider getChildren with empty project list
    {
        const mockClient = {
            getProjects: () => Promise.resolve([])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "No Active Projects");
    }
    // Test 5: DashboardProvider getChildren with one project node
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: 'p1', name: 'Project 1', rootPath: '/p1', excludedPaths: [] }
            ])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Project 1");
        assert.strictEqual(children[0].contextValue, "project");
    }
    // Test 6: DashboardProvider getChildren with multiple project nodes
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: 'p1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] },
                { id: 'p2', name: 'Proj2', rootPath: '/p2', excludedPaths: [] }
            ])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 2);
        assert.strictEqual(children[0].label, "Proj1");
        assert.strictEqual(children[1].label, "Proj2");
    }
    // Test 7: DashboardProvider resolves child details on project selection
    {
        const provider = new sidebarProvider_1.DashboardProvider({});
        const projectItem = new sidebarProvider_1.MemoraTreeItem("Proj1", 1, "project", "p1");
        const details = await provider.getChildren(projectItem);
        assert.strictEqual(details.length, 3);
        assert.strictEqual(details[0].label, "ID: p1");
        assert.strictEqual(details[1].label, "Status: Indexed");
        assert.strictEqual(details[2].label, "Connection: Online");
    }
    // Test 8: DashboardProvider reports Disconnected when backend fails
    {
        const mockClient = {
            getProjects: () => Promise.reject(new Error("Network Drop"))
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Disconnected (Read-Only Mode)");
    }
    // Test 9: DashboardProvider refresh fires event listener exactly once
    {
        const provider = new sidebarProvider_1.DashboardProvider({});
        let fireCount = 0;
        provider.onDidChangeTreeData(() => {
            fireCount++;
        });
        provider.refresh();
        assert.strictEqual(fireCount, 1, "Refresh event should trigger listener once");
    }
    // Test 10: ArchitectureProvider getTreeItem returns element
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const item = new sidebarProvider_1.MemoraTreeItem("ArchNode", 0);
        const returned = provider.getTreeItem(item);
        assert.strictEqual(returned, item);
    }
    // Test 11: ArchitectureProvider resolves root nodes
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const roots = await provider.getChildren();
        assert.strictEqual(roots.length, 3);
        assert.strictEqual(roots[0].label, "📝 Active Design Decisions (ADRs)");
        assert.strictEqual(roots[1].label, "⚠️ System Constraints");
        assert.strictEqual(roots[2].label, "🧠 Architectural Assumptions");
    }
    // Test 12: ArchitectureProvider expands ADR sub-items
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const parent = new sidebarProvider_1.MemoraTreeItem("📝 Active Design Decisions (ADRs)", 1);
        const children = await provider.getChildren(parent);
        assert.strictEqual(children.length, 2);
        assert.strictEqual(children[0].label, "ADR-001: Local-First Preservation");
        assert.strictEqual(children[1].label, "ADR-002: Stdio MCP Transport");
    }
    // Test 13: ArchitectureProvider expands Constraints sub-items
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const parent = new sidebarProvider_1.MemoraTreeItem("⚠️ System Constraints", 1);
        const children = await provider.getChildren(parent);
        assert.strictEqual(children.length, 2);
        assert.strictEqual(children[0].label, "Limit: 2000 Tokens Max Budget");
    }
    // Test 14: ArchitectureProvider expands Assumptions sub-items
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const parent = new sidebarProvider_1.MemoraTreeItem("🧠 Architectural Assumptions", 1);
        const children = await provider.getChildren(parent);
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Workspace matches local scanner identity");
    }
    // Test 15: WorkProvider getTreeItem returns element
    {
        const provider = new sidebarProvider_1.WorkProvider();
        const item = new sidebarProvider_1.MemoraTreeItem("WorkNode", 0);
        assert.strictEqual(provider.getTreeItem(item), item);
    }
    // Test 16: WorkProvider resolves root categories
    {
        const provider = new sidebarProvider_1.WorkProvider();
        const roots = await provider.getChildren();
        assert.strictEqual(roots.length, 3);
        assert.strictEqual(roots[0].label, "🚀 Features");
        assert.strictEqual(roots[1].label, "📋 Tasks");
        assert.strictEqual(roots[2].label, "🐛 Bugs");
    }
    // Test 17: WorkProvider expands Features sub-items
    {
        const provider = new sidebarProvider_1.WorkProvider();
        const parent = new sidebarProvider_1.MemoraTreeItem("🚀 Features", 1);
        const children = await provider.getChildren(parent);
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "FEAT-101: Knowledge Graph Integration");
    }
    // Test 18: WorkProvider expands Tasks & Bugs sub-items
    {
        const provider = new sidebarProvider_1.WorkProvider();
        const parentTask = new sidebarProvider_1.MemoraTreeItem("📋 Tasks", 1);
        const taskChildren = await provider.getChildren(parentTask);
        assert.strictEqual(taskChildren.length, 1);
        assert.strictEqual(taskChildren[0].label, "TASK-201: Implement VS Code Interface");
        const parentBug = new sidebarProvider_1.MemoraTreeItem("🐛 Bugs", 1);
        const bugChildren = await provider.getChildren(parentBug);
        assert.strictEqual(bugChildren.length, 1);
        assert.strictEqual(bugChildren[0].label, "BUG-301: Fix UUID path variable converter");
    }
    console.log("-> SidebarProviderTest passed.");
}
//# sourceMappingURL=SidebarProviderTest.js.map