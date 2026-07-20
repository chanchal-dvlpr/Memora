import { Console } from 'console';
import { projectApplicationService } from '../src/services/project';
import { ProjectResponse } from '../src/api/project';
import { ExecutionContext } from '../src/models/context';
import { ValidationError } from '../src/errors/errors';
import { HttpTransport } from '../src/http/transport';
import { httpClientService } from '../src/http/clientService';
import { configService } from '../src/config/service';
import { run } from '../src/cli';
import * as promptUtils from '../src/utils/prompt';

interface ServiceWithPipeline {
  pipeline: {
    transport: HttpTransport;
  };
}

describe('Project Management Subsystem', () => {
  const mockCtx = {
    workingDir: __dirname,
    requestId: 'req-123',
    correlationId: 'corr-456',
    env: {},
  } as unknown as ExecutionContext;

  let originalLog: typeof console.log;
  let logSpy: jest.SpyInstance;

  beforeAll(async () => {
    originalLog = new Console(process.stdout).log;
    await configService.load();
  });

  afterAll(() => {
    console.log = originalLog;
  });

  beforeEach(() => {
    console.log = originalLog;
    logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    logSpy.mockRestore();
  });

  describe('ProjectApplicationService Validations', () => {
    it('should fail registration on non-existent directory paths', async () => {
      await expect(
        projectApplicationService.registerProject('/invalid/path/doesnt/exist', undefined, mockCtx),
      ).rejects.toThrow(ValidationError);
    });

    it('should resolve absolute path and default project name to basename', async () => {
      const mockProjectResp: ProjectResponse = {
        id: 'proj-123',
        name: 'tests',
        rootPath: __dirname,
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockProjectResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const project = await projectApplicationService.registerProject(
        undefined,
        undefined,
        mockCtx,
      );
      expect(project.project.name).toBe('tests');
      expect(project.project.rootPath).toBe(__dirname);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });

  describe('Project CLI Commands Dispatching', () => {
    it('should execute memora project list successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [{ id: 'p1', name: 'proj1', rootPath: '/path1' }];

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'project', 'list', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as ProjectResponse[];
      expect(output).toEqual(mockProjectsList);

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should execute memora project show successfully', async () => {
      const mockProjectsList: ProjectResponse[] = [{ id: 'p1', name: 'proj1', rootPath: '/path1' }];

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'project', 'show', 'p1', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as ProjectResponse;
      expect(output.id).toBe('p1');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should successfully unregister a project when using --force', async () => {
      const mockProjectsList: ProjectResponse[] = [{ id: 'p1', name: 'proj1', rootPath: process.cwd() }];
      const mockTransport: HttpTransport = {
        send: async <T>(builder: { getPath(): string }) => {
          if (builder.getPath().includes('/projects/p1')) {
            return { status: 200, headers: {}, data: { projectId: 'p1' } as unknown as T };
          }
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline.transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const exitCode = await run(['node', 'memora', 'unregister', '--force']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      expect(logSpy.mock.calls[0][0]).toContain('Project successfully unregistered.');

      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should prompt for confirmation and succeed if approved', async () => {
      const mockProjectsList: ProjectResponse[] = [{ id: 'p1', name: 'proj1', rootPath: process.cwd() }];
      const mockTransport: HttpTransport = {
        send: async <T>(builder: { getPath(): string }) => {
          if (builder.getPath().includes('/projects/p1')) {
            return { status: 200, headers: {}, data: { projectId: 'p1' } as unknown as T };
          }
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline.transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const confirmSpy = jest.spyOn(promptUtils, 'confirmPrompt').mockResolvedValue(true);

      const exitCode = await run(['node', 'memora', 'unregister']);
      expect(exitCode).toBe(0);
      expect(confirmSpy).toHaveBeenCalled();
      expect(logSpy).toHaveBeenCalled();
      const combined = logSpy.mock.calls.map(call => call[0]).join('\n');
      expect(combined).toContain('Project successfully unregistered.');

      confirmSpy.mockRestore();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should prompt for confirmation and abort if declined', async () => {
      const mockProjectsList: ProjectResponse[] = [{ id: 'p1', name: 'proj1', rootPath: process.cwd() }];
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline.transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const confirmSpy = jest.spyOn(promptUtils, 'confirmPrompt').mockResolvedValue(false);

      const exitCode = await run(['node', 'memora', 'unregister']);
      expect(exitCode).toBe(0);
      expect(confirmSpy).toHaveBeenCalled();
      expect(logSpy).toHaveBeenCalledWith('Unregistration cancelled.\n');

      confirmSpy.mockRestore();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should return error if project is not registered', async () => {
      const mockProjectsList: ProjectResponse[] = [];
      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline.transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);

      const exitCode = await run(['node', 'memora', 'unregister', '--force']);
      expect(exitCode).toBe(2); // ValidationError exitCode is 2
      expect(stderrSpy).toHaveBeenCalled();
      const combined = stderrSpy.mock.calls.map(call => call[0]).join('\n');
      expect(combined).toContain('Project is not registered.');

      stderrSpy.mockRestore();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });
  });
});
