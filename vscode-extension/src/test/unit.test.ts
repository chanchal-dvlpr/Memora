import './mockVscode';
import { runBackendClientTests } from './BackendClientTest';
import { runSidebarProviderTests } from './SidebarProviderTest';
import { runEditorIntegrationTests } from './EditorIntegrationTest';
import { runGraphWebviewTests } from './GraphWebviewTest';
import { runExtensionActivationTests } from './ExtensionActivationTest';

// Integration imports
import { runExtensionIntegrationTests } from './integration/ExtensionIntegrationTest';
import { runBackendIntegrationTests } from './integration/BackendIntegrationTest';
import { runSidebarIntegrationTests } from './integration/SidebarIntegrationTest';
import { runEditorIntegrationFlowTests } from './integration/EditorIntegrationFlowTest';
import { runWebviewIntegrationTests } from './integration/WebviewIntegrationTest';
import { runCommandIntegrationTests } from './integration/CommandIntegrationTest';
import { runOfflineRecoveryIntegrationTests } from './integration/OfflineRecoveryIntegrationTest';

async function main() {
    console.log("=========================================");
    console.log("Starting Memora VS Code Extension Tests");
    console.log("=========================================");

    try {
        console.log("--- UNIT TESTS ---");
        await runBackendClientTests();
        await runSidebarProviderTests();
        await runEditorIntegrationTests();
        await runGraphWebviewTests();
        await runExtensionActivationTests();

        console.log("--- INTEGRATION TESTS ---");
        await runExtensionIntegrationTests();
        await runBackendIntegrationTests();
        await runSidebarIntegrationTests();
        await runEditorIntegrationFlowTests();
        await runWebviewIntegrationTests();
        await runCommandIntegrationTests();
        await runOfflineRecoveryIntegrationTests();

        console.log("--- PERFORMANCE TESTS ---");
        const { runAllPerformanceTests } = require('./performance/PerformanceTestRunner');
        await runAllPerformanceTests();

        console.log("--- MEMORY TESTS ---");
        const { runAllMemoryTests } = require('./memory/MemoryLeakTestRunner');
        await runAllMemoryTests();

        console.log("--- CONCURRENCY TESTS ---");
        const { runAllConcurrencyTests } = require('./concurrency/ConcurrencyTestRunner');
        await runAllConcurrencyTests();

        console.log("=========================================");
        console.log("All VS Code Extension Tests Passed!");
        console.log("=========================================");
        process.exit(0);
    } catch (err) {
        console.error("Test execution failed:", err);
        process.exit(1);
    }
}

main();
