import * as path from 'path';
import * as os from 'os';
import { runTests } from '@vscode/test-electron';

async function main() {
    try {
        const extensionDevelopmentPath = path.resolve(__dirname, '../../../');
        const extensionTestsPath = path.resolve(__dirname, './index');

        // Generate a short temporary user data directory to avoid domain socket path overflow (>103 chars)
        const userDataDir = path.join(os.tmpdir(), 'vsc-td');

        const workspacePath = path.resolve(__dirname, '../../../memora.code-workspace');

        await runTests({
            extensionDevelopmentPath,
            extensionTestsPath,
            launchArgs: [
                workspacePath,
                '--user-data-dir', userDataDir
            ]
        });
    } catch (err) {
        console.error('Failed to run tests', err);
        process.exit(1);
    }
}

main();
