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
exports.runGraphWebviewTests = runGraphWebviewTests;
require("./mockVscode");
const assert = __importStar(require("assert"));
const graphWebview_1 = require("../graphWebview");
async function runGraphWebviewTests() {
    console.log("-> Running GraphWebviewTest...");
    // Test 1: Panel creation and registration
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined, "Panel should be created and cached");
    }
    // Test 2: HTML generation incorporates strict CSP
    {
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        const html = panelInstance.getHtmlForWebview();
        assert.ok(html.includes("Content-Security-Policy"), "Should contain Content Security Policy meta tag");
        assert.ok(html.includes("default-src 'none'"), "CSP should restrict default loading to none");
        assert.ok(html.includes("vscode-resource"), "CSP should allow local resource schemes");
    }
    // Test 3: HTML inherits theme css variables
    {
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        const html = panelInstance.getHtmlForWebview();
        assert.ok(html.includes("var(--vscode-foreground)"), "Should use VS Code theme variables");
        assert.ok(html.includes("var(--vscode-editor-background)"), "Should use VS Code theme background");
        assert.ok(html.includes("var(--vscode-panel-border)"), "Should use VS Code border colors");
    }
    // Test 4: Offline placeholder loading message is present
    {
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        const html = panelInstance.getHtmlForWebview();
        assert.ok(html.includes("Interactive Network Graph Loading..."), "Should display loading placeholder");
    }
    // Test 5: Reopening panel reveals existing instance
    {
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        let revealCalled = false;
        const originalPanel = panelInstance.panel;
        panelInstance.panel = {
            reveal: () => { revealCalled = true; }
        };
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
        assert.strictEqual(revealCalled, true, "Reopen should reveal existing webview instance");
        panelInstance.panel = originalPanel;
    }
    // Test 6: dispose clears panel reference
    {
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        panelInstance.dispose();
        assert.strictEqual(graphWebview_1.GraphWebviewPanel.currentPanel, undefined, "Disposal should clear panel static caching");
    }
    // Test 7: Reopening after disposal creates a new panel
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        panelInstance.dispose();
    }
    // Test 8: WebView contains title header
    {
        const mockExtensionUri = { scheme: 'file', authority: '', path: '/ext' };
        graphWebview_1.GraphWebviewPanel.createOrShow(mockExtensionUri);
        const panelInstance = graphWebview_1.GraphWebviewPanel.currentPanel;
        assert.ok(panelInstance !== undefined);
        const html = panelInstance.getHtmlForWebview();
        assert.ok(html.includes("<h2>🕸️ Knowledge Graph Navigator</h2>"));
        panelInstance.dispose();
    }
    console.log("-> GraphWebviewTest passed.");
}
//# sourceMappingURL=GraphWebviewTest.js.map