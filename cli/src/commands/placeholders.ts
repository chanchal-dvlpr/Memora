import { VersionProvider } from '../utils/versionProvider';

function handlePlaceholder(commandName: string, isJson: boolean): void {
  if (isJson) {
    console.log(
      JSON.stringify(
        {
          success: true,
          command: commandName,
          message: 'This feature will be implemented in a later phase.',
        },
        null,
        2,
      ),
    );
  } else {
    console.log(`[memora ${commandName}] This feature will be implemented in a later phase.`);
  }
}

export function executeInit(isJson: boolean): void {
  handlePlaceholder('init', isJson);
}

export function executeProjects(isJson: boolean): void {
  handlePlaceholder('projects', isJson);
}

export function executeRefresh(isJson: boolean): void {
  handlePlaceholder('refresh', isJson);
}

export function executeHandoff(isJson: boolean): void {
  handlePlaceholder('handoff', isJson);
}

export function executeSearch(isJson: boolean): void {
  handlePlaceholder('search', isJson);
}

export function executeDoctor(isJson: boolean): void {
  handlePlaceholder('doctor', isJson);
}

export function executeStatus(isJson: boolean): void {
  handlePlaceholder('status', isJson);
}

export function executeVersion(isJson: boolean): void {
  if (isJson) {
    console.log(
      JSON.stringify(
        {
          cliVersion: VersionProvider.getVersionString(),
        },
        null,
        2,
      ),
    );
  } else {
    console.log(`Memora CLI v${VersionProvider.getVersionString()} (Build: 2026-07-18)`);
  }
}
