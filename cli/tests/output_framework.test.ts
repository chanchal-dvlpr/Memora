import * as fs from 'fs';
import * as path from 'path';
import { OutputService } from '../src/output/outputService';
import { RenderRequest } from '../src/output/types';
import { commandEventPublisher, CommandEvent } from '../src/events/commandEvents';
import { ValidationError } from '../src/errors/errors';
import { ExecutionContext } from '../src/models/context';
import { markdownRenderer } from '../src/output/markdown';
import { validateRenderRequest } from '../src/validators/output';

describe('Output & Formatting Subsystem Extensions', () => {
  const testDir = path.resolve(__dirname, 'temp-output-test-dir');
  const mockCtx = {
    workingDir: process.cwd(),
    requestId: 'req-123',
    correlationId: 'corr-123',
    env: {},
    outputMode: 'text',
    verbosity: 'normal',
    logger: {
      trace: () => {},
      debug: () => {},
      info: () => {},
      warn: () => {},
      error: () => {},
    },
  } as unknown as ExecutionContext;

  beforeAll(() => {
    if (!fs.existsSync(testDir)) {
      fs.mkdirSync(testDir, { recursive: true });
    }
  });

  afterAll(() => {
    if (fs.existsSync(testDir)) {
      fs.rmSync(testDir, { recursive: true, force: true });
    }
  });

  describe('MarkdownRenderer', () => {

    it('should format raw strings as-is', () => {
      expect(markdownRenderer.render('hello')).toBe('hello');
    });

    it('should format ProjectRegistrationResult', () => {
      const reg = {
        success: true,
        project: { id: 'p1', name: 'proj1', path: '/path1' }
      };
      const rendered = markdownRenderer.render(reg);
      expect(rendered).toContain('# Project Registered Successfully');
      expect(rendered).toContain('- **ID**: `p1`');
      expect(rendered).toContain('<!-- [FUTURE IMAGE PLACEHOLDER: diagram_or_screenshot] -->');
    });

    it('should format ProjectListResult', () => {
      const empty = { projects: [] };
      expect(markdownRenderer.render(empty)).toContain('No registered projects found.');

      const list = {
        projects: [
          { id: 'p1', name: 'proj1', rootPath: '/path1' },
          { id: 'p2', name: 'proj2', rootPath: '/path2' }
        ]
      };
      const rendered = markdownRenderer.render(list);
      expect(rendered).toContain('# Registered Projects');
      expect(rendered).toContain('| ID | Name | Path |');
      expect(rendered).toContain('| `p1` | `proj1` | `/path1` |');
    });

    it('should format ProjectDetails', () => {
      const details = { id: 'p1', name: 'proj1', rootPath: '/path1' };
      const rendered = markdownRenderer.render(details);
      expect(rendered).toContain('# Project Details');
      expect(rendered).toContain('- **ID**: `p1`');
    });

    it('should format ProjectRefreshResult', () => {
      const refresh = {
        projectId: 'p1',
        filesScanned: 10,
        snapshotGenerated: true
      };
      const rendered = markdownRenderer.render(refresh);
      expect(rendered).toContain('# Project Refreshed Successfully');
      expect(rendered).toContain('Files Scanned**: 10');
    });

    it('should format ProjectRemovalResult', () => {
      const rem = {
        success: true,
        projectId: 'p1',
        rootPath: '/path1'
      };
      const rendered = markdownRenderer.render(rem);
      expect(rendered).toContain('# Project Unregistered Successfully');
      expect(rendered).toContain('- **Project ID**: `p1`');
      expect(rendered).toContain('- **Project Path**: `/path1`');
    });

    it('should format GeneratedContextResult wrapped in session', () => {
      const wrapped = {
        result: { projectId: 'p1', content: 'ctx data', updatedAt: '2026-07-17' },
        session: { sessionId: 's1' }
      };
      const rendered = markdownRenderer.render(wrapped);
      expect(rendered).toContain('# Context Generated Successfully');
      expect(rendered).toContain('## Context Content');
      expect(rendered).toContain('```markdown\nctx data\n```');
    });

    it('should format ContextDetailsResult', () => {
      const details = {
        projectId: 'p1',
        content: 'line1\nline2',
        updatedAt: '2026-07-17'
      };
      const rendered = markdownRenderer.render(details);
      expect(rendered).toContain('# Project Context (`p1`)');
      expect(rendered).toContain('> line1\n> line2');
    });

    it('should format ContextDetailsResult in export mode', () => {
      const exportMd = {
        projectId: 'p1',
        content: 'raw markdown context',
        format: 'markdown'
      };
      const rendered = markdownRenderer.render(exportMd);
      expect(rendered).toBe('raw markdown context');
    });

    it('should format DeleteContextResult', () => {
      const del = {
        type: 'DeleteContextResult',
        projectId: 'p1',
        success: true
      };
      const rendered = markdownRenderer.render(del);
      expect(rendered).toContain('# Context Deleted Successfully');
      expect(rendered).toContain('- **Project ID**: `p1`');
    });

    it('should format KnowledgeSearchResult', () => {
      const searchEmpty = { projectId: 'p1', documents: [] };
      expect(markdownRenderer.render(searchEmpty)).toContain('No knowledge matches found.');

      const search = {
        projectId: 'p1',
        documents: [
          { id: 'd1', title: 'title1', content: 'c1', score: 0.95 }
        ]
      };
      const rendered = markdownRenderer.render(search);
      expect(rendered).toContain('# Knowledge Search Results');
      expect(rendered).toContain('| `d1` | `title1` | `0.95` |');
    });

    it('should format KnowledgeDetailsResult', () => {
      const details = { id: 'd1', title: 'title1', content: 'content1\ncontent2' };
      const rendered = markdownRenderer.render(details);
      expect(rendered).toContain('# Knowledge Document Details (`d1`)');
      expect(rendered).toContain('> content1\n> content2');
    });

    it('should format KnowledgeExplanationResult', () => {
      const exp = { id: 'd1', explanation: 'exp1\nexp2' };
      const rendered = markdownRenderer.render(exp);
      expect(rendered).toContain('# Knowledge Explanation (`d1`)');
      expect(rendered).toContain('> exp1\n> exp2');
    });

    it('should format KnowledgeRefreshResult', () => {
      const ref = {
        type: 'KnowledgeRefreshResult',
        projectId: 'p1',
        documents: [
          { id: 'd1', title: 'title1' }
        ]
      };
      const rendered = markdownRenderer.render(ref);
      expect(rendered).toContain('# Knowledge Base Refreshed');
      expect(rendered).toContain('- [REFRESHED] ID: `d1` - `title1`');
    });

    it('should format KnowledgeDeleteResult', () => {
      const del = { id: 'd1', success: true };
      const rendered = markdownRenderer.render(del);
      expect(rendered).toContain('# Knowledge Item Deleted');
      expect(rendered).toContain('- **ID**: `d1`');
    });

    it('should format HealthDiagnosticResult', () => {
      const diag = {
        allPassed: true,
        checks: [
          { id: 'health:daemon', name: 'Daemon status', status: 'PASS', result: 'Connected' }
        ]
      };
      const rendered = markdownRenderer.render(diag);
      expect(rendered).toContain('# Diagnostics Run');
      expect(rendered).toContain('| `health:daemon` | Daemon status | `PASS` | Connected |');
    });

    it('should format DiagnosticsReportResult', () => {
      const report = {
        allPassed: true,
        generatedAt: '2026-07-17',
        health: { allPassed: true, checks: [{ id: 'health:daemon', name: 'Daemon', status: 'PASS' }] },
        configuration: { allPassed: true, checks: [{ id: 'config:verify', name: 'Verify', status: 'PASS' }] },
        connectivity: { allPassed: true, checks: [{ id: 'connectivity:ping', name: 'Ping', status: 'PASS' }] },
        environment: { allPassed: true, checks: [{ id: 'env:os', name: 'OS', status: 'PASS' }] }
      };
      const rendered = markdownRenderer.render(report);
      expect(rendered).toContain('# MEMORA CLI UNIFIED DIAGNOSTICS REPORT');
      expect(rendered).toContain('## Health');
      expect(rendered).toContain('## Configuration');
      expect(rendered).toContain('## Connectivity');
      expect(rendered).toContain('## Environment');
    });
  });

  describe('Output Validation', () => {

    it('should throw validation error on undefined request or data', () => {
      expect(() => validateRenderRequest(null as unknown as RenderRequest)).toThrow(ValidationError);
      expect(() => validateRenderRequest({ mode: 'json' } as unknown as RenderRequest)).toThrow(ValidationError);
    });

    it('should throw validation error on invalid mode', () => {
      expect(() => validateRenderRequest({ mode: 'invalid', data: {} } as unknown as RenderRequest)).toThrow(ValidationError);
    });

    it('should throw validation error on invalid table payload format', () => {
      const req: RenderRequest = {
        mode: 'table',
        data: { invalidField: [] },
        destination: 'stdout'
      };
      expect(() => validateRenderRequest(req)).toThrow('Table renderer requires data with columns and rows arrays');
    });

    it('should throw validation error on missing/invalid file path', () => {
      const req: RenderRequest = {
        mode: 'pretty',
        data: 'text',
        destination: { type: 'file', path: '' }
      };
      expect(() => validateRenderRequest(req)).toThrow('Path is required for file destination');
    });

    it('should throw validation error on non-existent parent folder path', () => {
      const req: RenderRequest = {
        mode: 'pretty',
        data: 'text',
        destination: { type: 'file', path: path.join(testDir, 'non-existent-parent/file.txt') }
      };
      expect(() => validateRenderRequest(req)).toThrow('Parent directory does not exist');
    });
  });

  describe('RenderRequest Immutability', () => {
    it('should freeze RenderRequest recursively', async () => {
      const service = new OutputService(mockCtx, commandEventPublisher);
      const req: RenderRequest = {
        mode: 'pretty',
        data: { nested: { value: 1 } },
        destination: 'stdout'
      };

      const result = await service.writeRequest(req);
      expect(Object.isFrozen(result.request)).toBe(true);
      expect(Object.isFrozen((result.request.data as Record<string, Record<string, unknown>>).nested)).toBe(true);
    });
  });

  describe('Output Service & Lifecycle Events', () => {
    it('should publish started, completed, and export events in sequence', async () => {
      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        }
      });

      const service = new OutputService(mockCtx, commandEventPublisher);
      const req: RenderRequest = {
        mode: 'json',
        data: { test: 'val' },
        destination: 'stdout'
      };

      const originalConsoleLog = console.log;
      console.log = jest.fn();

      const result = await service.writeRequest(req);
      console.log = originalConsoleLog;

      expect(events.map(e => e.type)).toEqual([
        'RenderingStarted',
        'RenderingCompleted',
        'ExportStarted',
        'ExportCompleted'
      ]);

      expect(events[0].payload).toEqual({ mode: 'json', destination: 'stdout' });
      expect((events[1].payload as Record<string, unknown>).outputLength).toBe(result.output.length);
      expect(events[2].payload).toEqual({ destination: 'stdout' });
      expect((events[3].payload as Record<string, unknown>).bytesWritten).toBeGreaterThan(0);

      unsubscribe();
    });

    it('should publish failed events on validation error', async () => {
      const events: CommandEvent<unknown>[] = [];
      const unsubscribe = commandEventPublisher.subscribe({
        onEvent(e) {
          events.push(e);
        }
      });

      const service = new OutputService(mockCtx, commandEventPublisher);
      const req: RenderRequest = {
        mode: 'invalid' as unknown as RenderRequest['mode'],
        data: 'text',
        destination: 'stdout'
      };

      await expect(service.writeRequest(req)).rejects.toThrow();

      expect(events.map(e => e.type)).toEqual([
        'RenderingStarted',
        'RenderingFailed'
      ]);

      expect((events[1].payload as Record<string, unknown>).error).toContain('Unsupported renderer mode');

      unsubscribe();
    });
  });

  describe('Export Exporter Framework', () => {
    it('should support writing output directly to target file', async () => {
      const service = new OutputService(mockCtx, commandEventPublisher);
      const filePath = path.join(testDir, 'rendered.json');

      const req: RenderRequest = {
        mode: 'json',
        data: { message: 'hello' },
        destination: { type: 'file', path: filePath }
      };

      const result = await service.writeRequest(req);
      expect(fs.existsSync(filePath)).toBe(true);
      expect(fs.readFileSync(filePath, 'utf8')).toBe(result.output);
    });

    it('should support clipboard placeholder without throwing errors', async () => {
      const service = new OutputService(mockCtx, commandEventPublisher);
      const req: RenderRequest = {
        mode: 'pretty',
        data: 'clipboard-test',
        destination: { type: 'clipboard' }
      };

      await expect(service.writeRequest(req)).resolves.toBeDefined();
    });
  });
});
