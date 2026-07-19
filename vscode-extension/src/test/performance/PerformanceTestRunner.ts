import { runExtensionPerformanceTests } from './ExtensionPerformanceTest';
import { runCommandPerformanceTests } from './CommandPerformanceTest';
import { runSidebarPerformanceTests } from './SidebarPerformanceTest';
import { runEditorPerformanceTests } from './EditorPerformanceTest';
import { runWebviewPerformanceTests } from './WebviewPerformanceTest';
import { runConfigurationPerformanceTests } from './ConfigurationPerformanceTest';

export async function runAllPerformanceTests() {
    console.log("=========================================");
    console.log("Running Memora VS Code Extension Performance Benchmarks");
    console.log("=========================================");
    await runExtensionPerformanceTests();
    await runCommandPerformanceTests();
    await runSidebarPerformanceTests();
    await runEditorPerformanceTests();
    await runWebviewPerformanceTests();
    await runConfigurationPerformanceTests();
    console.log("=========================================");
}
