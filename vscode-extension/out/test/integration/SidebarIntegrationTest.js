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
exports.runSidebarIntegrationTests = runSidebarIntegrationTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const sidebarProvider_1 = require("../../sidebarProvider");
async function runSidebarIntegrationTests() {
    console.log("-> Running SidebarIntegrationTest...");
    // Test 1: Dashboard Tree integrates with backend projects list
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Proj1");
    }
    // Test 2: Architecture explorer details expand
    {
        const provider = new sidebarProvider_1.ArchitectureProvider();
        const categories = await provider.getChildren();
        assert.strictEqual(categories.length, 3);
        assert.strictEqual(categories[0].label, "📝 Active Design Decisions (ADRs)");
        const adrs = await provider.getChildren(categories[0]);
        assert.strictEqual(adrs.length, 2);
        assert.strictEqual(adrs[0].label, "ADR-001: Local-First Preservation");
    }
    // Test 3: Work activities details expand features/tasks/bugs
    {
        const provider = new sidebarProvider_1.WorkProvider();
        const categories = await provider.getChildren();
        assert.strictEqual(categories.length, 3);
        const features = await provider.getChildren(categories[0]);
        assert.strictEqual(features.length, 1);
        assert.strictEqual(features[0].label, "FEAT-101: Knowledge Graph Integration");
    }
    // Test 4: Dashboard tree handles backend connection drops gracefully
    {
        const mockClient = {
            getProjects: () => Promise.reject(new Error("Daemon Offline"))
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Disconnected (Read-Only Mode)");
    }
    // Test 5: Dashboard tree renders multiple project nodes in multi-workspace folders
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] },
                { id: '2', name: 'Proj2', rootPath: '/p2', excludedPaths: [] }
            ])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 2, "Should return multiple project nodes");
        assert.strictEqual(children[0].label, "Proj1");
        assert.strictEqual(children[1].label, "Proj2");
    }
    console.log("-> SidebarIntegrationTest passed.");
}
//# sourceMappingURL=SidebarIntegrationTest.js.map