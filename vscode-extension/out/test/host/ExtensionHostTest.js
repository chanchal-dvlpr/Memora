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
const assert = __importStar(require("assert"));
const vscode = __importStar(require("vscode"));
suite('ExtensionHostTest', () => {
    const extId = 'chanchal-dvlpr.memora-vscode';
    test('1. Extension should be discovered by Host', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined, "Extension must be discovered");
    });
    test('2. Extension ID should match manifest', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.id, extId, "Extension ID must match publisher.name");
    });
    test('3. Extension version is registered', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.version, '0.0.1');
    });
    test('4. Extension publisher matches publisher manifest', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.publisher, 'chanchal-dvlpr');
    });
    test('5. Extension category contains Other', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext?.packageJSON.categories.includes("Other"));
    });
    test('6. Extension main entrypoint file exists', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.main, './out/extension.js');
    });
    test('7. Extension description is loaded', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.packageJSON.description, "Local-first AI Context Engine for Visual Studio Code");
    });
    test('8. Extension activationEvents includes onStartupFinished', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext?.packageJSON.activationEvents.includes("onStartupFinished"));
    });
});
//# sourceMappingURL=ExtensionHostTest.js.map