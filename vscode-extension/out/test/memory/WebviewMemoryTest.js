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
exports.runWebviewMemoryTests = runWebviewMemoryTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const graphWebview_1 = require("../../graphWebview");
async function runWebviewMemoryTests() {
    console.log("-> Running WebviewMemoryTest...");
    const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
    // Test 1: 100 Webview panel creation/dispose cycles
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
            const panel = graphWebview_1.GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            panel.dispose();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 webview panels: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }
    // Test 2: 500 Webview panel creation/dispose cycles (Long run check)
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
            const panel = graphWebview_1.GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            panel.dispose();
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 webview panels: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5, "500 panel creations must release memory clean without leaks");
    }
    console.log("-> WebviewMemoryTest passed.");
}
//# sourceMappingURL=WebviewMemoryTest.js.map