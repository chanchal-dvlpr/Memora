import { prettyRenderer } from '../src/output/pretty';
import { jsonRenderer } from '../src/output/json';
import { tableRenderer, TableData } from '../src/output/table';
import { markdownRenderer } from '../src/output/markdown';

describe('Renderer Framework & Core Renderers', () => {
  describe('PrettyRenderer', () => {
    it('should format null/undefined as empty string', () => {
      expect(prettyRenderer.render(null)).toBe('');
      expect(prettyRenderer.render(undefined)).toBe('');
    });

    it('should format raw strings as-is', () => {
      expect(prettyRenderer.render('hello world')).toBe('hello world');
    });

    it('should format ProjectRegistrationResult', () => {
      const reg = {
        success: true,
        project: { id: 'p1', name: 'proj1', path: '/path1' }
      };
      expect(prettyRenderer.render(reg)).toBe('Project registered successfully: proj1 (p1)');
    });

    it('should format ProjectListResult', () => {
      const empty = { projects: [] };
      expect(prettyRenderer.render(empty)).toBe('No registered projects found.');

      const list = {
        projects: [
          { id: 'p1', name: 'proj1', rootPath: '/path1' },
          { id: 'p2', name: 'proj2', rootPath: '/path2' }
        ]
      };
      const formatted = prettyRenderer.render(list);
      expect(formatted).toContain('Registered Projects:');
      expect(formatted).toContain('ID:   p1\nName: proj1\nPath:\n/path1');
      expect(formatted).toContain('ID:   p2\nName: proj2\nPath:\n/path2');
    });

    it('should format ProjectDetails', () => {
      const details = { id: 'p1', name: 'proj1', rootPath: '/path1' };
      expect(prettyRenderer.render(details)).toBe('Project Details:\nID:   p1\nName: proj1\nPath:\n/path1');
    });

    it('should format ProjectRefreshResult', () => {
      const refresh = {
        projectId: 'p1',
        filesScanned: 10,
        snapshotGenerated: true
      };
      expect(prettyRenderer.render(refresh)).toContain('Project refreshed successfully!');
      expect(prettyRenderer.render(refresh)).toContain('Files Scanned:  10');
      expect(prettyRenderer.render(refresh)).toContain('Snapshot Saved: Yes');
    });

    it('should format ProjectRemovalResult', () => {
      const rem = {
        type: 'ProjectRemovalResult',
        success: true,
        projectId: 'p1',
        rootPath: '/path1'
      };
      expect(prettyRenderer.render(rem)).toBe('✓ Project successfully unregistered.\n\nProject:\n/path1');
    });

    it('should format GeneratedContextResult wrapped with session', () => {
      const wrapped = {
        result: { projectId: 'p1', content: 'ctx', updatedAt: '2026-07-17' },
        session: { sessionId: 's1' }
      };
      expect(prettyRenderer.render(wrapped)).toBe('Context generated successfully for project p1!\nUpdated At: 2026-07-17');
    });

    it('should format ContextDetailsResult', () => {
      const details = {
        projectId: 'p1',
        content: 'ctx content',
        updatedAt: '2026-07-17'
      };
      expect(prettyRenderer.render(details)).toContain('Project Context (p1):');
      expect(prettyRenderer.render(details)).toContain('ctx content');
    });

    it('should format ExportContextResult', () => {
      const exportMd = {
        projectId: 'p1',
        content: 'raw md content',
        format: 'markdown'
      };
      expect(prettyRenderer.render(exportMd)).toBe('raw md content');
    });

    it('should format DeleteContextResult', () => {
      const del = {
        type: 'DeleteContextResult',
        projectId: 'p1',
        success: true
      };
      expect(prettyRenderer.render(del)).toBe('Context deleted successfully for project p1!');
    });

    it('should format KnowledgeSearchResult', () => {
      const searchEmpty = { projectId: 'p1', documents: [] };
      expect(prettyRenderer.render(searchEmpty)).toBe('No knowledge matches found.');

      const search = {
        projectId: 'p1',
        documents: [
          { id: 'd1', title: 'title1', content: 'c1', score: 0.95 }
        ]
      };
      const formatted = prettyRenderer.render(search);
      expect(formatted).toContain('Knowledge search matches for project p1:');
      expect(formatted).toContain('ID:    d1');
      expect(formatted).toContain('Title: title1');
      expect(formatted).toContain('Score: 0.95');
    });

    it('should format KnowledgeDetailsResult', () => {
      const details = { id: 'd1', title: 'title1', content: 'content1' };
      const formatted = prettyRenderer.render(details);
      expect(formatted).toContain('Knowledge Document Details (d1):');
      expect(formatted).toContain('Title: title1');
      expect(formatted).toContain('content1');
    });

    it('should format KnowledgeExplanationResult', () => {
      const exp = { id: 'd1', explanation: 'exp1' };
      const formatted = prettyRenderer.render(exp);
      expect(formatted).toContain('Knowledge Explanation (d1):');
      expect(formatted).toContain('exp1');
    });

    it('should format KnowledgeDeleteResult', () => {
      const del = { id: 'd1', success: true };
      expect(prettyRenderer.render(del)).toBe('Knowledge item deleted successfully: d1');
    });

    it('should format HealthDiagnosticResult', () => {
      const diag = {
        allPassed: true,
        checks: [
          { id: 'health:daemon', name: 'Daemon status', status: 'PASS', result: 'Connected' }
        ]
      };
      const formatted = prettyRenderer.render(diag);
      expect(formatted).toContain('Diagnostics Health Suite Run:');
      expect(formatted).toContain('Check:   health:daemon');
      expect(formatted).toContain('Status:  PASS');
      expect(formatted).toContain('Result:  Connected');
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
      const formatted = prettyRenderer.render(report);
      expect(formatted).toContain('MEMORA CLI UNIFIED DIAGNOSTICS REPORT');
      expect(formatted).toContain('Suite: Health');
      expect(formatted).toContain('Suite: Configuration');
      expect(formatted).toContain('Suite: Connectivity');
      expect(formatted).toContain('Suite: Environment');
    });
  });

  describe('JsonRenderer', () => {
    it('should sort keys deterministically', () => {
      const obj = { z: 1, a: { y: 2, b: 3 } };
      const output = jsonRenderer.render(obj);
      expect(output).toBe(JSON.stringify({ a: { b: 3, y: 2 }, z: 1 }, null, 2));
    });

    it('should extract inner project/projects properties', () => {
      const reg = {
        success: true,
        project: { id: 'p1', name: 'proj1', path: '/path1' }
      };
      expect(JSON.parse(jsonRenderer.render(reg))).toEqual(reg.project);

      const list = {
        projects: [{ id: 'p1', name: 'proj1', path: '/path1' }]
      };
      expect(JSON.parse(jsonRenderer.render(list))).toEqual(list.projects);
    });
  });

  describe('TableRenderer', () => {
    it('should render correct columns, padding and alignment', () => {
      const data: TableData<{ id: string; name: string }> = {
        columns: [
          { header: 'ID', key: 'id' },
          { header: 'Name', key: 'name' }
        ],
        rows: [
          { id: '1', name: 'Short' },
          { id: '100', name: 'LongerName' }
        ]
      };
      const output = tableRenderer.render(data);
      expect(output).toContain('ID  | Name      ');
      expect(output).toContain('---+-----------');
      expect(output).toContain('1   | Short     ');
      expect(output).toContain('100 | LongerName');
    });

    it('should return no data available for empty rows', () => {
      const data: TableData<Record<string, unknown>> = { columns: [], rows: [] };
      expect(tableRenderer.render(data)).toBe('No data available.');
    });
  });

  describe('MarkdownRenderer', () => {
    it('should return raw string for string input', () => {
      expect(markdownRenderer.render('hello')).toBe('hello');
    });

    it('should format object as markdown code block', () => {
      expect(markdownRenderer.render({ a: 1 })).toBe('```json\n{\n  "a": 1\n}\n```');
    });
  });
});
