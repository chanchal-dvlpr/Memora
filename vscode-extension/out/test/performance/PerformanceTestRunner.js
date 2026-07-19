"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runAllPerformanceTests = runAllPerformanceTests;
const ExtensionPerformanceTest_1 = require("./ExtensionPerformanceTest");
const CommandPerformanceTest_1 = require("./CommandPerformanceTest");
const SidebarPerformanceTest_1 = require("./SidebarPerformanceTest");
const EditorPerformanceTest_1 = require("./EditorPerformanceTest");
const WebviewPerformanceTest_1 = require("./WebviewPerformanceTest");
const ConfigurationPerformanceTest_1 = require("./ConfigurationPerformanceTest");
async function runAllPerformanceTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Performance Benchmarks");
    console.log("=========================================");
    await (0, ExtensionPerformanceTest_1.runExtensionPerformanceTests)();
    await (0, CommandPerformanceTest_1.runCommandPerformanceTests)();
    await (0, SidebarPerformanceTest_1.runSidebarPerformanceTests)();
    await (0, EditorPerformanceTest_1.runEditorPerformanceTests)();
    await (0, WebviewPerformanceTest_1.runWebviewPerformanceTests)();
    await (0, ConfigurationPerformanceTest_1.runConfigurationPerformanceTests)();
    console.log("=========================================");
}
//# sourceMappingURL=PerformanceTestRunner.js.map