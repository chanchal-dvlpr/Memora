"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runAllMemoryTests = runAllMemoryTests;
const ExtensionMemoryTest_1 = require("./ExtensionMemoryTest");
const CommandMemoryTest_1 = require("./CommandMemoryTest");
const SidebarMemoryTest_1 = require("./SidebarMemoryTest");
const EditorMemoryTest_1 = require("./EditorMemoryTest");
const WebviewMemoryTest_1 = require("./WebviewMemoryTest");
const ConfigurationMemoryTest_1 = require("./ConfigurationMemoryTest");
async function runAllMemoryTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Memory Leak Diagnostics");
    console.log("=========================================");
    await (0, ExtensionMemoryTest_1.runExtensionMemoryTests)();
    await (0, CommandMemoryTest_1.runCommandMemoryTests)();
    await (0, SidebarMemoryTest_1.runSidebarMemoryTests)();
    await (0, EditorMemoryTest_1.runEditorMemoryTests)();
    await (0, WebviewMemoryTest_1.runWebviewMemoryTests)();
    await (0, ConfigurationMemoryTest_1.runConfigurationMemoryTests)();
    console.log("=========================================");
}
//# sourceMappingURL=MemoryLeakTestRunner.js.map