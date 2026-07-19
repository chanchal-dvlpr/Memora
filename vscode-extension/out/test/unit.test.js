"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
require("./mockVscode");
const BackendClientTest_1 = require("./BackendClientTest");
const SidebarProviderTest_1 = require("./SidebarProviderTest");
const EditorIntegrationTest_1 = require("./EditorIntegrationTest");
const GraphWebviewTest_1 = require("./GraphWebviewTest");
const ExtensionActivationTest_1 = require("./ExtensionActivationTest");
// Integration imports
const ExtensionIntegrationTest_1 = require("./integration/ExtensionIntegrationTest");
const BackendIntegrationTest_1 = require("./integration/BackendIntegrationTest");
const SidebarIntegrationTest_1 = require("./integration/SidebarIntegrationTest");
const EditorIntegrationFlowTest_1 = require("./integration/EditorIntegrationFlowTest");
const WebviewIntegrationTest_1 = require("./integration/WebviewIntegrationTest");
const CommandIntegrationTest_1 = require("./integration/CommandIntegrationTest");
const OfflineRecoveryIntegrationTest_1 = require("./integration/OfflineRecoveryIntegrationTest");
async function main() {
    console.log("=========================================");
    console.log("Starting Memora VS Code Extension Tests");
    console.log("=========================================");
    try {
        console.log("--- UNIT TESTS ---");
        await (0, BackendClientTest_1.runBackendClientTests)();
        await (0, SidebarProviderTest_1.runSidebarProviderTests)();
        await (0, EditorIntegrationTest_1.runEditorIntegrationTests)();
        await (0, GraphWebviewTest_1.runGraphWebviewTests)();
        await (0, ExtensionActivationTest_1.runExtensionActivationTests)();
        console.log("--- INTEGRATION TESTS ---");
        await (0, ExtensionIntegrationTest_1.runExtensionIntegrationTests)();
        await (0, BackendIntegrationTest_1.runBackendIntegrationTests)();
        await (0, SidebarIntegrationTest_1.runSidebarIntegrationTests)();
        await (0, EditorIntegrationFlowTest_1.runEditorIntegrationFlowTests)();
        await (0, WebviewIntegrationTest_1.runWebviewIntegrationTests)();
        await (0, CommandIntegrationTest_1.runCommandIntegrationTests)();
        await (0, OfflineRecoveryIntegrationTest_1.runOfflineRecoveryIntegrationTests)();
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
    }
    catch (err) {
        console.error("Test execution failed:", err);
        process.exit(1);
    }
}
main();
//# sourceMappingURL=unit.test.js.map