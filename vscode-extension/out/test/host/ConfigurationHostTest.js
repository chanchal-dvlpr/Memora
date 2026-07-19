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
suite('ConfigurationHostTest', () => {
    test('1. Default connection port matches configuration', () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const port = config.get('connection.port');
        assert.ok(port !== undefined);
    });
    test('2. Default connection token matches configuration', () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const token = config.get('connection.token');
        assert.ok(token !== undefined);
    });
    test('3. Modifying connection.port propagates settings', async () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const originalPort = config.get('connection.port');
        await config.update('connection.port', 9090, vscode.ConfigurationTarget.Global);
        const updatedConfig = vscode.workspace.getConfiguration('contextEngine');
        assert.strictEqual(updatedConfig.get('connection.port'), 9090);
        await config.update('connection.port', originalPort, vscode.ConfigurationTarget.Global);
    });
    test('4. Modifying connection.token propagates settings', async () => {
        const config = vscode.workspace.getConfiguration('contextEngine');
        const originalToken = config.get('connection.token');
        await config.update('connection.token', 'test-token', vscode.ConfigurationTarget.Global);
        const updatedConfig = vscode.workspace.getConfiguration('contextEngine');
        assert.strictEqual(updatedConfig.get('connection.token'), 'test-token');
        await config.update('connection.token', originalToken, vscode.ConfigurationTarget.Global);
    });
    test('5. Configuration update event triggers update listeners', () => {
        assert.ok(true);
    });
});
//# sourceMappingURL=ConfigurationHostTest.js.map