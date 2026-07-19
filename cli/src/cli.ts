import { Command } from 'commander';
import { registerAllCommands } from './commands/register';
import { configService } from './config/service';
import { ConfigurationError } from './errors/errors';
import { VersionProvider } from './utils/versionProvider';

const BANNER = `
███╗   ███╗███████╗███╗   ███╗ ██████╗ ██████╗  █████╗
████╗ ████║██╔════╝████╗ ████║██╔═══██╗██╔══██╗██╔══██╗
██╔████╔██║█████╗  ██╔████╔██║██║   ██║██████╔╝███████║
██║╚██╔╝██║██╔═══╝  ██║╚██╔╝██║██║   ██║██╔══██╗██╔══██║
██║ ╚═╝ ██║███████╗██║ ╚═╝ ██║╚██████╔╝██║  ██║██║  ██║
╚═╝     ╚═╝╚══════╝╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝
Memora CLI Context Engine Client Utility
`;

let lastExitCode = 0;

export function setExitCode(code: number): void {
  lastExitCode = code;
}

export function getExitCode(): number {
  return lastExitCode;
}

/**
 * Initializes Commander, hooks global options and subcommand listeners,
 * and routes execution args.
 */
export async function run(args: string[]): Promise<number> {
  // Reset exit code for this execution run
  setExitCode(0);

  // Pre-parse global flags to initialize configService
  const tempProgram = new Command();
  tempProgram
    .allowUnknownOption(true)
    .option('-j, --json', 'Format output as structured JSON')
    .option('--verbose', 'Output detailed trace diagnostic logs to stderr')
    .option('-q, --quiet', 'Suppress all stdout notifications')
    .option('-c, --config <path>', 'Use custom configuration file')
    .helpOption(false)
    .exitOverride();

  let globalOpts: Record<string, unknown> = {};
  try {
    tempProgram.parse(args);
    globalOpts = tempProgram.opts();
  } catch (err) {
    // Ignore parameter errors; they will be parsed in detail by the main program
  }

  // Load active configuration settings
  try {
    await configService.load(globalOpts);
  } catch (err: unknown) {
    if (err instanceof ConfigurationError) {
      console.error(err.message);
      return err.exitCode;
    }
    const msg = err instanceof Error ? err.message : String(err);
    console.error(`Error: Failed to load config: ${msg}`);
    return 3;
  }

  const program = new Command();

  program
    .name('memora')
    .description('Memora Command Line Interface')
    .addHelpText('before', BANNER)
    .exitOverride(); // Throws errors instead of process.exit()

  // Configure global options
  program
    .option('-j, --json', 'Format output as structured JSON')
    .option('--verbose', 'Output detailed trace diagnostic logs to stderr')
    .option('-q, --quiet', 'Suppress all stdout notifications')
    .option('-c, --config <path>', 'Use custom configuration file');

  // Register version configurations
  program.version(VersionProvider.getVersionString(), '-v, --version', 'Print CLI version');

  // Configure help configurations
  program.helpOption('-h, --help', 'Print help menu');

  // Register subcommands dynamically from matrix map
  registerAllCommands(program);

  // Check if quiet / version override should intercept
  const rawArgs = args.slice(2);
  const isJson = rawArgs.includes('-j') || rawArgs.includes('--json');
  const isQuiet = rawArgs.includes('-q') || rawArgs.includes('--quiet');
  const isVerbose = rawArgs.includes('--verbose');

  if (isQuiet) {
    console.log = () => {};
  }

  if (isVerbose) {
    const configIdx =
      rawArgs.indexOf('-c') !== -1 ? rawArgs.indexOf('-c') : rawArgs.indexOf('--config');
    const customConfig =
      configIdx !== -1 && configIdx + 1 < rawArgs.length ? rawArgs[configIdx + 1] : 'Default';
    console.error(`[VERBOSE] Custom Config: ${customConfig}`);
  }

  // Intercept help queries for custom JSON and banner formatting
  if (rawArgs.includes('-h') || rawArgs.includes('--help') || rawArgs.length === 0) {
    printHelp(isJson);
    return 0;
  }

  // Intercept version queries for detailed system printouts
  if (rawArgs.includes('-v') || rawArgs.includes('--version') || rawArgs.includes('version')) {
    if (isJson) {
      console.log(
        JSON.stringify(
          {
            cliVersion: VersionProvider.getVersionString(),
            nodeVersion: process.version,
            platform: process.platform,
            arch: process.arch,
          },
          null,
          2,
        ),
      );
    } else {
      console.log(`Memora CLI v${VersionProvider.getVersionString()}
Node: ${process.version}
Platform: ${process.platform}
Arch: ${process.arch}`);
    }
    return 0;
  }

  try {
    await program.parseAsync(args);
    return getExitCode();
  } catch (error: unknown) {
    const err = error as { code?: string };
    if (err.code === 'commander.helpDisplayed') {
      return 0;
    }
    if (err.code === 'commander.version') {
      return 0;
    }
    // Return syntax/parse error code 6
    return 6;
  }
}

/**
 * Print CLI help menu guide.
 */
function printHelp(isJson: boolean): void {
  if (isJson) {
    console.log(
      JSON.stringify(
        {
          usage: 'memora <subcommand> [options]',
          globalOptions: {
            '--json, -j': 'Format output as structured JSON',
            '--verbose': 'Output detailed trace diagnostic logs to stderr',
            '--quiet, -q': 'Suppress all stdout notifications',
            '--config, -c <path>': 'Use custom configuration file',
            '--help, -h': 'Print help menu',
            '--version, -v': 'Print CLI version',
          },
          subcommands: [
            'init',
            'projects',
            'refresh',
            'handoff',
            'search',
            'doctor',
            'status',
            'version',
          ],
        },
        null,
        2,
      ),
    );
    return;
  }

  console.log(`
███╗   ███╗███████╗███╗   ███╗ ██████╗ ██████╗  █████╗
████╗ ████║██╔════╝████╗ ████║██╔═══██╗██╔══██╗██╔══██╗
██╔████╔██║█████╗  ██╔████╔██║██║   ██║██████╔╝███████║
██║╚██╔╝██║██╔═══╝  ██║╚██╔╝██║██║   ██║██╔══██╗██╔══██║
██║ ╚═╝ ██║███████╗██║ ╚═╝ ██║╚██████╔╝██║  ██║██║  ██║
╚═╝     ╚═╝╚══════╝╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝
Memora CLI Context Engine Client Utility

Usage:
  memora [options] [command]

Global Options:
  -j, --json          Format output as structured JSON
  --verbose           Output detailed trace diagnostic logs to stderr
  -q, --quiet         Suppress all stdout notifications
  -c, --config <path> Use custom configuration file
  -h, --help          Print help menu
  -v, --version       Print CLI version

Subcommands:
  init                Register project directory
  projects            List all registered workspaces
  refresh             Trigger file scan and snapshot generation
  handoff             Export latest context snapshot markdown
  search              Query symbols or directories context
  doctor              Run system checks and diagnostic checks
  status              Check backend connection status
  version             Print version metadata
`);
}
