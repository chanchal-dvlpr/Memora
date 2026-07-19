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
exports.runExtensionConcurrencyTests = runExtensionConcurrencyTests;
require("../mockVscode");
const assert = __importStar(require("assert"));
const extension_1 = require("../../extension");
async function runExtensionConcurrencyTests() {
    console.log("-> Running ExtensionConcurrencyTest...");
    // Test 1: 10 concurrent activations
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 10 }, () => {
            return Promise.resolve().then(() => (0, extension_1.activate)(context));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }
    // Test 2: 30 concurrent activations
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 30 }, () => {
            return Promise.resolve().then(() => (0, extension_1.activate)(context));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }
    // Test 3: 50 concurrent activations
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => (0, extension_1.activate)(context));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }
    // Test 4: 100 concurrent activations
    {
        const context = {
            subscriptions: [],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 100 }, () => {
            return Promise.resolve().then(() => (0, extension_1.activate)(context));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }
    // Test 5: Concurrent deactivations
    {
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => (0, extension_1.deactivate)());
        });
        await Promise.all(promises);
        assert.ok(true);
    }
    console.log("-> ExtensionConcurrencyTest passed.");
}
//# sourceMappingURL=ExtensionConcurrencyTest.js.map