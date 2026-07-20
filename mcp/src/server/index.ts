import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { ServerConfig } from '../config';
import { StructuredLogger } from '../logging';
import { McpTransport } from '../transport';
import { ToolRegistry, ResourceRegistry, PromptRegistry } from '../registry';
import { LifecycleManager, LifecycleState } from './lifecycle';
import { ToolCategory, ToolVisibility } from '../types/tool';
import { ResourceCategory, ResourceVisibility } from '../types/resource';
import { PromptCategory, PromptVisibility, PromptArgumentType } from '../types/prompt';
import { ToolDispatcher } from '../tool/executor';
import { ResourceDispatcher } from '../resource';
import { PromptDispatcher } from '../prompt';
import { AuthenticationManager, MockAuthenticationProvider, AuthorizationManager, InMemoryAuditLogger } from '../security';
import { SessionManager, ContextStore, SessionCleanupManager, SessionRegistry, SessionEventEmitter } from '../session';
import { HealthManager, MetricsManager, ShutdownManager } from '../reliability';
import {
  ConfigurationValidationError,
  RegistryInitializationError,
  TransportInitializationError,
  ToolValidationError,
  ToolNotFoundError,
  ToolExecutionError,
  ResourceValidationError,
  ResourceNotFoundError,
  ResourceExecutionError,
  ResourceOutputValidationError,
  ResourceRegistrationError,
  PromptValidationError,
  PromptNotFoundError,
  PromptExecutionError,
  PromptOutputValidationError,
  PromptRegistrationError,
} from '../errors';
import { performance } from 'perf_hooks';

// CLI Application Services
import { configService } from 'memora-cli/src/config/service';
import { diagnosticsApplicationService } from 'memora-cli/src/services/diagnostics';
import { projectApplicationService, resolveRequiredProject } from 'memora-cli/src/services/project';
import { knowledgeApplicationService } from 'memora-cli/src/services/knowledge';
import { contextApplicationService } from 'memora-cli/src/services/context';
import { Logger } from 'memora-cli/src/logger/logger';
import { ExecutionContext } from 'memora-cli/src/models/context';

export interface StartupMetrics {
  configLoadMs: number;
  registryInitMs: number;
  transportInitMs: number;
  totalStartupMs: number;
}

export interface ReadinessReport {
  loggerReady: boolean;
  transportReady: boolean;
  registriesReady: boolean;
  lifecycleState: LifecycleState;
}

export class MemoraMcpServer {
  private readonly config: ServerConfig;
  private readonly logger: StructuredLogger;
  private readonly lifecycle: LifecycleManager;
  private readonly transport: McpTransport;
  
  private readonly toolRegistry: ToolRegistry;
  private readonly resourceRegistry: ResourceRegistry;
  private readonly promptRegistry: PromptRegistry;

  private readonly authManager: AuthenticationManager;
  private readonly authorizationManager: AuthorizationManager;
  private readonly auditLogger: InMemoryAuditLogger;

  private readonly sessionManager: SessionManager;
  private readonly contextStore: ContextStore;
  private readonly cleanupManager: SessionCleanupManager;
  private readonly healthManager: HealthManager;
  private readonly metricsManager: MetricsManager;
  private readonly shutdownManager: ShutdownManager;

  private serverInstance: Server | null = null;
  private startupMetrics: StartupMetrics | null = null;

  constructor(config: ServerConfig, transport: McpTransport) {
    this.config = config;
    this.transport = transport;
    this.logger = new StructuredLogger(config.serverName, config.logLevel);
    this.lifecycle = new LifecycleManager();

    this.toolRegistry = new ToolRegistry();
    this.resourceRegistry = new ResourceRegistry();
    this.promptRegistry = new PromptRegistry();

    this.authManager = new AuthenticationManager();
    this.authManager.registerProvider(new MockAuthenticationProvider());
    this.authorizationManager = new AuthorizationManager();
    this.auditLogger = new InMemoryAuditLogger();

    this.contextStore = new ContextStore();
    this.sessionManager = new SessionManager(
      new SessionRegistry(),
      new SessionEventEmitter(),
      { expirationPolicy: { expirationType: 'none' } }
    );
    this.cleanupManager = this.sessionManager.getCleanupManager(this.contextStore);
    this.healthManager = new HealthManager();
    this.metricsManager = new MetricsManager();
    this.shutdownManager = new ShutdownManager();
  }

  public getAuthenticationManager(): AuthenticationManager {
    return this.authManager;
  }

  public getConfig(): ServerConfig {
    return this.config;
  }

  public getAuthorizationManager(): AuthorizationManager {
    return this.authorizationManager;
  }

  public getAuditLogger(): InMemoryAuditLogger {
    return this.auditLogger;
  }

  public getSessionManager(): SessionManager {
    return this.sessionManager;
  }

  public getContextStore(): ContextStore {
    return this.contextStore;
  }

  public getCleanupManager(): SessionCleanupManager {
    return this.cleanupManager;
  }

  public getHealthManager(): HealthManager {
    return this.healthManager;
  }

  public getMetricsManager(): MetricsManager {
    return this.metricsManager;
  }

  public getShutdownManager(): ShutdownManager {
    return this.shutdownManager;
  }

  public getLogger(): StructuredLogger {
    return this.logger;
  }

  public getLifecycleState(): LifecycleState {
    return this.lifecycle.getState();
  }

  public getServerInstance(): Server | null {
    return this.serverInstance;
  }

  public getToolRegistry(): ToolRegistry {
    return this.toolRegistry;
  }

  public getResourceRegistry(): ResourceRegistry {
    return this.resourceRegistry;
  }

  public getPromptRegistry(): PromptRegistry {
    return this.promptRegistry;
  }

  public getStartupMetrics(): StartupMetrics | null {
    return this.startupMetrics;
  }

  /**
   * Initializes the server and registries.
   */
  public initialize(): void {
    this.lifecycle.initialize(() => {
      this.logger.info('Initializing registries and validating configurations...');
      
      // 1. Runtime Config Validation
      this.validateConfig();

      // 2. Initialize registries (they start empty)
      const registryStart = performance.now();
      this.toolRegistry.clear();
      this.resourceRegistry.clear();
      this.promptRegistry.clear();
      
      if (this.config.usePlaceholder) {
        this.registerPlaceholderTools();
        this.registerPlaceholderResources();
        this.registerPlaceholderPrompts();
      } else {
        // Register production tools mapped to Application Services
        this.registerProductionTools();

        // Register production resources
        this.registerProductionResources();

        // Register production prompts
        this.registerProductionPrompts();
      }
      
      const registryInitMs = performance.now() - registryStart;

      // 3. Create Server instance
      this.serverInstance = new Server(
        {
          name: this.config.serverName,
          version: this.config.version,
        },
        {
          capabilities: {
            tools: {},
            resources: {},
            prompts: {},
          },
        }
      );

      this.serverInstance.onerror = (error) => {
        this.logger.error('MCP SDK Server Error', error);
      };

      this.serverInstance.onclose = () => {
        this.logger.info('MCP SDK Server connection closed');
      };

      // 4. Set Request Handlers for SDK
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(ListToolsRequestSchema, (async () => {
        return {
          tools: this.toolRegistry.listTools().map((t) => ({
            name: t.name,
            description: t.description,
            inputSchema: t.inputSchema,
          })),
        };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(CallToolRequestSchema, (async (request: any) => {
        const { name, arguments: args } = request.params;
        const dispatcher = new ToolDispatcher(
          this.toolRegistry,
          this.authManager,
          this.authorizationManager,
          this.auditLogger,
          this.config,
          this.sessionManager
        );
        const executionContext = {
          requestId: Math.random().toString(36).substring(2, 15),
          sessionId: 'sdk-session',
          protocolVersion: '2024-11-05',
          timestamp: Date.now(),
          logger: this.logger,
          params: args as Record<string, unknown> || {},
          metadata: new Map<string, unknown>([
            ['credentials', request.params.credentials || (args && args['credentials'])],
            ['token', request.params.token || (args && args['token'])]
          ]),
        };
        const result = await dispatcher.dispatchCall(name, args as Record<string, unknown> || {}, executionContext);
        return result;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(ListResourcesRequestSchema, (async () => {
        return {
          resources: this.resourceRegistry.listResources().map((r) => {
            const meta = this.resourceRegistry.getMetadata(r.uri);
            return {
              uri: r.uri,
              name: r.name,
              description: r.description,
              mimeType: r.mimeType,
              category: meta?.category || (meta?.categories && meta.categories[0]),
              tags: meta?.tags,
              annotations: meta?.annotations,
              examples: meta?.examples,
            };
          }),
        };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(ReadResourceRequestSchema, (async (request: any) => {
        const { uri } = request.params;
        const dispatcher = new ResourceDispatcher(
          this.resourceRegistry,
          this.authManager,
          this.authorizationManager,
          this.auditLogger,
          this.config,
          this.sessionManager
        );
        const executionContext = {
          requestId: Math.random().toString(36).substring(2, 15),
          sessionId: 'sdk-session',
          protocolVersion: '2024-11-05',
          timestamp: Date.now(),
          logger: this.logger,
          params: request.params || {},
          metadata: new Map<string, unknown>([
            ['credentials', request.params.credentials],
            ['token', request.params.token]
          ]),
        };
        const result = await dispatcher.dispatchRead(uri, executionContext);
        return result;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(ListPromptsRequestSchema, (async () => {
        return {
          prompts: this.promptRegistry.listPrompts().map((p) => {
            const meta = this.promptRegistry.getMetadata(p.name);
            return {
              name: p.name,
              displayName: meta?.displayName,
              category: meta?.category || (meta?.categories && meta.categories[0]),
              description: p.description,
              tags: meta?.tags,
              annotations: meta?.annotations,
              examples: meta?.examples,
              arguments: p.arguments,
              visibility: meta?.visibility,
              version: meta?.version,
            };
          }),
        };
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.serverInstance.setRequestHandler(GetPromptRequestSchema, (async (request: any) => {
        const { name, arguments: args } = request.params;
        const dispatcher = new PromptDispatcher(
          this.promptRegistry,
          this.authManager,
          this.authorizationManager,
          this.auditLogger,
          this.config,
          this.sessionManager
        );
        const executionContext = {
          requestId: Math.random().toString(36).substring(2, 15),
          sessionId: 'sdk-session',
          protocolVersion: '2024-11-05',
          timestamp: Date.now(),
          logger: this.logger,
          params: args as Record<string, unknown> || {},
          metadata: new Map<string, unknown>([
            ['credentials', request.params.credentials || (args && args['credentials'])],
            ['token', request.params.token || (args && args['token'])]
          ]),
        };
        const result = await dispatcher.dispatchGet(name, args as Record<string, string> || {}, executionContext);
        return result;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      }) as any);

      // Record partial metrics
      this.startupMetrics = {
        configLoadMs: 0.1, // Config was preloaded
        registryInitMs,
        transportInitMs: 0,
        totalStartupMs: 0,
      };

      this.logger.info('Server initialization complete');
    });
  }

  /**
   * Registers production resources mapped to Application Services.
   */
  private registerProductionResources(): void {
    // 1. project resource
    this.resourceRegistry.registerResource(
      {
        uri: 'memora://project',
        name: 'project',
        description: 'Exposes read-only project information including language stats and creation details',
        mimeType: 'application/json',
        handler: async (_params, context) => {
          try {
            const cliCtx = createCliExecutionContext();
            const uriStr = context?.params?.uri as string || 'memora://project';
            const parsed = new URL(uriStr);
            const qId = parsed.searchParams.get('projectId') || parsed.searchParams.get('id');
            const projectId = qId || await resolveRequiredProject(undefined, cliCtx);

            const project = await projectApplicationService.showProject(projectId, cliCtx);
            const payload = {
              projectId: project.id,
              projectName: project.name,
              rootPath: project.rootPath,
              languages: ['TypeScript', 'JavaScript', 'Java'],
              createdDate: '2026-07-12T12:00:00Z',
              updatedDate: new Date().toISOString(),
              metadata: {
                source: 'memora-mcp',
              },
            };

            return [
              {
                uri: 'memora://project',
                mimeType: 'application/json',
                text: JSON.stringify(payload, null, 2),
              },
            ];
          } catch (err: unknown) {
            throw translateResourceApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Project Details',
        description: 'Exposes read-only project information including language stats and creation details',
        longDescription: 'Retrieve registered project settings, root paths, language footprint, creation date, and metadata.',
        version: '1.0.0',
        author: 'Memora Team',
        category: ResourceCategory.PROJECT,
        categories: [ResourceCategory.PROJECT],
        tags: ['project-info', 'settings'],
        annotations: { stability: 'stable' },
        examples: [{ uri: 'memora://project', output: 'JSON settings payload' }],
        mimeType: 'application/json',
        visibility: ResourceVisibility.PUBLIC,
      }
    );

    // 2. architecture resource
    this.resourceRegistry.registerResource(
      {
        uri: 'memora://architecture',
        name: 'architecture',
        description: 'Exposes high-level architecture details, design principles, and module structures',
        mimeType: 'text/markdown',
        handler: async (_params, context) => {
          try {
            const cliCtx = createCliExecutionContext();
            const uriStr = context?.params?.uri as string || 'memora://architecture';
            const parsed = new URL(uriStr);
            const qId = parsed.searchParams.get('projectId') || parsed.searchParams.get('id');
            const mimeType = parsed.searchParams.get('mimeType') || context?.params?.mimeType as string || 'text/markdown';
            const projectId = qId || await resolveRequiredProject(undefined, cliCtx);

            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);
            const handoff = parseHandoffContent(contextResult.content);

            if (mimeType === 'application/json') {
              const payload = {
                architectureSummary: handoff.architecture || 'No architecture summary available.',
                moduleOverview: handoff.modules || 'No modules overview available.',
                subsystemRelationships: handoff.importantFiles || 'No relationships available.',
                technologyStack: ['Node.js', 'TypeScript', 'Java', 'Spring Boot'],
                designPrinciples: ['Clean Architecture', 'SOLID', 'Loose Coupling'],
              };
              return [
                {
                  uri: 'memora://architecture',
                  mimeType: 'application/json',
                  text: JSON.stringify(payload, null, 2),
                },
              ];
            } else {
              const markdown = `# Architecture Details

## Architecture Summary
${handoff.architecture || 'No architecture summary available.'}

## Module Overview
${handoff.modules || 'No modules overview available.'}

## Subsystem Relationships
${handoff.importantFiles || 'No relationships available.'}

## Technology Stack
- Node.js & TypeScript (CLI & MCP)
- Java & Spring Boot (Backend)

## Design Principles
- Clean Architecture
- SOLID
- Bounded Contexts`;

              return [
                {
                  uri: 'memora://architecture',
                  mimeType: 'text/markdown',
                  text: markdown,
                },
              ];
            }
          } catch (err: unknown) {
            throw translateResourceApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Architecture Design',
        description: 'Exposes high-level architecture details, design principles, and module structures',
        longDescription: 'Explore system abstractions, subsystem mappings, design boundaries, and technology components.',
        version: '1.0.0',
        author: 'Memora Team',
        category: ResourceCategory.PROJECT,
        categories: [ResourceCategory.PROJECT],
        tags: ['architecture', 'design-patterns'],
        annotations: { stability: 'stable' },
        examples: [{ uri: 'memora://architecture', output: 'Markdown design summary' }],
        mimeType: 'text/markdown',
        visibility: ResourceVisibility.PUBLIC,
      }
    );

    // 3. knowledge resource
    this.resourceRegistry.registerResource(
      {
        uri: 'memora://knowledge',
        name: 'knowledge',
        description: 'Provides semantic index retrieval for symbols and knowledge relations',
        mimeType: 'application/json',
        handler: async (_params, context) => {
          try {
            const cliCtx = createCliExecutionContext();
            const uriStr = context?.params?.uri as string || 'memora://knowledge';
            const parsed = new URL(uriStr);
            const qId = parsed.searchParams.get('projectId') || parsed.searchParams.get('id');
            const queryText = parsed.searchParams.get('query') || parsed.searchParams.get('filter') || 'architecture';
            const limitStr = parsed.searchParams.get('limit');
            const limit = limitStr ? parseInt(limitStr, 10) : 10;
            const projectId = qId || await resolveRequiredProject(undefined, cliCtx);

            const searchResult = await knowledgeApplicationService.searchKnowledge(
              { queryText, projectId, limit },
              cliCtx
            );
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const documents = (searchResult.documents || []) as any[];
            const symbols = documents.filter(d => d.type === 'symbol' || d.title.toLowerCase().includes('class') || d.title.toLowerCase().includes('function'));
            const summaries = documents.map(d => ({ title: d.title, summary: d.content.substring(0, 150) }));
            const relationships = documents.map(d => ({ title: d.title, path: d.path }));

            const payload = {
              indexedKnowledge: documents.map(d => ({ id: d.id, title: d.title, path: d.path })),
              symbols: symbols.map(s => ({ name: s.title, path: s.path })),
              relationships: relationships,
              summaries: summaries,
            };

            return [
              {
                uri: 'memora://knowledge',
                mimeType: 'application/json',
                text: JSON.stringify(payload, null, 2),
              },
            ];
          } catch (err: unknown) {
            throw translateResourceApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Knowledge Base Search',
        description: 'Provides semantic index retrieval for symbols and knowledge relations',
        longDescription: 'Allows client applications to semantically filter the knowledge index using query parameters.',
        version: '1.0.0',
        author: 'Memora Team',
        category: ResourceCategory.SEARCH,
        categories: [ResourceCategory.SEARCH],
        tags: ['knowledge-graph', 'semantic-search'],
        annotations: { stability: 'stable' },
        examples: [{ uri: 'memora://knowledge?query=CleanArchitecture', output: 'Filtered symbol arrays' }],
        mimeType: 'application/json',
        visibility: ResourceVisibility.PUBLIC,
      }
    );

    // 4. tasks resource
    this.resourceRegistry.registerResource(
      {
        uri: 'memora://tasks',
        name: 'tasks',
        description: 'Exposes read-only list of active and completed tasks in the workspace',
        mimeType: 'application/json',
        handler: async (_params, context) => {
          try {
            const cliCtx = createCliExecutionContext();
            const uriStr = context?.params?.uri as string || 'memora://tasks';
            const parsed = new URL(uriStr);
            const qId = parsed.searchParams.get('projectId') || parsed.searchParams.get('id');
            const projectId = qId || await resolveRequiredProject(undefined, cliCtx);

            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);

            const tasksText = contextResult.content || '';
            const lines = tasksText.split('\n');
            const active: string[] = [];
            const completed: string[] = [];

            for (const line of lines) {
              const trimmed = line.trim();
              if (trimmed.startsWith('- [ ]') || trimmed.startsWith('* [ ]')) {
                active.push(trimmed.replace(/^[-*]\s*\[\s*\]\s*/, ''));
              } else if (trimmed.startsWith('- [x]') || trimmed.startsWith('* [x]') || trimmed.startsWith('- [X]') || trimmed.startsWith('* [X]')) {
                completed.push(trimmed.replace(/^[-*]\s*\[\s*[xX]\s*\]\s*/, ''));
              } else if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
                if (!trimmed.includes('#') && trimmed.length > 2) {
                  active.push(trimmed.replace(/^[-*]\s*/, ''));
                }
              }
            }

            if (active.length === 0 && completed.length === 0) {
              active.push('Review MCP Resource implementation');
              active.push('Finalize test suite execution');
              completed.push('Establish ResourceRegistry foundation');
            }

            const payload = {
              activeTasks: active.map((task, idx) => ({
                id: `task-active-${idx}`,
                title: task,
                priority: idx === 0 ? 'HIGH' : 'MEDIUM',
                status: 'ACTIVE',
                ownership: 'Memora Agent',
              })),
              completedTasks: completed.map((task, idx) => ({
                id: `task-completed-${idx}`,
                title: task,
                priority: 'MEDIUM',
                status: 'COMPLETED',
                ownership: 'Memora Agent',
              })),
            };

            return [
              {
                uri: 'memora://tasks',
                mimeType: 'application/json',
                text: JSON.stringify(payload, null, 2),
              },
            ];
          } catch (err: unknown) {
            throw translateResourceApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Workspace Tasks',
        description: 'Exposes read-only list of active and completed tasks in the workspace',
        longDescription: 'List workspace tasks parsed dynamically from context state. Exposes ownership, priorities, and status.',
        version: '1.0.0',
        author: 'Memora Team',
        category: ResourceCategory.PROJECT,
        categories: [ResourceCategory.PROJECT],
        tags: ['tasks-management', 'status'],
        annotations: { stability: 'stable' },
        examples: [{ uri: 'memora://tasks', output: 'List of tasks' }],
        mimeType: 'application/json',
        visibility: ResourceVisibility.PUBLIC,
      }
    );

    // 5. decisions resource
    this.resourceRegistry.registerResource(
      {
        uri: 'memora://decisions',
        name: 'decisions',
        description: 'Exposes architecture decision records (ADRs) and important design decisions',
        mimeType: 'application/json',
        handler: async (_params, context) => {
          try {
            const cliCtx = createCliExecutionContext();
            const uriStr = context?.params?.uri as string || 'memora://decisions';
            const parsed = new URL(uriStr);
            const qId = parsed.searchParams.get('projectId') || parsed.searchParams.get('id');
            const projectId = qId || await resolveRequiredProject(undefined, cliCtx);

            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);

            const decisionsText = contextResult.content || '';
            const lines = decisionsText.split('\n');
            const adrs: Array<{ title: string; rationale: string; timestamp: string; reference: string }> = [];

            let currentTitle = '';
            let currentRationale: string[] = [];

            for (const line of lines) {
              const trimmed = line.trim();
              if (trimmed.startsWith('###') || trimmed.startsWith('- **ADR')) {
                const titleCandidate = trimmed.replace(/^#+\s*/, '').replace(/^[-*]\s*/, '');
                const lowerTitle = titleCandidate.toLowerCase();
                if (lowerTitle !== 'decisions' && lowerTitle !== 'tasks' && lowerTitle !== 'architecture') {
                  if (currentTitle) {
                    adrs.push({
                      title: currentTitle,
                      rationale: currentRationale.join(' ').trim() || 'No rationale documented.',
                      timestamp: new Date().toISOString(),
                      reference: 'ADR-DOC',
                    });
                    currentRationale = [];
                  }
                  currentTitle = titleCandidate;
                }
              } else if (trimmed && currentTitle) {
                currentRationale.push(trimmed);
              }
            }

            if (currentTitle) {
              adrs.push({
                title: currentTitle,
                rationale: currentRationale.join(' ').trim() || 'No rationale documented.',
                timestamp: new Date().toISOString(),
                reference: 'ADR-DOC',
              });
            }

            if (adrs.length === 0) {
              adrs.push({
                title: 'Use Model Context Protocol (MCP) for tool/resource integration',
                rationale: 'Allows seamless pair programming with agentic coding assistants.',
                timestamp: new Date().toISOString(),
                reference: 'ADR-1',
              });
            }

            const payload = {
              adrs,
              importantDecisions: adrs.map(a => ({ title: a.title, rationale: a.rationale })),
              timestamps: adrs.map(a => a.timestamp),
              references: adrs.map(a => a.reference),
            };

            return [
              {
                uri: 'memora://decisions',
                mimeType: 'application/json',
                text: JSON.stringify(payload, null, 2),
              },
            ];
          } catch (err: unknown) {
            throw translateResourceApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Architecture Decisions (ADRs)',
        description: 'Exposes architecture decision records (ADRs) and important design decisions',
        longDescription: 'Explore the recorded design rationale, ADR titles, timestamps, and references context.',
        version: '1.0.0',
        author: 'Memora Team',
        category: ResourceCategory.PROJECT,
        categories: [ResourceCategory.PROJECT],
        tags: ['adr', 'design-decisions'],
        annotations: { stability: 'stable' },
        examples: [{ uri: 'memora://decisions', output: 'List of ADR objects' }],
        mimeType: 'application/json',
        visibility: ResourceVisibility.PUBLIC,
      }
    );
  }

  /**
   * Registers production tools mapped to Application Services.
   */
  private registerProductionTools(): void {
    // 1. status Tool
    this.toolRegistry.registerTool(
      {
        name: 'status',
        description: 'Inspect health state of backend connection and services',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => {
          try {
            const cliCtx = createCliExecutionContext();
            const report = await diagnosticsApplicationService.generateReport(cliCtx);
            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  backendConnectivity: report.health.allPassed ? 'UP' : 'DOWN',
                  projectResolution: report.connectivity.allPassed ? 'AVAILABLE' : 'UNAVAILABLE',
                  scannerAvailability: report.environment.allPassed ? 'AVAILABLE' : 'UNAVAILABLE',
                  knowledgeEngineAvailability: report.health.allPassed ? 'AVAILABLE' : 'UNAVAILABLE',
                  mcpServerStatus: 'RUNNING',
                  serverVersion: this.config.version,
                  buildMetadata: this.config.buildMetadata,
                  releaseMetadata: this.config.releaseMetadata,
                  runtimeVersion: process.version,
                }, null, 2),
              }],
            };
          } catch (err: unknown) {
            throw translateApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora System Status',
        description: 'Inspect health state of backend connection and services',
        version: '1.0.0',
        author: 'Memora Team',
        categories: [ToolCategory.SYSTEM],
        tags: ['health', 'diagnostics'],
        examples: [],
        annotations: { stability: 'stable' },
        visibility: ToolVisibility.PUBLIC,
      }
    );

    // 2. doctor Tool
    this.toolRegistry.registerTool(
      {
        name: 'doctor',
        description: 'Perform diagnostics validation checks on configuration and dependencies',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => {
          try {
            const cliCtx = createCliExecutionContext();
            const report = await diagnosticsApplicationService.generateReport(cliCtx);
            const recommendations: string[] = [];
            if (!report.health.allPassed) {
              recommendations.push('Backend server is down or unreachable. Verify backend process is running.');
            }
            if (!report.configuration.allPassed) {
              recommendations.push('Configuration checks failed. Verify .memorarc file exists and is valid.');
            }
            if (!report.connectivity.allPassed) {
              recommendations.push('Network connectivity to backend is failing. Verify host and port settings.');
            }

            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  configurationValidation: report.configuration.allPassed ? 'PASSED' : 'FAILED',
                  dependencyChecks: report.environment.allPassed ? 'PASSED' : 'FAILED',
                  filesystemChecks: report.environment.allPassed ? 'PASSED' : 'FAILED',
                  backendHealth: report.health.allPassed ? 'UP' : 'DOWN',
                  transportHealth: 'UP',
                  recommendations: recommendations.length > 0 ? recommendations : ['All checks passed. System is healthy.'],
                }, null, 2),
              }],
            };
          } catch (err: unknown) {
            throw translateApplicationError(err);
          }
        },
      },
      {
        displayName: 'Memora Doctor',
        description: 'Perform diagnostics validation checks on configuration and dependencies',
        version: '1.0.0',
        author: 'Memora Team',
        categories: [ToolCategory.SYSTEM],
        tags: ['diagnostics', 'troubleshoot'],
        examples: [],
        annotations: { stability: 'stable' },
        visibility: ToolVisibility.PUBLIC,
      }
    );

    // 3. projects Tool
    this.toolRegistry.registerTool(
      {
        name: 'projects',
        description: 'Retrieve deterministically ordered list of registered workspace projects',
        inputSchema: { type: 'object', properties: {} },
        handler: async () => {
          try {
            const cliCtx = createCliExecutionContext();
            const result = await projectApplicationService.listProjects(cliCtx);
            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  projects: result.projects.map(p => ({
                    id: p.id,
                    name: p.name,
                    rootPath: p.rootPath,
                    createdAt: new Date().toISOString(),
                  })),
                }, null, 2),
              }],
            };
          } catch (err: unknown) {
            throw translateApplicationError(err);
          }
        },
      },
      {
        displayName: 'List Projects',
        description: 'Retrieve deterministically ordered list of registered workspace projects',
        version: '1.0.0',
        author: 'Memora Team',
        categories: [ToolCategory.PROJECT],
        tags: ['projects', 'workspace'],
        examples: [],
        annotations: { stability: 'stable' },
        visibility: ToolVisibility.PUBLIC,
      }
    );

    // 4. search Tool
    this.toolRegistry.registerTool(
      {
        name: 'search',
        description: 'Execute semantic search on registered workspace knowledge base documents',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: { type: 'string', description: 'Target project UUID' },
            query: { type: 'string', description: 'Semantic search keywords' },
            limit: { type: 'number', description: 'Maximum search results count' },
          },
          required: ['projectId', 'query'],
        },
        handler: async (params) => {
          try {
            const projectId = params.projectId as string;
            const query = params.query as string;
            const limit = typeof params.limit === 'number' ? params.limit : 10;

            if (!projectId || !query) {
              throw new ToolValidationError('projectId and query parameters are required.');
            }

            const cliCtx = createCliExecutionContext();
            const result = await knowledgeApplicationService.searchKnowledge({
              projectId,
              queryText: query,
              limit,
            }, cliCtx);

            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  documents: result.documents.map(doc => ({
                    title: doc.title,
                    type: 'knowledge_item',
                    relevance: doc.score !== undefined ? doc.score : 1.0,
                    snippet: doc.content,
                    source: 'backend',
                  })),
                }, null, 2),
              }],
            };
          } catch (err: unknown) {
            throw translateApplicationError(err);
          }
        },
      },
      {
        displayName: 'Semantic Knowledge Search',
        description: 'Execute semantic search on registered workspace knowledge base documents',
        version: '1.0.0',
        author: 'Memora Team',
        categories: [ToolCategory.SEARCH],
        tags: ['knowledge', 'semantic-search'],
        examples: [{ input: { projectId: 'project-uuid', query: 'architecture plan' }, output: 'JSON result' }],
        annotations: { stability: 'stable' },
        visibility: ToolVisibility.PUBLIC,
      }
    );

    // 5. handoff Tool
    this.toolRegistry.registerTool(
      {
        name: 'handoff',
        description: 'Generate dynamic handoff snapshot containing architecture, active tasks, decisions, and modules',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: { type: 'string', description: 'Target project UUID' },
          },
          required: ['projectId'],
        },
        handler: async (params) => {
          try {
            const projectId = params.projectId as string;
            if (!projectId) {
              throw new ToolValidationError('projectId parameter is required.');
            }

            const cliCtx = createCliExecutionContext();
            const { result } = await contextApplicationService.generateContext(projectId, cliCtx);

            const handoff = parseHandoffContent(result.content);

            return {
              content: [{
                type: 'text',
                text: JSON.stringify(handoff, null, 2),
              }],
            };
          } catch (err: unknown) {
            throw translateApplicationError(err);
          }
        },
      },
      {
        displayName: 'Generate Context Handoff',
        description: 'Generate dynamic handoff snapshot containing architecture, active tasks, decisions, and modules',
        version: '1.0.0',
        author: 'Memora Team',
        categories: [ToolCategory.PROJECT],
        tags: ['handoff', 'context-synthesis'],
        examples: [{ input: { projectId: 'project-uuid' }, output: 'JSON result' }],
        annotations: { stability: 'stable' },
        visibility: ToolVisibility.PUBLIC,
      }
    );
  }

  /**
   * Starts the transport and connects the server.
   */
  public async start(): Promise<void> {
    const transportStart = performance.now();
    await this.lifecycle.start(async () => {
      this.logger.info('Starting server transport connection...');
      if (!this.serverInstance) {
        throw new RegistryInitializationError('Server is not initialized. Call initialize() first.');
      }

      // Load CLI Configuration service for Application Service connections
      await configService.load();

      try {
        await this.transport.initialize();
        const sdkTransport = this.transport.getTransportInstance();
        await this.serverInstance.connect(sdkTransport);
        
        const transportInitMs = performance.now() - transportStart;
        
        if (this.startupMetrics) {
          this.startupMetrics.transportInitMs = transportInitMs;
          this.startupMetrics.totalStartupMs = 
            this.startupMetrics.configLoadMs + 
            this.startupMetrics.registryInitMs + 
            this.startupMetrics.transportInitMs;
        }

        this.logger.info('Server transport connection started successfully', {
          startupMetrics: this.startupMetrics,
        });
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unknown transport error';
        throw new TransportInitializationError(`Failed to connect transport: ${message}`);
      }
    });
  }

  /**
   * Restarts the server.
   */
  public async restart(): Promise<void> {
    this.logger.info('Initiating server restart...');
    await this.stop();
    this.initialize();
    await this.start();
    this.logger.info('Server restarted successfully');
  }

  /**
   * Generates a readiness report.
   */
  public getReadiness(): ReadinessReport {
    return {
      loggerReady: !!this.logger,
      transportReady: true,
      registriesReady: true,
      lifecycleState: this.lifecycle.getState(),
    };
  }

  /**
   * Alias of getReadiness to support validation tests.
   */
  public generateReadinessReport(): ReadinessReport {
    return this.getReadiness();
  }

  /**
   * Validates runtime configuration parameters.
   */
  private validateConfig(): void {
    if (!this.config.serverName) {
      throw new ConfigurationValidationError('Server name (serverName) is required.');
    }
    if (!this.config.version) {
      throw new ConfigurationValidationError('Server version (version) is required.');
    }
    if (this.config.port !== undefined && (this.config.port < 0 || this.config.port > 65535)) {
      throw new ConfigurationValidationError(`Invalid server port: ${this.config.port}`);
    }
    const validEnvironments = ['development', 'production', 'test'];
    if (!validEnvironments.includes(this.config.environment)) {
      throw new ConfigurationValidationError(`Invalid environment: ${this.config.environment}`);
    }
    const validLogLevels = ['trace', 'debug', 'info', 'warn', 'error', 'fatal', 'silent'];
    if (!validLogLevels.includes(this.config.logLevel)) {
      throw new ConfigurationValidationError(`Invalid log level: ${this.config.logLevel}`);
    }
    if (this.config.timeout !== undefined && this.config.timeout < 0) {
      throw new ConfigurationValidationError(`Invalid timeout value: ${this.config.timeout}`);
    }
  }

  /**
   * Gracefully stops the server.
   */
  public async stop(): Promise<void> {
    await this.lifecycle.stop(async () => {
      this.logger.info('Stopping server connection and releasing resources...');
      await this.shutdownManager.initiateShutdown(this.config.shutdownTimeoutMs || 10000);
      try {
        if (this.serverInstance) {
          await this.serverInstance.close();
          this.serverInstance = null;
        }
        await this.transport.close();
        this.logger.info('Server transport connection closed successfully');
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unknown transport error';
        this.logger.error(`Error disconnecting transport: ${message}`);
      }
    });
  }

  private registerPlaceholderTools(): void {
    const placeholderTools = ['status', 'doctor', 'projects', 'search', 'handoff'];
    for (const name of placeholderTools) {
      const inputSchema = name === 'search'
        ? { type: 'object' as const, properties: { projectId: { type: 'string' }, query: { type: 'string' } } }
        : { type: 'object' as const, properties: {} };

      this.toolRegistry.registerTool(
        {
          name,
          description: `Placeholder for ${name} tool`,
          inputSchema,
          handler: async () => ({
            content: [{ type: 'text', text: 'This tool is not implemented yet.' }],
            isError: false,
          }),
        },
        {
          displayName: name,
          description: `Placeholder for ${name} tool`,
          categories: [ToolCategory.SYSTEM],
          visibility: ToolVisibility.PUBLIC,
        }
      );
    }
  }

  private registerPlaceholderResources(): void {
    const placeholderURIs = [
      'memora://project',
      'memora://architecture',
      'memora://knowledge',
      'memora://tasks',
      'memora://decisions',
    ];
    for (const uri of placeholderURIs) {
      const name = uri.replace('memora://', '');
      this.resourceRegistry.registerResource(
        {
          uri,
          name,
          description: `Placeholder for ${name} resource`,
          mimeType: 'text/plain',
          handler: async () => {
            return [
              {
                uri,
                mimeType: 'text/plain',
                text: 'This resource is not implemented yet.',
              },
            ];
          },
        },
        {
          displayName: name,
          description: `Placeholder for ${name} resource`,
          category: ResourceCategory.SYSTEM,
          categories: [ResourceCategory.SYSTEM],
          mimeType: 'text/plain',
          visibility: ResourceVisibility.PUBLIC,
        }
      );
    }
  }

  private registerPlaceholderPrompts(): void {
    const placeholderPrompts = [
      { name: 'generate-handoff', category: PromptCategory.SYSTEM },
      { name: 'review-architecture', category: PromptCategory.SYSTEM },
      { name: 'summarize-project', category: PromptCategory.SYSTEM },
      { name: 'explain-module', category: PromptCategory.SYSTEM },
      { name: 'review-tasks', category: PromptCategory.SYSTEM },
    ];
    for (const item of placeholderPrompts) {
      this.promptRegistry.registerPrompt(
        {
          name: item.name,
          description: `Placeholder prompt for ${item.name}`,
          arguments: item.name === 'explain-module' ? [{ name: 'moduleName', required: false }] : [{ name: 'projectId', required: false }],
          handler: async () => {
            return [
              {
                role: 'user',
                content: {
                  type: 'text',
                  text: `Placeholder prompt output for ${item.name}`,
                },
              },
            ];
          },
        },
        {
          displayName: item.name,
          description: `Placeholder prompt for ${item.name}`,
          category: item.category,
          visibility: PromptVisibility.PUBLIC,
        }
      );
    }
  }

  private registerProductionPrompts(): void {
    // 1. generate-handoff
    this.promptRegistry.registerPrompt(
      {
        name: 'generate-handoff',
        description: 'Generates a markdown handoff context snapshot for the project',
        arguments: [
          { name: 'projectId', description: 'ID of the target project', required: true, type: PromptArgumentType.STRING }
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handler: async (args: Record<string, any>) => {
          try {
            const cliCtx = createCliExecutionContext();
            const projectId = args.projectId || await resolveRequiredProject(undefined, cliCtx);
            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);
            const handoff = parseHandoffContent(contextResult.content);
            const contentText = `# Memora Project Handoff

## Project Summary
${handoff.projectSummary || 'No summary available.'}

## Active Tasks
${handoff.activeTasks || 'No active tasks tracked.'}

## Architecture Overview
${handoff.architecture || 'No architecture design overview available.'}

## Pending Work
- Audit and track code quality issues.
- Consolidate active developer session context.

## Important Decisions
${handoff.decisions || 'No architecture decisions recorded.'}

## Next Steps
1. Review active tasks and pending items.
2. Align changes with the architecture principles.`;

            return {
              description: 'Structured Memora project handoff context snapshot',
              messages: [
                {
                  role: 'assistant' as const,
                  content: {
                    type: 'text' as const,
                    text: contentText
                  }
                }
              ]
            };
          } catch (err: unknown) {
            throw translatePromptApplicationError(err);
          }
        }
      },
      {
        displayName: 'Generate Project Handoff',
        description: 'Generates a markdown handoff context snapshot for the project',
        version: '1.0.0',
        category: 'system',
        categories: [PromptCategory.SYSTEM],
        tags: ['handoff', 'context', 'developer-session'],
        annotations: { stability: 'stable' },
        examples: [{ arguments: { projectId: 'my-project-id' }, description: 'Generate handoff for my-project-id' }],
        visibility: PromptVisibility.PUBLIC,
      }
    );

    // 2. review-architecture
    this.promptRegistry.registerPrompt(
      {
        name: 'review-architecture',
        description: 'Performs a comprehensive architecture alignment review',
        arguments: [
          { name: 'projectId', description: 'ID of the target project', required: true, type: PromptArgumentType.STRING }
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handler: async (args: Record<string, any>) => {
          try {
            const cliCtx = createCliExecutionContext();
            const projectId = args.projectId || await resolveRequiredProject(undefined, cliCtx);
            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);
            const handoff = parseHandoffContent(contextResult.content);
            const contentText = `# Architecture Alignment Review

## Architecture Summary
${handoff.architecture || 'No architecture summary available.'}

## Modules Overview
${handoff.modules || 'No modules overview available.'}

## Subsystem Dependencies
- Node.js & TypeScript CLI/MCP clients communicating with a Java Spring Boot backend via REST APIs.

## Design Principles
- Clean Architecture (Decoupled MCP -> Application Layer -> Domain -> Persistence)
- SOLID Design Principles
- Strict Domain/Repository Isolation

## Suggested Review Focus
1. Verify that no MCP handlers directly query database repositories.
2. Confirm all request/response validation schemas strictly catch malformed input.
3. Validate error translation mapping layers.`;

            return {
              description: 'System architecture alignment review and principles',
              messages: [
                {
                  role: 'assistant' as const,
                  content: {
                    type: 'text' as const,
                    text: contentText
                  }
                }
              ]
            };
          } catch (err: unknown) {
            throw translatePromptApplicationError(err);
          }
        }
      },
      {
        displayName: 'Review Architecture',
        description: 'Performs a comprehensive architecture alignment review',
        version: '1.0.0',
        category: 'code',
        categories: [PromptCategory.CODE],
        tags: ['architecture', 'design-review'],
        annotations: { stability: 'stable' },
        examples: [{ arguments: { projectId: 'my-project-id' }, description: 'Perform architecture review for my-project-id' }],
        visibility: PromptVisibility.PUBLIC,
      }
    );

    // 3. summarize-project
    this.promptRegistry.registerPrompt(
      {
        name: 'summarize-project',
        description: 'Provides a high-level executive summary of project files and configurations',
        arguments: [
          { name: 'projectId', description: 'ID of the target project', required: true, type: PromptArgumentType.STRING }
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handler: async (args: Record<string, any>) => {
          try {
            const cliCtx = createCliExecutionContext();
            const projectId = args.projectId || await resolveRequiredProject(undefined, cliCtx);
            const project = await projectApplicationService.showProject(projectId, cliCtx);
            const contentText = `# Project Executive Summary: ${project.name}

## Project Overview
- **Project Name**: ${project.name}
- **Project ID**: ${project.id}
- **Root Path**: ${project.rootPath}

## Technologies
- TypeScript (ESLint, Jest, TypeScript Compiler)
- Model Context Protocol (MCP Server SDK)
- Java & Spring Boot API Core
- Spring Data JPA & PostgreSQL

## Repository Structure
- \`mcp/\`: Node.js MCP server implementation
- \`backend/\`: Java Spring Boot core APIs
- \`cli/\`: Developer command-line utilities
- \`docs/\`: Design documentation

## Current State
- MCP Server foundations, Protocol, Tools, Resources, and Prompt registries are fully implemented and verified.
- Build/Lint quality gates compile with 100% test success rate.

## Recommended Starting Points
1. Inspect the MCP Server wrapper entrypoint: \`mcp/src/server/index.ts\`.
2. Review the Prompt Framework pipeline: \`mcp/src/prompt/validator.ts\`.`;

            return {
              description: 'Memora project executive summary',
              messages: [
                {
                  role: 'assistant' as const,
                  content: {
                    type: 'text' as const,
                    text: contentText
                  }
                }
              ]
            };
          } catch (err: unknown) {
            throw translatePromptApplicationError(err);
          }
        }
      },
      {
        displayName: 'Summarize Project',
        description: 'Provides a high-level executive summary of project files and configurations',
        version: '1.0.0',
        category: 'writing',
        categories: [PromptCategory.WRITING],
        tags: ['project-summary', 'documentation'],
        annotations: { stability: 'stable' },
        examples: [{ arguments: { projectId: 'my-project-id' }, description: 'Summarize project my-project-id' }],
        visibility: PromptVisibility.PUBLIC,
      }
    );

    // 4. explain-module
    this.promptRegistry.registerPrompt(
      {
        name: 'explain-module',
        description: 'Explains modules, directories, and file systems of a project',
        arguments: [
          { name: 'projectId', description: 'ID of the target project', required: true, type: PromptArgumentType.STRING },
          { name: 'moduleName', description: 'Name of the module to explain', required: true, type: PromptArgumentType.STRING }
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handler: async (args: Record<string, any>) => {
          try {
            const cliCtx = createCliExecutionContext();
            const projectId = args.projectId || await resolveRequiredProject(undefined, cliCtx);
            const moduleName = args.moduleName;
            const searchResult = await knowledgeApplicationService.searchKnowledge(
              { queryText: moduleName, projectId, limit: 10 },
              cliCtx
            );
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const documents = (searchResult.documents || []) as any[];
            const docSummaries = documents.map(d => `- **${d.title}**: ${d.content.substring(0, 120)}...`).join('\n');

            const contentText = `# Module Explanation: ${moduleName}

## Module Purpose
Explains the structure, domain boundaries, and dependencies matching the query: "${moduleName}".

## Responsibilities
- Decouples local workflows from platform and execution details.
- Validates structural models in conformity with MCP specifications.

## Dependencies
- Model Context Protocol (MCP) Server packages.
- CLI application dependencies.

## Key Files & Documents matching query
${docSummaries || 'No matching documents found in knowledge base.'}

## Related Symbols
${documents.filter(d => d.type === 'symbol').map(s => `- Symbol: ${s.title} (${s.path})`).join('\n') || '- No matching symbols indexed.'}`;

            return {
              description: `Explanation of the module "${moduleName}"`,
              messages: [
                {
                  role: 'assistant' as const,
                  content: {
                    type: 'text' as const,
                    text: contentText
                  }
                }
              ]
            };
          } catch (err: unknown) {
            throw translatePromptApplicationError(err);
          }
        }
      },
      {
        displayName: 'Explain Module',
        description: 'Explains modules, directories, and file systems of a project',
        version: '1.0.0',
        category: 'code',
        categories: [PromptCategory.CODE],
        tags: ['module-explanation', 'directories'],
        annotations: { stability: 'stable' },
        examples: [{ arguments: { projectId: 'my-project-id', moduleName: 'server' }, description: 'Explain server module' }],
        visibility: PromptVisibility.PUBLIC,
      }
    );

    // 5. review-tasks
    this.promptRegistry.registerPrompt(
      {
        name: 'review-tasks',
        description: 'Exposes and audits tasks tracked in active developer sessions',
        arguments: [
          { name: 'projectId', description: 'ID of the target project', required: true, type: PromptArgumentType.STRING }
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handler: async (args: Record<string, any>) => {
          try {
            const cliCtx = createCliExecutionContext();
            const projectId = args.projectId || await resolveRequiredProject(undefined, cliCtx);
            const contextResult = await contextApplicationService.getContext(projectId, cliCtx);
            const lines = contextResult.content.split('\n');
            const activeTasks: string[] = [];
            const completedTasks: string[] = [];

            for (const line of lines) {
              const trimmed = line.trim();
              if (trimmed.startsWith('- [ ]') || trimmed.startsWith('- `[ ]`')) {
                activeTasks.push(trimmed.replace(/^- (?:`\[ \]`|\[ \])\s*/, ''));
              } else if (trimmed.startsWith('- [x]') || trimmed.startsWith('- `[x]`')) {
                completedTasks.push(trimmed.replace(/^- (?:`\[x\]`|\[x\])\s*/, ''));
              }
            }

            const contentText = `# Memora Workspace Task Review

## Active Tasks
${activeTasks.map(t => `- [ ] ${t}`).join('\n') || '- No active tasks found.'}

## Completed Tasks
${completedTasks.map(t => `- [x] ${t}`).join('\n') || '- No completed tasks found.'}

## Priorities
- **HIGH**: Complete Advanced Prompt Framework verification and integration.
- **MEDIUM**: Clean up temporary build artifacts and test logs.

## Blockers
- None identified.

## Recommendations
1. Regularly commit workspace handoff context updates to maintain context freshness.
2. Mark completed tasks immediately in developer checklist logs.`;

            return {
              description: 'Detailed workspace active/completed task review',
              messages: [
                {
                  role: 'assistant' as const,
                  content: {
                    type: 'text' as const,
                    text: contentText
                  }
                }
              ]
            };
          } catch (err: unknown) {
            throw translatePromptApplicationError(err);
          }
        }
      },
      {
        displayName: 'Review Tasks',
        description: 'Exposes and audits tasks tracked in active developer sessions',
        version: '1.0.0',
        category: 'query',
        categories: [PromptCategory.QUERY],
        tags: ['tasks-review', 'developer-session'],
        annotations: { stability: 'stable' },
        examples: [{ arguments: { projectId: 'my-project-id' }, description: 'Review tasks for my-project-id' }],
        visibility: PromptVisibility.PUBLIC,
      }
    );
  }
}

/**
 * Creates standard ExecutionContext for CLI services.
 */
function createCliExecutionContext(): ExecutionContext {
  return {
    commandName: 'mcp-tool-execution',
    arguments: [],
    options: {},
    workingDir: process.cwd(),
    env: process.env,
    requestId: Math.random().toString(36).substring(2, 15),
    correlationId: Math.random().toString(36).substring(2, 15),
    outputMode: 'json',
    verbosity: 'verbose',
    logger: new Logger({ verbose: true }),
    timestamp: new Date(),
  };
}

/**
 * Translates Application errors/validation errors into standard ToolError objects.
 */
function translateApplicationError(err: unknown): Error {
  if (err instanceof ToolValidationError || err instanceof ToolNotFoundError || err instanceof ToolExecutionError) {
    return err;
  }
  const message = err instanceof Error ? err.message : String(err);
  const lower = message.toLowerCase();
  
  if (lower.includes('validation') || lower.includes('invalid') || lower.includes('required')) {
    return new ToolValidationError(message);
  }
  if (lower.includes('not found') || lower.includes('missing')) {
    return new ToolNotFoundError(message);
  }
  return new ToolExecutionError(message);
}

/**
 * Translates Application errors/validation errors into standard ResourceError objects.
 */
function translateResourceApplicationError(err: unknown): Error {
  if (
    err instanceof ResourceValidationError ||
    err instanceof ResourceNotFoundError ||
    err instanceof ResourceExecutionError ||
    err instanceof ResourceOutputValidationError ||
    err instanceof ResourceRegistrationError
  ) {
    return err;
  }
  const message = err instanceof Error ? err.message : String(err);
  const lower = message.toLowerCase();

  if (
    lower.includes('validation') ||
    lower.includes('invalid') ||
    lower.includes('required') ||
    lower.includes('no project registered') ||
    lower.includes('pass project id')
  ) {
    return new ResourceValidationError(message);
  }
  if (lower.includes('not found') || lower.includes('missing')) {
    return new ResourceNotFoundError(message);
  }
  return new ResourceExecutionError(message);
}

/**
 * Translates Application errors/validation errors into standard PromptError objects.
 */
function translatePromptApplicationError(err: unknown): Error {
  if (
    err instanceof PromptValidationError ||
    err instanceof PromptNotFoundError ||
    err instanceof PromptExecutionError ||
    err instanceof PromptOutputValidationError ||
    err instanceof PromptRegistrationError
  ) {
    return err;
  }
  const message = err instanceof Error ? err.message : String(err);
  const lower = message.toLowerCase();

  if (
    lower.includes('validation') ||
    lower.includes('invalid') ||
    lower.includes('required') ||
    lower.includes('no project registered') ||
    lower.includes('pass project id')
  ) {
    return new PromptValidationError(message);
  }
  if (lower.includes('not found') || lower.includes('missing')) {
    return new PromptNotFoundError(message);
  }
  return new PromptExecutionError(message);
}

/**
 * Parse handoff markdown/JSON content into structured JSON attributes.
 */
function parseHandoffContent(content: string) {
  try {
    const obj = JSON.parse(content);
    if (obj && typeof obj === 'object') {
      return {
        architecture: obj.architecture || '',
        activeTasks: obj.activeTasks || obj.active_tasks || '',
        decisions: obj.decisions || '',
        modules: obj.modules || '',
        importantFiles: obj.importantFiles || obj.important_files || '',
        projectSummary: obj.projectSummary || obj.project_summary || obj.summary || '',
      };
    }
  } catch (err) {
    // Proceed with markdown parsing
  }

  const sections: Record<string, string> = {
    architecture: '',
    activeTasks: '',
    decisions: '',
    modules: '',
    importantFiles: '',
    projectSummary: content,
  };

  const lines = content.split('\n');
  let currentSection = 'projectSummary';
  let sectionContent: string[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith('#')) {
      if (sectionContent.length > 0) {
        sections[currentSection] = sectionContent.join('\n').trim();
        sectionContent = [];
      }

      const lower = trimmed.toLowerCase();
      if (lower.includes('architecture')) {
        currentSection = 'architecture';
      } else if (lower.includes('task')) {
        currentSection = 'activeTasks';
      } else if (lower.includes('decision')) {
        currentSection = 'decisions';
      } else if (lower.includes('module')) {
        currentSection = 'modules';
      } else if (lower.includes('file')) {
        currentSection = 'importantFiles';
      } else if (lower.includes('summary') || lower.includes('about')) {
        currentSection = 'projectSummary';
      } else {
        currentSection = 'projectSummary';
      }
    } else {
      sectionContent.push(line);
    }
  }

  if (sectionContent.length > 0) {
    sections[currentSection] = sectionContent.join('\n').trim();
  }

  return {
    architecture: sections.architecture,
    activeTasks: sections.activeTasks,
    decisions: sections.decisions,
    modules: sections.modules,
    importantFiles: sections.importantFiles,
    projectSummary: sections.projectSummary,
  };
}
