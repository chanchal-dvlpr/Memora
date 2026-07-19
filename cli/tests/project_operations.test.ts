import { ProjectResponse } from '../src/api/project';
import { HttpTransport } from '../src/http/transport';
import { httpClientService } from '../src/http/clientService';
import { commandEventPublisher, CommandEvent } from '../src/events/commandEvents';
import { run } from '../src/cli';
import { Console } from 'console';
import { resolveProjectForPathOrId } from '../src/services/project';

interface ServiceWithPipeline {
  pipeline: {
    transport: HttpTransport;
  };
}

describe('Project Operations & Events Subsystem', () => {
  let originalLog: typeof console.log;
  let logSpy: jest.SpyInstance;

  beforeAll(() => {
    originalLog = new Console(process.stdout).log;
  });

  beforeEach(() => {
    console.log = originalLog;
    logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    logSpy.mockRestore();
  });

  describe('Command Event Publisher/Subscriber', () => {
    it('should notify subscribers of published events and support unsubscribe', () => {
      const receivedEvents: CommandEvent<unknown>[] = [];
      const subscriber = {
        onEvent(event: CommandEvent<unknown>) {
          receivedEvents.push(event);
        },
      };

      const unsubscribe = commandEventPublisher.subscribe(subscriber);

      commandEventPublisher.publish({
        type: 'TestEvent',
        timestamp: new Date(),
        payload: { val: 42 },
      });

      expect(receivedEvents.length).toBe(1);
      expect(receivedEvents[0].type).toBe('TestEvent');
      expect((receivedEvents[0].payload as Record<string, unknown>).val).toBe(42);

      // Unsubscribe
      unsubscribe();
      commandEventPublisher.publish({
        type: 'TestEvent2',
        timestamp: new Date(),
        payload: { val: 100 },
      });

      expect(receivedEvents.length).toBe(1);
    });
  });

  describe('Project CLI Command Operations & Events integration', () => {
    it('should refresh project successfully and emit event', async () => {
      const mockProjectsList: ProjectResponse[] = [
        { id: 'proj-1', name: 'my-project', rootPath: __dirname },
      ];
      const mockRefreshResp = {
        projectId: 'proj-1',
        filesScanned: 15,
        snapshotGenerated: true,
      };

      const mockTransport: HttpTransport = {
        send: async <T>(builder: unknown) => {
          const b = builder as { path: string };
          if (b.path.includes('/refresh')) {
            return { status: 200, headers: {}, data: mockRefreshResp as unknown as T };
          }
          return { status: 200, headers: {}, data: mockProjectsList as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      const exitCode = await run(['node', 'memora', 'project', 'refresh', 'proj-1', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as Record<string, unknown>;
      expect(output.projectId).toBe('proj-1');
      expect(output.filesScanned).toBe(15);

      expect(events.some((e) => e.type === 'ProjectRefreshed')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should remove project successfully and emit event', async () => {
      const mockRemoveResp = {
        projectId: 'proj-removed',
      };

      const mockTransport: HttpTransport = {
        send: async <T>() => {
          return { status: 200, headers: {}, data: mockRemoveResp as unknown as T };
        },
      };

      const originalTransport = (httpClientService as unknown as ServiceWithPipeline).pipeline
        .transport;
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = mockTransport;

      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      const exitCode = await run(['node', 'memora', 'project', 'remove', 'proj-removed', '--json']);
      expect(exitCode).toBe(0);
      expect(logSpy).toHaveBeenCalled();
      const output = JSON.parse(logSpy.mock.calls[0][0]) as Record<string, unknown>;
      expect(output.projectId).toBe('proj-removed');

      expect(events.some((e) => e.type === 'ProjectRemoved')).toBe(true);

      unsubscribe();
      (httpClientService as unknown as ServiceWithPipeline).pipeline.transport = originalTransport;
    });

    it('should publish ProjectCommandFailed events on failures', async () => {
      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        },
      });

      const exitCode = await run(['node', 'memora', 'project', 'show', 'non-existent', '--json']);
      expect(exitCode).not.toBe(0);
      expect(events.some((e) => e.type === 'ProjectCommandFailed')).toBe(true);

      unsubscribe();
    });
  });

  describe('resolveProjectForPathOrId', () => {
    const mockProjects = [
      { id: 'proj-root', name: 'root', rootPath: '/Users/test/workspace' },
      { id: 'proj-nested', name: 'nested', rootPath: '/Users/test/workspace/cli' },
    ];

    it('should match directly by project ID', () => {
      const resolved = resolveProjectForPathOrId('proj-root', mockProjects);
      expect(resolved).toEqual(mockProjects[0]);
    });

    it('should resolve subdirectories to the closest enclosing project root (longest matching path)', () => {
      // inside /Users/test/workspace/cli/src -> matches both, but proj-nested is longer
      const resolvedNested = resolveProjectForPathOrId('/Users/test/workspace/cli/src', mockProjects);
      expect(resolvedNested).toEqual(mockProjects[1]);

      // inside /Users/test/workspace/backend -> only matches proj-root
      const resolvedRoot = resolveProjectForPathOrId('/Users/test/workspace/backend', mockProjects);
      expect(resolvedRoot).toEqual(mockProjects[0]);
    });

    it('should return undefined if no matching project is found', () => {
      const resolved = resolveProjectForPathOrId('/Users/other/path', mockProjects);
      expect(resolved).toBeUndefined();
    });
  });
});
