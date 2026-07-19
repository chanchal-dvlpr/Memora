import * as fs from 'fs';
import * as path from 'path';
import { ProjectApiClient, CreateProjectRequest } from '../api/project';
import { httpClientService } from '../http/clientService';
import { ExecutionContext } from '../models/context';
import { ValidationError } from '../errors/errors';
import {
  ProjectDetails,
  ProjectListResult,
  ProjectRefreshResult,
  ProjectRegistrationResult,
  ProjectRemovalResult,
} from '../models/projectResult';

/**
 * Orchestrates CLI project management workflows, input validations, and client integrations.
 */
export class ProjectApplicationService {
  constructor(private readonly projectApiClient: ProjectApiClient) {}

  /**
   * Resolves target paths and registers workspaces via ProjectApiClient.
   */
  public async registerProject(
    customPath: string | undefined,
    customName: string | undefined,
    ctx: ExecutionContext,
  ): Promise<ProjectRegistrationResult> {
    const targetPath = customPath || ctx.workingDir;
    const resolvedPath = path.resolve(targetPath);

    try {
      const stats = fs.statSync(resolvedPath);
      if (!stats.isDirectory()) {
        throw new ValidationError(`Path is not a directory: ${resolvedPath}`);
      }
    } catch (err: unknown) {
      if (err instanceof ValidationError) throw err;
      throw new ValidationError(`Directory does not exist: ${resolvedPath}`);
    }

    const name = customName || path.basename(resolvedPath) || 'unnamed-project';

    const req: CreateProjectRequest = {
      name,
      rootPath: resolvedPath,
    };

    const res = await this.projectApiClient.createProject(req, ctx);
    return {
      success: true,
      project: {
        id: res.id,
        name: res.name,
        rootPath: res.rootPath,
      },
    };
  }

  /**
   * Retrieves all registered workspaces.
   */
  public async listProjects(ctx: ExecutionContext): Promise<ProjectListResult> {
    const res = await this.projectApiClient.listProjects(ctx);
    return {
      projects: res.map((p) => ({
        id: p.id,
        name: p.name,
        rootPath: p.rootPath,
      })),
    };
  }

  /**
   * Returns details of a specific project by id.
   */
  public async showProject(id: string, ctx: ExecutionContext): Promise<ProjectDetails> {
    const listResult = await this.listProjects(ctx);
    const found = listResult.projects.find((p) => p.id === id);
    if (!found) {
      throw new ValidationError(`Project with ID ${id} not found`);
    }
    return {
      id: found.id,
      name: found.name,
      rootPath: found.rootPath,
    };
  }

  /**
   * Triggers project file scanning and snapshot update actions.
   */
  public async refreshProject(id: string, ctx: ExecutionContext): Promise<ProjectRefreshResult> {
    const res = await this.projectApiClient.refreshProject(id, ctx);
    return {
      success: true,
      projectId: res.projectId,
      filesScanned: res.filesScanned,
      snapshotGenerated: res.snapshotGenerated,
    };
  }

  /**
   * Removes a project registry reference.
   */
  public async removeProject(id: string, ctx: ExecutionContext): Promise<ProjectRemovalResult> {
    let rootPath: string | undefined;
    try {
      const listResult = await this.listProjects(ctx);
      const found = listResult.projects.find((p) => p.id === id);
      if (found) {
        rootPath = found.rootPath;
      }
    } catch {
      // Ignore list failure and proceed with deletion
    }

    const res = await this.projectApiClient.removeProject(id, ctx);
    return {
      type: 'ProjectRemovalResult',
      success: true,
      projectId: res.projectId,
      rootPath,
    };
  }
}

/**
 * Resolves a project hierarchically from a target path or ID string.
 * Behaves similarly to Git repository discovery.
 */
export function resolveProjectForPathOrId(
  target: string,
  projects: ProjectDetails[]
): ProjectDetails | undefined {
  // 1. Check if the target is directly a registered project ID
  const projectById = projects.find((p) => p.id === target);
  if (projectById) {
    return projectById;
  }

  // 2. Otherwise, treat target as a filesystem path
  const absPath = path.resolve(target);

  // 3. Select all projects that contain or match the target path (parent or equal)
  const matches = projects.filter((p) => {
    const parent = path.resolve(p.rootPath);
    const relative = path.relative(parent, absPath);
    // child is sub-directory or equal to parent if relative path is empty
    // or doesn't start with '..' and is not absolute.
    return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative));
  });

  if (matches.length === 0) {
    return undefined;
  }

  // 4. Choose the project with the longest matching rootPath
  return matches.reduce((longest, current) => {
    return current.rootPath.length > longest.rootPath.length ? current : longest;
  });
}

/**
 * Resolves a required project ID or throws ValidationError.
 */
export async function resolveRequiredProject(
  target: string | undefined,
  ctx: ExecutionContext
): Promise<string> {
  const projectsResult = await projectApplicationService.listProjects(ctx);
  const pathOrId = target || ctx.workingDir;
  const found = resolveProjectForPathOrId(pathOrId, projectsResult.projects);
  if (!found) {
    if (target) {
      throw new ValidationError(`No registered project found matching: ${target}`);
    } else {
      throw new ValidationError(
        'No project registered in current directory. Pass project ID explicitly.'
      );
    }
  }
  return found.id;
}

export const projectApiClient = new ProjectApiClient(httpClientService);
export const projectApplicationService = new ProjectApplicationService(projectApiClient);
export default projectApplicationService;
