import { Command } from 'commander';
import { dispatcher } from './dispatcher';
import { projectApplicationService, resolveProjectForPathOrId, resolveRequiredProject } from '../services/project';
import { contextApplicationService } from '../services/context';
import { knowledgeApplicationService } from '../services/knowledge';
import { diagnosticsApplicationService } from '../services/diagnostics';
import { ValidationError } from '../errors/errors';
import { executeVersion } from './placeholders';
import { confirmPrompt } from '../utils/prompt';

export interface CommandOption {
  flags: string;
  description: string;
  defaultValue?: unknown;
}

export interface CommandConfig {
  name: string;
  description: string;
  arguments?: string;
  options?: CommandOption[];
  action?: (args: string[], options: Record<string, unknown>) => void | Promise<void>;
  subcommands?: CommandConfig[];
}
import { CommandContext } from './commandContext';

/**
 * Reusable helper coordinating project unregistration CLI commands.
 */
async function runUnregisterAction(
  args: string[],
  options: Record<string, unknown>,
  cmdCtx: CommandContext
): Promise<void> {
  const target = args[0] || cmdCtx.executionContext.workingDir;

  // Find if this path matches a registered project
  const projectsResult = await projectApplicationService.listProjects(
    cmdCtx.executionContext,
  );
  const found = resolveProjectForPathOrId(target, projectsResult.projects);

  if (!found) {
    throw new ValidationError('Project is not registered.');
  }

  const isForce = !!options.force;
  if (!isForce) {
    cmdCtx.outputService.write(`Project:\n${found.rootPath}\n`);
    const confirmed = await confirmPrompt('Are you sure? (y/N) ');
    if (!confirmed) {
      cmdCtx.outputService.write('Unregistration cancelled.\n');
      return;
    }
  }

  const projectRemovalResult = await projectApplicationService.removeProject(
    found.id,
    cmdCtx.executionContext,
  );

  cmdCtx.outputService.write(projectRemovalResult);

  cmdCtx.eventPublisher.publish({
    type: 'ProjectRemoved',
    timestamp: new Date(),
    payload: {
      projectId: found.id,
    },
  });
}

/**
 * Registry mapping containing every subcommand's metadata and callback.
 */
export const registeredCommands: CommandConfig[] = [
  {
    name: 'init',
    description: 'Register project directory',
    arguments: '[path]',
    options: [{ flags: '-n, --name <title>', description: 'Custom name for the project' }],
    action: async (args, options) => {
      await dispatcher.dispatch('init', args, options, async (cmdCtx) => {
        const project = await projectApplicationService.registerProject(
          args[0],
          options.name as string | undefined,
          cmdCtx.executionContext,
        );

        cmdCtx.outputService.write(project);

        cmdCtx.eventPublisher.publish({
          type: 'ProjectRegistered',
          timestamp: new Date(),
          payload: {
            projectId: project.project.id,
            name: project.project.name,
            rootPath: project.project.rootPath,
          },
        });
      });
    },
  },
  {
    name: 'unregister',
    description: 'Unregister a project workspace',
    arguments: '[path|id]',
    options: [{ flags: '--force', description: 'Skip confirmation prompt' }],
    action: async (args, options) => {
      await dispatcher.dispatch('unregister', args, options, async (cmdCtx) => {
        await runUnregisterAction(args, options, cmdCtx);
      });
    },
  },
  {
    name: 'projects',
    description: 'List all registered workspaces',
    action: async (args, options) => {
      await dispatcher.dispatch('projects', args, options, async (cmdCtx) => {
        const result = await projectApplicationService.listProjects(cmdCtx.executionContext);

        cmdCtx.outputService.write(result);

        cmdCtx.eventPublisher.publish({
          type: 'ProjectListed',
          timestamp: new Date(),
          payload: {
            projectCount: result.projects.length,
          },
        });
      });
    },
  },
  {
    name: 'project',
    description: 'Project management commands',
    subcommands: [
      {
        name: 'register',
        description: 'Register a project directory',
        arguments: '[path]',
        options: [{ flags: '-n, --name <title>', description: 'Custom name for the project' }],
        action: async (args, options) => {
          await dispatcher.dispatch('project register', args, options, async (cmdCtx) => {
            const project = await projectApplicationService.registerProject(
              args[0],
              options.name as string | undefined,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(project);

            cmdCtx.eventPublisher.publish({
              type: 'ProjectRegistered',
              timestamp: new Date(),
              payload: {
                projectId: project.project.id,
                name: project.project.name,
                rootPath: project.project.rootPath,
              },
            });
          });
        },
      },
      {
        name: 'list',
        description: 'List all registered projects',
        action: async (args, options) => {
          await dispatcher.dispatch('project list', args, options, async (cmdCtx) => {
            const result = await projectApplicationService.listProjects(cmdCtx.executionContext);

            cmdCtx.outputService.write(result);

            cmdCtx.eventPublisher.publish({
              type: 'ProjectListed',
              timestamp: new Date(),
              payload: {
                projectCount: result.projects.length,
              },
            });
          });
        },
      },
      {
        name: 'show',
        description: 'Show project details',
        arguments: '<id>',
        action: async (args, options) => {
          await dispatcher.dispatch('project show', args, options, async (cmdCtx) => {
            const project = await projectApplicationService.showProject(
              args[0],
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(project);

            cmdCtx.eventPublisher.publish({
              type: 'ProjectViewed',
              timestamp: new Date(),
              payload: {
                projectId: project.id,
              },
            });
          });
        },
      },
      {
        name: 'refresh',
        description: 'Trigger file scan and snapshot generation',
        arguments: '[path|id]',
        options: [
          { flags: '--full', description: 'Force a full scan instead of an incremental scan' },
          { flags: '--dry-run', description: 'Simulate the scan without committing snapshots' },
        ],
        action: async (args, options) => {
          await dispatcher.dispatch('project refresh', args, options, async (cmdCtx) => {
            const target = args[0] || cmdCtx.executionContext.workingDir;
            const projectsResult = await projectApplicationService.listProjects(
              cmdCtx.executionContext,
            );

            const found = resolveProjectForPathOrId(target, projectsResult.projects);

            if (!found) {
              throw new ValidationError(`No registered project found matching: ${target}`);
            }

            const refreshResult = await projectApplicationService.refreshProject(
              found.id,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(refreshResult);

            cmdCtx.eventPublisher.publish({
              type: 'ProjectRefreshed',
              timestamp: new Date(),
              payload: {
                projectId: refreshResult.projectId,
                filesScanned: refreshResult.filesScanned,
                snapshotGenerated: refreshResult.snapshotGenerated,
              },
            });
          });
        },
      },
      {
        name: 'remove',
        description: 'Remove a project workspace registration',
        arguments: '<id>',
        options: [
          { flags: '--force', description: 'Force removal without prompting' },
        ],
        action: async (args, options) => {
          await dispatcher.dispatch('project remove', args, options, async (cmdCtx) => {
            const id = args[0];
            const removeResult = await projectApplicationService.removeProject(
              id,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(removeResult);

            cmdCtx.eventPublisher.publish({
              type: 'ProjectRemoved',
              timestamp: new Date(),
              payload: {
                projectId: removeResult.projectId,
              },
            });
          });
        },
      },
      {
        name: 'unregister',
        description: 'Unregister a project directory',
        arguments: '[path|id]',
        options: [{ flags: '--force', description: 'Skip confirmation prompt' }],
        action: async (args, options) => {
          await dispatcher.dispatch('project unregister', args, options, async (cmdCtx) => {
            await runUnregisterAction(args, options, cmdCtx);
          });
        },
      },
    ],
  },
  {
    name: 'context',
    description: 'Context management commands',
    subcommands: [
      {
        name: 'generate',
        description: 'Generate AI context for the target project',
        arguments: '[projectId]',
        action: async (args, options) => {
          await dispatcher.dispatch('context generate', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const { result, session } = await contextApplicationService.generateContext(
              projectId,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write({ result, session });

            cmdCtx.eventPublisher.publish({
              type: 'ContextGenerated',
              timestamp: new Date(),
              payload: { projectId: result.projectId },
            });
          });
        },
      },
      {
        name: 'show',
        description: 'Show latest generated context',
        arguments: '[projectId]',
        action: async (args, options) => {
          await dispatcher.dispatch('context show', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const contextResult = await contextApplicationService.getContext(
              projectId,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(contextResult);

            cmdCtx.eventPublisher.publish({
              type: 'ContextViewed',
              timestamp: new Date(),
              payload: { projectId: contextResult.projectId },
            });
          });
        },
      },
      {
        name: 'export',
        description: 'Export latest context to stdout or file',
        arguments: '[projectId]',
        options: [
          {
            flags: '-f, --format <format>',
            description: 'Export format: markdown or json',
            defaultValue: 'markdown',
          },
        ],
        action: async (args, options) => {
          await dispatcher.dispatch('context export', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const contextResult = await contextApplicationService.getContext(
              projectId,
              cmdCtx.executionContext,
            );
            const format = (options.format as string || 'markdown').toLowerCase();
            if (format !== 'json' && format !== 'markdown') {
              throw new ValidationError(`Unsupported export format: ${format}`);
            }

            cmdCtx.outputService.write({
              projectId: contextResult.projectId,
              content: contextResult.content,
              updatedAt: contextResult.updatedAt,
              format
            });

            cmdCtx.eventPublisher.publish({
              type: 'ContextExported',
              timestamp: new Date(),
              payload: { projectId: contextResult.projectId, format },
            });
          });
        },
      },
      {
        name: 'refresh',
        description: 'Refresh the target project context',
        arguments: '[projectId]',
        action: async (args, options) => {
          await dispatcher.dispatch('context refresh', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const refreshResult = await contextApplicationService.refreshContext(
              projectId,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(refreshResult);
          });
        },
      },
      {
        name: 'delete',
        description: 'Delete project context snapshot',
        arguments: '[projectId]',
        action: async (args, options) => {
          await dispatcher.dispatch('context delete', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const deleteResult = await contextApplicationService.deleteContext(
              projectId,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(deleteResult);
          });
        },
      },
    ],
  },
  {
    name: 'knowledge',
    description: 'Knowledge management commands',
    subcommands: [
      {
        name: 'search',
        description: 'Search project knowledge base',
        arguments: '<query> [projectId]',
        options: [
          { flags: '-l, --limit <count>', description: 'Limit search results count', defaultValue: 10 },
        ],
        action: async (args, options) => {
          await dispatcher.dispatch('knowledge search', args, options, async (cmdCtx) => {
            const queryText = args[0];
            const projectId = await resolveRequiredProject(args[1], cmdCtx.executionContext);

            const query = knowledgeApplicationService.createQuery(
              queryText,
              projectId,
              Number(options.limit),
            );
            const searchResult = await knowledgeApplicationService.searchKnowledge(
              query,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(searchResult);
          });
        },
      },
      {
        name: 'show',
        description: 'Show detailed knowledge item',
        arguments: '<id>',
        action: async (args, options) => {
          await dispatcher.dispatch('knowledge show', args, options, async (cmdCtx) => {
            const id = args[0];
            const result = await knowledgeApplicationService.showKnowledge(
              id,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'explain',
        description: 'Display explanation for a knowledge item',
        arguments: '<id>',
        action: async (args, options) => {
          await dispatcher.dispatch('knowledge explain', args, options, async (cmdCtx) => {
            const id = args[0];
            const result = await knowledgeApplicationService.explainKnowledge(
              id,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'refresh',
        description: 'Refresh project knowledge base',
        arguments: '[projectId]',
        action: async (args, options) => {
          await dispatcher.dispatch('knowledge refresh', args, options, async (cmdCtx) => {
            const projectId = await resolveRequiredProject(args[0], cmdCtx.executionContext);

            const refreshResult = await knowledgeApplicationService.refreshKnowledge(
              projectId,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(refreshResult);
          });
        },
      },
      {
        name: 'delete',
        description: 'Delete project knowledge item',
        arguments: '<id>',
        action: async (args, options) => {
          await dispatcher.dispatch('knowledge delete', args, options, async (cmdCtx) => {
            const id = args[0];
            const deleteResult = await knowledgeApplicationService.deleteKnowledge(
              id,
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(deleteResult);
          });
        },
      },
    ],
  },
  {
    name: 'diagnostics',
    description: 'CLI operational visibility and check tools',
    subcommands: [
      {
        name: 'health',
        description: 'Verify backend connection status and daemon compatibility',
        action: async (args, options) => {
          await dispatcher.dispatch('diagnostics health', args, options, async (cmdCtx) => {
            const result = await diagnosticsApplicationService.runHealthChecks(
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'config',
        description: 'Verify CLI configuration options loaded and contain valid values',
        action: async (args, options) => {
          await dispatcher.dispatch('diagnostics config', args, options, async (cmdCtx) => {
            const result = await diagnosticsApplicationService.runConfigChecks(
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'connectivity',
        description: 'Verify backend reachable and responds successfully',
        action: async (args, options) => {
          await dispatcher.dispatch('diagnostics connectivity', args, options, async (cmdCtx) => {
            const result = await diagnosticsApplicationService.runConnectivityChecks(
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'environment',
        description: 'Verify operating system platform and Node runtime properties',
        action: async (args, options) => {
          await dispatcher.dispatch('diagnostics environment', args, options, async (cmdCtx) => {
            const result = await diagnosticsApplicationService.runEnvironmentChecks(
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
      {
        name: 'report',
        description: 'Generate unified diagnostics report of all check suites',
        action: async (args, options) => {
          await dispatcher.dispatch('diagnostics report', args, options, async (cmdCtx) => {
            const result = await diagnosticsApplicationService.generateReport(
              cmdCtx.executionContext,
            );

            cmdCtx.outputService.write(result);
          });
        },
      },
    ],
  },
  {
    name: 'refresh',
    description: 'Trigger file scan and snapshot generation',
    arguments: '[path|id]',
    options: [
      { flags: '--full', description: 'Force a full scan instead of an incremental scan' },
      { flags: '--dry-run', description: 'Simulate the scan without committing snapshots' },
    ],
    action: async (args, options) => {
      await dispatcher.dispatch('refresh', args, options, async (cmdCtx) => {
        const target = args[0] || cmdCtx.executionContext.workingDir;
        const projectsResult = await projectApplicationService.listProjects(
          cmdCtx.executionContext,
        );

        const found = resolveProjectForPathOrId(target, projectsResult.projects);

        if (!found) {
          throw new ValidationError(`No registered project found matching: ${target}`);
        }

        const refreshResult = await projectApplicationService.refreshProject(
          found.id,
          cmdCtx.executionContext,
        );

        cmdCtx.outputService.write(refreshResult);

        cmdCtx.eventPublisher.publish({
          type: 'ProjectRefreshed',
          timestamp: new Date(),
          payload: {
            projectId: refreshResult.projectId,
            filesScanned: refreshResult.filesScanned,
            snapshotGenerated: refreshResult.snapshotGenerated,
          },
        });
      });
    },
  },
  {
    name: 'handoff',
    description: 'Export latest context snapshot markdown',
    arguments: '[path|id]',
    options: [{ flags: '-s, --stdout', description: 'Output directly to terminal stdout' }],
    action: async (args, options) => {
      await dispatcher.dispatch('handoff', args, options, async (cmdCtx) => {
        const target = args[0] || cmdCtx.executionContext.workingDir;
        const projectsResult = await projectApplicationService.listProjects(
          cmdCtx.executionContext,
        );

        const found = resolveProjectForPathOrId(target, projectsResult.projects);

        if (!found) {
          throw new ValidationError(`No registered project found matching: ${target}`);
        }

        const contextResult = await contextApplicationService.getContext(
          found.id,
          cmdCtx.executionContext,
        );

        const isStdout = !!options.stdout;
        if (isStdout) {
          console.log(contextResult.content);
        } else {
          cmdCtx.outputService.write({
            projectId: contextResult.projectId,
            content: contextResult.content,
            updatedAt: contextResult.updatedAt,
            format: 'markdown'
          });
        }

        cmdCtx.eventPublisher.publish({
          type: 'ContextExported',
          timestamp: new Date(),
          payload: { projectId: contextResult.projectId, format: 'markdown' },
        });
      });
    },
  },
  {
    name: 'search',
    description: 'Query symbols or directories context',
    arguments: '<query> [path|id]',
    options: [{ flags: '-l, --limit <count>', description: 'Limit results count', defaultValue: 10 }],
    action: async (args, options) => {
      await dispatcher.dispatch('search', args, options, async (cmdCtx) => {
        const queryText = args[0];
        const target = args[1] || cmdCtx.executionContext.workingDir;
        const projectsResult = await projectApplicationService.listProjects(
          cmdCtx.executionContext,
        );

        const found = resolveProjectForPathOrId(target, projectsResult.projects);

        if (!found) {
          throw new ValidationError(`No registered project found matching: ${target}`);
        }

        const query = knowledgeApplicationService.createQuery(
          queryText,
          found.id,
          Number(options.limit),
        );
        const searchResult = await knowledgeApplicationService.searchKnowledge(
          query,
          cmdCtx.executionContext,
        );

        cmdCtx.outputService.write(searchResult);
      });
    },
  },
  {
    name: 'doctor',
    description: 'Run system checks and diagnostic checks',
    action: async (args, options) => {
      await dispatcher.dispatch('doctor', args, options, async (cmdCtx) => {
        const result = await diagnosticsApplicationService.runHealthChecks(
          cmdCtx.executionContext,
        );

        cmdCtx.outputService.write(result);
      });
    },
  },
  {
    name: 'status',
    description: 'Check backend connection status',
    action: async (args, options) => {
      await dispatcher.dispatch('status', args, options, async (cmdCtx) => {
        const result = await diagnosticsApplicationService.runConnectivityChecks(
          cmdCtx.executionContext,
        );

        cmdCtx.outputService.write(result);
      });
    },
  },
  {
    name: 'version',
    description: 'Print version metadata',
    action: async (args, options) => {
      await dispatcher.dispatch('version', args, options, (cmdCtx) => {
        executeVersion(cmdCtx.globalOptions.json);
      });
    },
  },
];

/**
 * Loops and dynamically registers subcommands to the Commander program instance.
 */
export function registerAllCommands(program: Command): void {
  const registerCmd = (parent: Command, cmdConfig: CommandConfig) => {
    const cmd = parent.command(cmdConfig.name).description(cmdConfig.description);

    if (cmdConfig.arguments) {
      cmd.arguments(cmdConfig.arguments);
    }

    if (cmdConfig.options) {
      for (const opt of cmdConfig.options) {
        cmd.option(
          opt.flags,
          opt.description,
          opt.defaultValue as string | boolean | string[] | undefined,
        );
      }
    }

    if (cmdConfig.subcommands) {
      for (const sub of cmdConfig.subcommands) {
        registerCmd(cmd, sub);
      }
    }

    if (cmdConfig.action) {
      cmd.action((...parsedArgs: unknown[]) => {
        const globalOpts = program.opts();
        const localOpts = (parsedArgs[parsedArgs.length - 2] as Record<string, unknown>) || {};
        const combinedOpts = { ...globalOpts, ...localOpts };
        const positionalArgs = parsedArgs.slice(0, parsedArgs.length - 2) as string[];

        return Promise.resolve(cmdConfig.action!(positionalArgs, combinedOpts)).catch((err) => {
          process.stderr.write(`Execution failed: ${err.message}\n`);
        });
      });
    }
  };

  for (const cmdConfig of registeredCommands) {
    registerCmd(program, cmdConfig);
  }
}
