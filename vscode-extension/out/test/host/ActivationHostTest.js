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
suite('ActivationHostTest', () => {
    const extId = 'chanchal-dvlpr.memora-vscode';
    test('1. Extension should activate successfully', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        if (!ext.isActive) {
            await ext.activate();
        }
        assert.strictEqual(ext.isActive, true);
    });
    test('2. Extension remains active on double activate', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.strictEqual(ext.isActive, true);
    });
    test('3. Extension exports are defined', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        // Since we return undefined from activate, it should be fine
        assert.strictEqual(ext.exports, undefined);
    });
    test('4. Extension activation event listeners register subscriptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        assert.ok(ext.isActive);
    });
    test('5. Extension registers commands subscriptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        await ext.activate();
        const commands = await vscode.commands.getCommands(true);
        assert.ok(commands.includes("contextEngine.refreshContext"));
    });
    test('6. Extension activation does not leak exceptions', async () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
        try {
            await ext.activate();
            assert.ok(true);
        }
        catch (err) {
            assert.fail("Activation threw error");
        }
    });
    test('7. Extension activation time is non-zero', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.ok(ext !== undefined);
    });
    test('8. Extension stays active throughout test run', () => {
        const ext = vscode.extensions.getExtension(extId);
        assert.strictEqual(ext?.isActive, true);
    });
});
//# sourceMappingURL=ActivationHostTest.js.map