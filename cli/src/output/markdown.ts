import { Renderer } from './renderer';
import { ProjectRegistrationResult, ProjectListResult, ProjectDetails, ProjectRefreshResult, ProjectRemovalResult } from '../models/projectResult';
import { GeneratedContextResult, ContextDetailsResult, RefreshContextResult, DeleteContextResult } from '../models/contextResult';
import { KnowledgeSearchResult, KnowledgeDetailsResult, KnowledgeExplanationResult, KnowledgeRefreshResult, KnowledgeDeleteResult } from '../models/knowledgeResult';
import { HealthDiagnosticResult, DiagnosticsReportResult } from '../models/diagnosticResult';

/**
 * Renderer generating markdown presentation blocks for all core Result Models.
 */
export class MarkdownRenderer implements Renderer<unknown> {
  render(model: unknown): string {
    if (model === null || model === undefined) {
      return '';
    }

    if (typeof model === 'string') {
      return model;
    }

    const m = model as Record<string, unknown>;

    // Future image placeholder comment block as requested
    const imgPlaceholder = '\n\n<!-- [FUTURE IMAGE PLACEHOLDER: diagram_or_screenshot] -->';

    // 1. Project
    if ('project' in m && 'success' in m) {
      const reg = m as unknown as ProjectRegistrationResult;
      return `# Project Registered Successfully\n\n- **ID**: \`${reg.project.id}\`\n- **Name**: \`${reg.project.name}\`\n- **Path**: \`${reg.project.rootPath}\`${imgPlaceholder}`;
    }
    if ('projects' in m) {
      const list = m as unknown as ProjectListResult;
      if (list.projects.length === 0) {
        return '# Registered Projects\n\n> No registered projects found.';
      }
      const lines = ['# Registered Projects', '', '| ID | Name | Path |', '| --- | --- | --- |'];
      for (const p of list.projects) {
        lines.push(`| \`${p.id}\` | \`${p.name}\` | \`${p.rootPath}\` |`);
      }
      return lines.join('\n') + imgPlaceholder;
    }
    if ('id' in m && 'name' in m && 'rootPath' in m && !('success' in m)) {
      const details = m as unknown as ProjectDetails;
      return `# Project Details\n\n- **ID**: \`${details.id}\`\n- **Name**: \`${details.name}\`\n- **Path**: \`${details.rootPath}\`${imgPlaceholder}`;
    }
    if ('projectId' in m && 'filesScanned' in m && 'snapshotGenerated' in m) {
      const ref = m as unknown as ProjectRefreshResult;
      return `# Project Refreshed Successfully\n\n- **Project ID**: \`${ref.projectId}\`\n- **Files Scanned**: ${ref.filesScanned}\n- **Snapshot Generated**: ${ref.snapshotGenerated ? 'Yes' : 'No'}`;
    }
    if ('projectId' in m && 'success' in m && !('content' in m) && !('updatedAt' in m) && m.type !== 'DeleteContextResult') {
      const rem = m as unknown as ProjectRemovalResult;
      return `# Project Unregistered Successfully\n\n- **Project ID**: \`${rem.projectId}\`\n- **Project Path**: \`${rem.rootPath || ''}\``;
    }

    // 2. Context
    if ('result' in m && 'session' in m) {
      const res = m.result as GeneratedContextResult;
      return `# Context Generated Successfully\n\n- **Project ID**: \`${res.projectId}\`\n- **Updated At**: \`${res.updatedAt}\`\n\n## Context Content\n\n\`\`\`markdown\n${res.content}\n\`\`\``;
    }
    if ('projectId' in m && 'content' in m && 'updatedAt' in m && !('format' in m)) {
      const details = m as unknown as ContextDetailsResult;
      return `# Project Context (\`${details.projectId}\`)\n\n> ${details.content.split('\n').join('\n> ')}`;
    }
    if ('projectId' in m && 'content' in m && 'format' in m) {
      return m.content as string;
    }
    if ('projectId' in m && 'content' in m && 'updatedAt' in m) {
      const ref = m as unknown as RefreshContextResult;
      return `# Context Refreshed Successfully\n\n- **Project ID**: \`${ref.projectId}\`\n- **Updated At**: \`${ref.updatedAt}\`\n\n## Context Content\n\n\`\`\`markdown\n${ref.content}\n\`\`\``;
    }
    if ('projectId' in m && 'success' in m && !('filesScanned' in m) && m.type !== 'ProjectRemovalResult') {
      const del = m as unknown as DeleteContextResult;
      return `# Context Deleted Successfully\n\n- **Project ID**: \`${del.projectId}\``;
    }

    // 3. Knowledge
    if ('documents' in m && 'projectId' in m && m.type !== 'KnowledgeRefreshResult') {
      const search = m as unknown as KnowledgeSearchResult;
      if (search.documents.length === 0) {
        return '# Knowledge Search Results\n\n> No knowledge matches found.';
      }
      const lines = ['# Knowledge Search Results', '', `- **Project ID**: \`${search.projectId}\``, '', '| ID | Title | Score |', '| --- | --- | --- |'];
      for (const doc of search.documents) {
        lines.push(`| \`${doc.id}\` | \`${doc.title}\` | \`${doc.score ?? 'N/A'}\` |`);
      }
      return lines.join('\n');
    }
    if ('id' in m && 'title' in m && 'content' in m) {
      const details = m as unknown as KnowledgeDetailsResult;
      return `# Knowledge Document Details (\`${details.id}\`)\n\n- **Title**: \`${details.title}\`\n\n## Content\n\n> ${details.content.split('\n').join('\n> ')}`;
    }
    if ('id' in m && 'explanation' in m) {
      const exp = m as unknown as KnowledgeExplanationResult;
      return `# Knowledge Explanation (\`${exp.id}\`)\n\n> ${exp.explanation.split('\n').join('\n> ')}`;
    }
    if ('projectId' in m && 'documents' in m && m.type !== 'KnowledgeSearchResult') {
      const ref = m as unknown as KnowledgeRefreshResult;
      const lines = ['# Knowledge Base Refreshed', '', `- **Project ID**: \`${ref.projectId}\``, '', '## Refreshed Documents'];
      for (const doc of ref.documents) {
        lines.push(`- [REFRESHED] ID: \`${doc.id}\` - \`${doc.title}\``);
      }
      return lines.join('\n');
    }
    if ('id' in m && 'success' in m && !('projectId' in m)) {
      const del = m as unknown as KnowledgeDeleteResult;
      return `# Knowledge Item Deleted\n\n- **ID**: \`${del.id}\``;
    }

    // 4. Diagnostics
    if ('checks' in m && 'allPassed' in m) {
      const diag = m as unknown as HealthDiagnosticResult;
      const lines = ['# Diagnostics Run', '', `- **All Passed**: ${diag.allPassed ? 'Yes' : 'No'}`, '', '| Check ID | Description | Status | Result / Error |', '| --- | --- | --- | --- |'];
      for (const check of diag.checks) {
        const details = check.result || check.error || 'N/A';
        const nameOrDesc = (check as unknown as Record<string, unknown>).description || (check as unknown as Record<string, unknown>).name || 'N/A';
        lines.push(`| \`${check.id}\` | ${nameOrDesc} | \`${check.status}\` | ${details} |`);
      }
      return lines.join('\n') + imgPlaceholder;
    }
    if ('health' in m && 'configuration' in m && 'connectivity' in m && 'environment' in m) {
      const rep = m as unknown as DiagnosticsReportResult;
      const lines = [
        '# MEMORA CLI UNIFIED DIAGNOSTICS REPORT',
        '',
        `- **Generated At**: \`${rep.generatedAt}\``,
        `- **All Passed**: ${rep.allPassed ? 'Yes' : 'No'}`,
        ''
      ];

      const formatSuite = (title: string, suite: HealthDiagnosticResult) => {
        lines.push(`## ${title}`);
        lines.push(`- **All Passed**: ${suite.allPassed ? 'Yes' : 'No'}`);
        lines.push('');
        lines.push('| Check ID | Status |');
        lines.push('| --- | --- |');
        for (const check of suite.checks) {
          lines.push(`| \`${check.id}\` | \`${check.status}\` |`);
        }
        lines.push('');
      };

      formatSuite('Health', rep.health);
      formatSuite('Configuration', rep.configuration);
      formatSuite('Connectivity', rep.connectivity);
      formatSuite('Environment', rep.environment);

      return lines.join('\n') + imgPlaceholder;
    }

    return `\`\`\`json\n${JSON.stringify(m, null, 2)}\n\`\`\``;
  }
}

export const markdownRenderer = new MarkdownRenderer();
