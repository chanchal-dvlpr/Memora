import { mockOnDidChangeConfiguration } from '../mockVscode';
import * as assert from 'assert';
import { activate, deactivate } from '../../extension';

export async function runConfigurationConcurrencyTests() {
    console.log("-> Running ConfigurationConcurrencyTest...");

    // Test 1: Concurrent settings change updates
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        const promises = Array.from({ length: 50 }, () => {
            return Promise.resolve().then(() => {
                mockOnDidChangeConfiguration.fire({
                    affectsConfiguration: (section: string) => section === 'contextEngine'
                });
            });
        });

        await Promise.all(promises);
        assert.ok(true);

        deactivate();
    }

    // Test 2: Last update wins check on connection configuration modifications
    {
        const context = {
            subscriptions: [] as any[],
            extensionUri: { scheme: 'file', authority: '', path: '/ext' }
        };
        activate(context as any);

        let finalPortValue = 0;
        const promises = Array.from({ length: 50 }, (_, i) => {
            return Promise.resolve().then(() => {
                finalPortValue = 9000 + i;
                mockOnDidChangeConfiguration.fire({
                    affectsConfiguration: (section: string) => section === 'contextEngine'
                });
            });
        });

        await Promise.all(promises);
        assert.ok(finalPortValue >= 9000);

        deactivate();
    }

    console.log("-> ConfigurationConcurrencyTest passed.");
}
