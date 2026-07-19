import { runExtensionConcurrencyTests } from './ExtensionConcurrencyTest';
import { runCommandConcurrencyTests } from './CommandConcurrencyTest';
import { runSidebarConcurrencyTests } from './SidebarConcurrencyTest';
import { runEditorConcurrencyTests } from './EditorConcurrencyTest';
import { runWebviewConcurrencyTests } from './WebviewConcurrencyTest';
import { runConfigurationConcurrencyTests } from './ConfigurationConcurrencyTest';

export async function runAllConcurrencyTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Concurrency Tests");
    console.log("=========================================");
    await runExtensionConcurrencyTests();
    await runCommandConcurrencyTests();
    await runSidebarConcurrencyTests();
    await runEditorConcurrencyTests();
    await runWebviewConcurrencyTests();
    await runConfigurationConcurrencyTests();
    console.log("=========================================");
}
