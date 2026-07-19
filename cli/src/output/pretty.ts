import { Renderer } from './renderer';
import { ProjectRegistrationResult, ProjectListResult, ProjectDetails, ProjectRefreshResult, ProjectRemovalResult } from '../models/projectResult';
import { GeneratedContextResult, ContextDetailsResult, RefreshContextResult, DeleteContextResult } from '../models/contextResult';
import { KnowledgeSearchResult, KnowledgeDetailsResult, KnowledgeExplanationResult, KnowledgeRefreshResult, KnowledgeDeleteResult } from '../models/knowledgeResult';
import { HealthDiagnosticResult, DiagnosticsReportResult } from '../models/diagnosticResult';
import { DiagnosticCheck } from '../models/diagnosticCheck';

/**
 * Renderer generating human-readable pretty logs for all core Result Models.
 */
export class PrettyRenderer implements Renderer<unknown> {
  render(model: unknown): string {
    if (model === null || model === undefined) {
      return '';
    }

    if (typeof model === 'string') {
      return model;
    }

    const m = model as Record<string, unknown>;

    // 1. Project
    if ('project' in m && 'success' in m) {
      const reg = m as unknown as ProjectRegistrationResult;
      return `Project registered successfully: ${reg.project.name} (${reg.project.id})`;
    }
    if ('projects' in m) {
      const list = m as unknown as ProjectListResult;
      if (list.projects.length === 0) {
        return 'No registered projects found.';
      }
      const lines = ['Registered Projects:', '----------------------------------------'];
      for (const p of list.projects) {
        lines.push(`ID:   ${p.id}\nName: ${p.name}\nPath:\n${p.rootPath}\n----------------------------------------`);
      }
      return lines.join('\n');
    }
    if ('id' in m && 'name' in m && 'rootPath' in m && !('success' in m)) {
      const details = m as unknown as ProjectDetails;
      return `Project Details:\nID:   ${details.id}\nName: ${details.name}\nPath:\n${details.rootPath}`;
    }
    if ('projectId' in m && 'filesScanned' in m && 'snapshotGenerated' in m) {
      const ref = m as unknown as ProjectRefreshResult;
      return `Project refreshed successfully!\nProject ID:     ${ref.projectId}\nFiles Scanned:  ${ref.filesScanned}\nSnapshot Saved: ${ref.snapshotGenerated ? 'Yes' : 'No'}`;
    }
    if ('projectId' in m && 'success' in m && !('content' in m) && !('updatedAt' in m) && m.type !== 'DeleteContextResult') {
      const rem = m as unknown as ProjectRemovalResult;
      return `✓ Project successfully unregistered.\n\nProject:\n${rem.rootPath || rem.projectId}`;
    }

    // 2. Context
    if ('result' in m && 'session' in m) {
      const res = m.result as GeneratedContextResult;
      return `Context generated successfully for project ${res.projectId}!\nUpdated At: ${res.updatedAt}`;
    }
    if ('projectId' in m && 'content' in m && 'updatedAt' in m && !('format' in m)) {
      const details = m as unknown as ContextDetailsResult;
      return `Project Context (${details.projectId}):\n----------------------------------------\n${details.content}\n----------------------------------------`;
    }
    if ('projectId' in m && 'content' in m && 'format' in m) {
      const exportMd = m as unknown as { content: string };
      return exportMd.content;
    }
    if ('projectId' in m && 'content' in m && 'updatedAt' in m) {
      const ref = m as unknown as RefreshContextResult;
      return `Context refreshed successfully for project ${ref.projectId}!\nUpdated At: ${ref.updatedAt}`;
    }
    if ('projectId' in m && 'success' in m && !('filesScanned' in m) && m.type !== 'ProjectRemovalResult') {
       const del = m as unknown as DeleteContextResult;
       return `Context deleted successfully for project ${del.projectId}!`;
    }

    // 3. Knowledge
    if ('documents' in m && 'projectId' in m) {
      const search = m as unknown as KnowledgeSearchResult;
      if (search.documents.length === 0) {
        return 'No knowledge matches found.';
      }
      const lines = [`Knowledge search matches for project ${search.projectId}:`, '----------------------------------------'];
      for (const doc of search.documents) {
        lines.push(`ID:    ${doc.id}\nTitle: ${doc.title}\nScore: ${doc.score ?? 'N/A'}\n----------------------------------------`);
      }
      return lines.join('\n');
    }
    if ('id' in m && 'title' in m && 'content' in m) {
      const details = m as unknown as KnowledgeDetailsResult;
      return `Knowledge Document Details (${details.id}):\nTitle: ${details.title}\n----------------------------------------\n${details.content}\n----------------------------------------`;
    }
    if ('id' in m && 'explanation' in m) {
      const exp = m as unknown as KnowledgeExplanationResult;
      return `Knowledge Explanation (${exp.id}):\n----------------------------------------\n${exp.explanation}\n----------------------------------------`;
    }
    if ('projectId' in m && 'documents' in m) {
      const ref = m as unknown as KnowledgeRefreshResult;
      const lines = [`Knowledge base refreshed for project ${ref.projectId}!`, '----------------------------------------'];
      for (const doc of ref.documents) {
        lines.push(`[REFRESHED] ID: ${doc.id} - ${doc.title}`);
      }
      return lines.join('\n');
    }
    if ('id' in m && 'success' in m && !('projectId' in m)) {
      const del = m as unknown as KnowledgeDeleteResult;
      return `Knowledge item deleted successfully: ${del.id}`;
    }

    // 4. Diagnostics
    if ('checks' in m && 'allPassed' in m) {
      const diag = m as unknown as HealthDiagnosticResult;
      let title = 'Diagnostics Run:';
      if (diag.checks.length > 0) {
        const firstId = diag.checks[0].id;
        if (firstId.startsWith('health:')) {
          title = 'Diagnostics Health Suite Run:';
        } else if (firstId.startsWith('config:')) {
          title = 'Diagnostics Config Suite Run:';
        } else if (firstId.startsWith('connectivity:')) {
          title = 'Diagnostics Connectivity Suite Run:';
        } else if (firstId.startsWith('env:')) {
          title = 'Diagnostics Environment Suite Run:';
        }
      }
      const lines = [title, '----------------------------------------'];
      for (const check of diag.checks) {
        lines.push(`Check:   ${check.id}`);
        lines.push(`Status:  ${check.status}`);
        if (check.result) lines.push(`Result:  ${check.result}`);
        if (check.error) lines.push(`Error:   ${check.error}`);
        lines.push('----------------------------------------');
      }
      return lines.join('\n');
    }
    if ('health' in m && 'configuration' in m && 'connectivity' in m && 'environment' in m) {
      const rep = m as unknown as DiagnosticsReportResult;
      const lines = [
        '========================================',
        'MEMORA CLI UNIFIED DIAGNOSTICS REPORT',
        `Generated At: ${rep.generatedAt}`,
        `All Passed:   ${rep.allPassed ? 'Yes' : 'No'}`,
        '========================================'
      ];

      const formatSuite = (title: string, checks: DiagnosticCheck[]) => {
        lines.push(`\nSuite: ${title}`);
        lines.push('----------------------------------------');
        for (const check of checks) {
          lines.push(`[${check.status}] ${check.id}`);
          if (check.result) lines.push(`  Result: ${check.result}`);
          if (check.error) lines.push(`  Error:  ${check.error}`);
        }
      };

      formatSuite('Health', rep.health.checks);
      formatSuite('Configuration', rep.configuration.checks);
      formatSuite('Connectivity', rep.connectivity.checks);
      formatSuite('Environment', rep.environment.checks);
      lines.push('\n========================================');
      return lines.join('\n');
    }

    return String(model);
  }
}

export const prettyRenderer = new PrettyRenderer();
