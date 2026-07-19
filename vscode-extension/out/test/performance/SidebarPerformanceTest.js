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
exports.runSidebarPerformanceTests = runSidebarPerformanceTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const sidebarProvider_1 = require("../../sidebarProvider");
async function runSidebarPerformanceTests() {
    console.log("-> Running SidebarPerformanceTest...");
    // Test 1: Initial tree creation latency
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };
        const start = process.hrtime.bigint();
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        await provider.getChildren();
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   Initial tree creation: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "Initial tree creation must be < 100 ms");
    }
    // Test 2: Tree rendering latency with 1000 nodes (Scale/Load Test)
    {
        const largeProjectList = Array.from({ length: 1000 }, (_, i) => ({
            id: String(i),
            name: `Proj-${i}`,
            rootPath: `/path/${i}`,
            excludedPaths: []
        }));
        const mockClient = {
            getProjects: () => Promise.resolve(largeProjectList)
        };
        const provider = new sidebarProvider_1.DashboardProvider(mockClient);
        const start = process.hrtime.bigint();
        const children = await provider.getChildren();
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;
        console.log(`   1000-node tree rendering: ${duration.toFixed(2)} ms`);
        assert.ok(children.length === 1000);
        assert.ok(duration < 100, "1000-node tree rendering must be < 100 ms");
    }
    console.log("-> SidebarPerformanceTest passed.");
}
//# sourceMappingURL=SidebarPerformanceTest.js.map