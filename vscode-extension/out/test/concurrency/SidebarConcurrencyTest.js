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
exports.runSidebarConcurrencyTests = runSidebarConcurrencyTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const sidebarProvider_1 = require("../../sidebarProvider");
async function runSidebarConcurrencyTests() {
    console.log("-> Running SidebarConcurrencyTest...");
    // Test 1: Concurrent getChildren calls
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const promises = Array.from({ length: 50 }, () => provider.getChildren());
        const results = await Promise.all(promises);
        results.forEach(children => {
            assert.strictEqual(children.length, 1);
            assert.strictEqual(children[0].label, "Proj1");
        });
    }
    // Test 2: Project list shifts (add/remove) concurrently
    {
        let projects = [
            { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
        ];
        const mockClient = {
            getProjects: () => Promise.resolve(projects)
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        // Concurrently query children while projects array shifts dynamically
        const p1 = provider.getChildren();
        projects.push({ id: '2', name: 'Proj2', rootPath: '/p2', excludedPaths: [] });
        const p2 = provider.getChildren();
        projects.pop();
        const p3 = provider.getChildren();
        const [r1, r2, r3] = await Promise.all([p1, p2, p3]);
        assert.ok(r1.length > 0);
        assert.ok(r2.length > 0);
        assert.ok(r3.length > 0);
    }
    console.log("-> SidebarConcurrencyTest passed.");
}
//# sourceMappingURL=SidebarConcurrencyTest.js.map