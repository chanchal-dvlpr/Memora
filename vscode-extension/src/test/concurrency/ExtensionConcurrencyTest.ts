import '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runExtensionConcurrencyTests() {
    console.log("-> Running ExtensionConcurrencyTest...");

    // Test 1: 10 concurrent activations
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 10 }, () => {
            return Promise.resolve().then(() => activate(context as any));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }

    // Test 2: 30 concurrent activations
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 30 }, () => {
            return Promise.resolve().then(() => activate(context as any));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }

    // Test 3: 50 concurrent activations
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => activate(context as any));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }

    // Test 4: 100 concurrent activations
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        const promises = Array.from({ length: 100 }, () => {
            return Promise.resolve().then(() => activate(context as any));
        });
        await Promise.all(promises);
        assert.ok(context.subscriptions.length > 0);
    }

    // Test 5: Concurrent deactivations
    {
        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => deactivate());
        });
        await Promise.all(promises);
        assert.ok(true);
    }

    console.log("-> ExtensionConcurrencyTest passed.");
}
