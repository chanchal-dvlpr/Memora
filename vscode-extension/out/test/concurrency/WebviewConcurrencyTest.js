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
exports.runWebviewConcurrencyTests = runWebviewConcurrencyTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const graphWebview_1 = require("../../graphWebview");
async function runWebviewConcurrencyTests() {
    console.log("-> Running WebviewConcurrencyTest...");
    // Test 1: Concurrent panel creation and disposal
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        const promises = Array.from({ length: 20 }, async () => {
            graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
            const panel = graphWebview_1.GraphWebviewPanel.currentPanel;
            if (panel) {
                panel.dispose();
            }
        });
        await Promise.all(promises);
        assert.strictEqual(graphWebview_1.GraphWebviewPanel.currentPanel, undefined, "Disposal cleans panel reference");
    }
    // Test 2: 50 concurrent updates to panel options/state
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        const promises = Array.from({ length: 50 }, async () => {
            graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
            const panel = graphWebview_1.GraphWebviewPanel.currentPanel;
            assert.ok(panel !== undefined);
            panel.dispose();
        });
        await Promise.all(promises);
        assert.strictEqual(graphWebview_1.GraphWebviewPanel.currentPanel, undefined);
    }
    console.log("-> WebviewConcurrencyTest passed.");
}
//# sourceMappingURL=WebviewConcurrencyTest.js.map