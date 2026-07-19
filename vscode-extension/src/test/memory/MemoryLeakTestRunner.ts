import { runExtensionMemoryTests } from './ExtensionMemoryTest';
import { runCommandMemoryTests } from './CommandMemoryTest';
import { runSidebarMemoryTests } from './SidebarMemoryTest';
import { runEditorMemoryTests } from './EditorMemoryTest';
import { runWebviewMemoryTests } from './WebviewMemoryTest';
import { runConfigurationMemoryTests } from './ConfigurationMemoryTest';

export async function runAllMemoryTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Memory Leak Diagnostics");
    console.log("=========================================");
    await runExtensionMemoryTests();
    await runCommandMemoryTests();
    await runSidebarMemoryTests();
    await runEditorMemoryTests();
    await runWebviewMemoryTests();
    await runConfigurationMemoryTests();
    console.log("=========================================");
}
