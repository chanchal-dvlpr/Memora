"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runAllConcurrencyTests = runAllConcurrencyTests;
const ExtensionConcurrencyTest_1 = require("./ExtensionConcurrencyTest");
const CommandConcurrencyTest_1 = require("./CommandConcurrencyTest");
const SidebarConcurrencyTest_1 = require("./SidebarConcurrencyTest");
const EditorConcurrencyTest_1 = require("./EditorConcurrencyTest");
const WebviewConcurrencyTest_1 = require("./WebviewConcurrencyTest");
const ConfigurationConcurrencyTest_1 = require("./ConfigurationConcurrencyTest");
async function runAllConcurrencyTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Concurrency Tests");
    console.log("=========================================");
    await (0, ExtensionConcurrencyTest_1.runExtensionConcurrencyTests)();
    await (0, CommandConcurrencyTest_1.runCommandConcurrencyTests)();
    await (0, SidebarConcurrencyTest_1.runSidebarConcurrencyTests)();
    await (0, EditorConcurrencyTest_1.runEditorConcurrencyTests)();
    await (0, WebviewConcurrencyTest_1.runWebviewConcurrencyTests)();
    await (0, ConfigurationConcurrencyTest_1.runConfigurationConcurrencyTests)();
    console.log("=========================================");
}
//# sourceMappingURL=ConcurrencyTestRunner.js.map