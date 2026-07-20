/* eslint-disable @typescript-eslint/no-explicit-any */
import { PromptRegistry } from '../src/registry/prompt';
import { PromptValidator, PromptDispatcher } from '../src/prompt';
import { PromptCategory, PromptVisibility, PromptArgumentType, PromptCache, PromptCacheEntry } from '../src/types/prompt';
import {
  PromptValidationError,
  PromptOutputValidationError,
  PromptNotFoundError,
  PromptExecutionError,
  PromptRegistrationError,
} from '../src/errors';
import { MemoraMcpServer } from '../src/server';
import { StructuredLogger } from '../src/logging/logger';
import { ListPromptsRequestSchema, GetPromptRequestSchema } from '@modelcontextprotocol/sdk/types.js';
import { httpClientService } from 'memora-cli/src/http/clientService';
import { HttpTransport } from 'memora-cli/src/http/transport';
import { configService } from 'memora-cli/src/config/service';

// Mock MCP SDK modules
const mockHandlers = new Map<unknown, any>();

jest.mock('@modelcontextprotocol/sdk/types.js', () => {
  return {
    CallToolRequestSchema: { method: 'tools/call' },
    ListToolsRequestSchema: { method: 'tools/list' },
    ListResourcesRequestSchema: { method: 'resources/list' },
    ReadResourceRequestSchema: { method: 'resources/read' },
    ListPromptsRequestSchema: { method: 'prompts/list' },
    GetPromptRequestSchema: { method: 'prompts/get' },
  };
});

// Mock Server to capture handlers
jest.mock('@modelcontextprotocol/sdk/server/index.js', () => {
  return {
    Server: jest.fn().mockImplementation(() => {
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        close: jest.fn().mockResolvedValue(undefined),
        onerror: jest.fn(),
        onclose: jest.fn(),
        setRequestHandler: jest.fn().mockImplementation((schema, handler) => {
          mockHandlers.set(schema, handler);
        }),
      };
    }),
  };
});

describe('MCP Prompt Framework Foundation', () => {
  let registry: PromptRegistry;
  let dispatcher: PromptDispatcher;
  const logger = new StructuredLogger('test', 'error');

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'test-req',
    sessionId: 'test-session',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params,
    metadata: new Map<string, unknown>(),
  });

  beforeEach(() => {
    registry = new PromptRegistry();
    dispatcher = new PromptDispatcher(registry);
    mockHandlers.clear();
  });

  describe('PromptRegistry', () => {
    it('should register a prompt and fetch it back', () => {
      const promptDef = {
        name: 'test-prompt',
        description: 'A test prompt template',
        arguments: [{ name: 'param', required: true, type: PromptArgumentType.STRING }],
        handler: async (args: Record<string, string>) => [
          { role: 'user' as const, content: { type: 'text' as const, text: `Hello ${args.param}` } },
        ],
      };

      registry.registerPrompt(promptDef);
      expect(registry.hasPrompt('test-prompt')).toBe(true);
      expect(registry.getPrompt('test-prompt')).toBeDefined();
      expect(registry.getPrompt('test-prompt')?.name).toBe('test-prompt');
    });

    it('should fail when registering duplicate prompt names', () => {
      const p = {
        name: 'duplicate',
        handler: async () => [],
      };
      registry.registerPrompt(p);
      expect(() => registry.registerPrompt(p)).toThrow(PromptRegistrationError);
    });

    it('should unregister a prompt successfully', () => {
      const p = {
        name: 'unreg',
        handler: async () => [],
      };
      registry.registerPrompt(p);
      expect(registry.hasPrompt('unreg')).toBe(true);
      registry.unregisterPrompt('unreg');
      expect(registry.hasPrompt('unreg')).toBe(false);
    });

    it('should return prompts sorted alphabetically', () => {
      registry.registerPrompt({ name: 'c', handler: async () => [] });
      registry.registerPrompt({ name: 'a', handler: async () => [] });
      registry.registerPrompt({ name: 'b', handler: async () => [] });

      const prompts = registry.listPrompts();
      expect(prompts[0].name).toBe('a');
      expect(prompts[1].name).toBe('b');
      expect(prompts[2].name).toBe('c');
    });

    it('should freeze prompt and metadata to enforce immutability', () => {
      const p = {
        name: 'immutable',
        description: 'original',
        handler: async () => [],
      };
      registry.registerPrompt(p, {
        displayName: 'Immutable Prompt',
        categories: [PromptCategory.CODE],
        visibility: PromptVisibility.PUBLIC,
      });

      const registered = registry.getPrompt('immutable')!;
      const metadata = registry.getMetadata('immutable')!;

      expect(Object.isFrozen(registered)).toBe(true);
      expect(Object.isFrozen(metadata)).toBe(true);
    });
  });

  describe('PromptValidator', () => {
    it('should validate correct inputs', () => {
      const argsDef = [
        { name: 'str', required: true, type: PromptArgumentType.STRING },
        { name: 'num', required: false, type: PromptArgumentType.NUMBER },
        { name: 'bool', required: false, type: PromptArgumentType.BOOLEAN },
      ];

      expect(() =>
        PromptValidator.validateInput(argsDef, { str: 'hello', num: '123', bool: 'true' })
      ).not.toThrow();
    });

    it('should throw on missing required arguments', () => {
      const argsDef = [{ name: 'req', required: true }];
      expect(() => PromptValidator.validateInput(argsDef, {})).toThrow(PromptValidationError);
    });

    it('should throw on unknown arguments', () => {
      const argsDef = [{ name: 'allowed' }];
      expect(() => PromptValidator.validateInput(argsDef, { unknown: 'val' })).toThrow(
        PromptValidationError
      );
    });

    it('should throw on invalid types', () => {
      const argsDef = [
        { name: 'num', type: PromptArgumentType.NUMBER },
        { name: 'bool', type: PromptArgumentType.BOOLEAN },
      ];

      expect(() => PromptValidator.validateInput(argsDef, { num: 'not-a-number' })).toThrow(
        PromptValidationError
      );
      expect(() => PromptValidator.validateInput(argsDef, { bool: 'not-a-bool' })).toThrow(
        PromptValidationError
      );
    });

    it('should validate output format structure', () => {
      const validResult = {
        messages: [
          { role: 'system' as const, content: { type: 'resource' as const, resource: { uri: 'res://foo' } } },
          { role: 'user' as const, content: { type: 'text' as const, text: 'hi' } },
          { role: 'assistant' as const, content: { type: 'image' as const, data: 'd', mimeType: 'img/png' } },
        ],
      };

      expect(() => PromptValidator.validateOutput(validResult)).not.toThrow();
    });

    it('should throw on invalid output message role', () => {
      const invalidResult = {
        messages: [
          { role: 'unknown-role' as any, content: { type: 'text' as const, text: 'hi' } },
        ],
      };
      expect(() => PromptValidator.validateOutput(invalidResult)).toThrow(PromptOutputValidationError);
    });

    it('should throw on invalid content text', () => {
      const invalidResult = {
        messages: [
          { role: 'user' as const, content: { type: 'text' as const, text: 123 as any } },
        ],
      };
      expect(() => PromptValidator.validateOutput(invalidResult)).toThrow(PromptOutputValidationError);
    });

    it('should throw on non-serializable circular references', () => {
      const circular: any = {};
      circular.self = circular;
      const invalidResult = {
        messages: [
          { role: 'user' as const, content: { type: 'text' as const, text: 'ok' }, circular },
        ],
      };
      expect(() => PromptValidator.validateOutput(invalidResult)).toThrow(PromptOutputValidationError);
    });
  });

  describe('PromptDispatcher & Middleware Pipeline', () => {
    it('should dispatch call and execute middleware onion stack in order', async () => {
      const executionOrder: string[] = [];

      const p = {
        name: 'onion',
        handler: async () => {
          executionOrder.push('handler');
          return [{ role: 'user' as const, content: { type: 'text' as const, text: 'result' } }];
        },
      };

      registry.registerPrompt(p);

      dispatcher.use(async (_ctx, _prompt, next) => {
        executionOrder.push('m1-start');
        const res = await next();
        executionOrder.push('m1-end');
        return res;
      });

      dispatcher.use(async (_ctx, _prompt, next) => {
        executionOrder.push('m2-start');
        const res = await next();
        executionOrder.push('m2-end');
        return res;
      });

      await dispatcher.dispatchGet('onion', {}, mockContext());

      expect(executionOrder).toEqual([
        'm1-start',
        'm2-start',
        'handler',
        'm2-end',
        'm1-end',
      ]);
    });

    it('should throw PromptNotFoundError for unknown prompts', async () => {
      await expect(
        dispatcher.dispatchGet('unknown', {}, mockContext())
      ).rejects.toThrow(PromptNotFoundError);
    });

    it('should abort pipeline if auth blocked metadata is set', async () => {
      const p = {
        name: 'blocked',
        handler: async () => [],
      };
      registry.registerPrompt(p);

      const ctx = mockContext();
      ctx.metadata.set('auth_blocked', true);

      await expect(dispatcher.dispatchGet('blocked', {}, ctx)).rejects.toThrow(
        /Authorization blocked/
      );
    });
  });

  describe('MemoraMcpServer Integration', () => {
    let server: MemoraMcpServer;

    const setMockTransport = (sendFn: any) => {
      (httpClientService as any).pipeline.transport = {
        send: sendFn,
      } as HttpTransport;
    };

    beforeAll(async () => {
      await configService.load();
    });

    beforeEach(() => {
      const mockTransport = {
        connect: jest.fn(),
        close: jest.fn(),
        send: jest.fn(),
      } as any;

      server = new MemoraMcpServer(
        {
          serverName: 'test-memora-mcp',
          version: '1.0.0',
          logLevel: 'error',
          usePlaceholder: false, // Use production prompts
          host: '127.0.0.1',
          port: 0,
          environment: 'test',
          timeout: 5000,
        },
        mockTransport
      );

      server.initialize();
    });

    it('should expose prompts capability', () => {
      const instance = server.getServerInstance();
      expect(instance).toBeDefined();
    });

    it('should register all 5 production prompts upon initialization', () => {
      const promptRegistry = server.getPromptRegistry();
      const prompts = promptRegistry.listPrompts().map((p) => p.name);

      expect(prompts).toContain('generate-handoff');
      expect(prompts).toContain('review-architecture');
      expect(prompts).toContain('summarize-project');
      expect(prompts).toContain('explain-module');
      expect(prompts).toContain('review-tasks');
    });

    it('should handle ListPromptsRequestSchema JSON-RPC requests with full metadata', async () => {
      const listHandler = mockHandlers.get(ListPromptsRequestSchema);
      expect(listHandler).toBeDefined();

      const result = await listHandler();
      expect(result.prompts).toHaveLength(5);
      
      const handoffPrompt = result.prompts.find((p: any) => p.name === 'generate-handoff');
      expect(handoffPrompt).toBeDefined();
      expect(handoffPrompt.displayName).toBe('Generate Project Handoff');
      expect(handoffPrompt.category).toBe('system');
      expect(handoffPrompt.version).toBe('1.0.0');
      expect(handoffPrompt.visibility).toBe('public');
      expect(handoffPrompt.tags).toContain('handoff');
      expect(handoffPrompt.examples).toHaveLength(1);
    });

    it('should handle GetPromptRequestSchema for generate-handoff', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: 'r' }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '# Architecture\nClean Architecture Overview\n# Tasks\n- [ ] Task 1',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      const result = await getHandler({
        params: {
          name: 'generate-handoff',
          arguments: { projectId: 'proj123' },
        },
      });

      expect(result.messages).toBeDefined();
      expect(result.messages[0].content.text).toContain('Clean Architecture Overview');
      expect(result.messages[0].content.text).toContain('Task 1');
    });

    it('should handle GetPromptRequestSchema for review-architecture', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: 'r' }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '# Architecture\nCore design patterns\n# Modules\n- Server module',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      const result = await getHandler({
        params: {
          name: 'review-architecture',
          arguments: { projectId: 'proj123' },
        },
      });

      expect(result.messages).toBeDefined();
      expect(result.messages[0].content.text).toContain('Core design patterns');
      expect(result.messages[0].content.text).toContain('Server module');
    });

    it('should handle GetPromptRequestSchema for summarize-project', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'memora-p', rootPath: 'r' }] };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      const result = await getHandler({
        params: {
          name: 'summarize-project',
          arguments: { projectId: 'proj123' },
        },
      });

      expect(result.messages).toBeDefined();
      expect(result.messages[0].content.text).toContain('Project Executive Summary: memora-p');
    });

    it('should handle GetPromptRequestSchema for explain-module', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: 'r' }] };
        }
        if (path.includes('/knowledge/query')) {
          return {
            status: 200,
            headers: {},
            data: {
              documents: [
                { id: '1', title: 'module-doc', content: 'This is module document content', type: 'symbol', path: 'src/module' }
              ]
            }
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      const result = await getHandler({
        params: {
          name: 'explain-module',
          arguments: { projectId: 'proj123', moduleName: 'server' },
        },
      });

      expect(result.messages).toBeDefined();
      expect(result.messages[0].content.text).toContain('module-doc');
      expect(result.messages[0].content.text).toContain('This is module document content');
    });

    it('should handle GetPromptRequestSchema for review-tasks', async () => {
      setMockTransport(async (builder: any) => {
        const path = builder.getPath();
        if (path.includes('/projects')) {
          return { status: 200, headers: {}, data: [{ id: 'proj123', name: 'p', rootPath: 'r' }] };
        }
        if (path.includes('/context/')) {
          return {
            status: 200,
            headers: {},
            data: {
              projectId: 'proj123',
              content: '- [ ] Finish integration tests\n- [x] Create plan',
            },
          };
        }
        return { status: 404, headers: {}, data: 'Not found' };
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      const result = await getHandler({
        params: {
          name: 'review-tasks',
          arguments: { projectId: 'proj123' },
        },
      });

      expect(result.messages).toBeDefined();
      expect(result.messages[0].content.text).toContain('Finish integration tests');
      expect(result.messages[0].content.text).toContain('Create plan');
    });

    it('should translate application service backend failures to PromptExecutionError', async () => {
      setMockTransport(async () => {
        throw new Error('Connection refused');
      });

      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      await expect(
        getHandler({
          params: {
            name: 'generate-handoff',
            arguments: { projectId: 'proj123' },
          },
        })
      ).rejects.toThrow(PromptExecutionError);
    });

    it('should reject get requests missing required parameters with PromptValidationError', async () => {
      const getHandler = mockHandlers.get(GetPromptRequestSchema);
      await expect(
        getHandler({
          params: {
            name: 'generate-handoff',
            arguments: {}, // missing projectId
          },
        })
      ).rejects.toThrow(PromptValidationError);
    });
  });

  describe('Advanced Constraints & Features', () => {
    it('should validate advanced input constraints (regex, length, range)', () => {
      const argsDef = [
        { name: 'code', type: PromptArgumentType.STRING, pattern: '^[A-Z]{3}-\\d{3}$' },
        { name: 'level', type: PromptArgumentType.NUMBER, minimum: 1, maximum: 5 },
        { name: 'name', type: PromptArgumentType.STRING, minLength: 2, maxLength: 10 },
      ];

      // Valid
      const validArgs = { code: 'ABC-123', level: '3', name: 'Antigrav' };
      expect(() => PromptValidator.validateInput(argsDef, validArgs)).not.toThrow();
      expect(validArgs.level).toBe(3); // parsed

      // Invalid code (regex)
      expect(() =>
        PromptValidator.validateInput(argsDef, { code: 'abc-123', level: '3', name: 'Antigrav' })
      ).toThrow(PromptValidationError);

      // Invalid level (min)
      expect(() =>
        PromptValidator.validateInput(argsDef, { code: 'ABC-123', level: '0', name: 'Antigrav' })
      ).toThrow(PromptValidationError);

      // Invalid name (length)
      expect(() =>
        PromptValidator.validateInput(argsDef, { code: 'ABC-123', level: '3', name: 'A' })
      ).toThrow(PromptValidationError);
    });

    it('should validate arrays and nested objects', () => {
      const argsDef = [
        { name: 'tags', type: PromptArgumentType.ARRAY, items: { type: PromptArgumentType.STRING } },
        {
          name: 'config',
          type: PromptArgumentType.OBJECT,
          properties: {
            debug: { name: 'debug', type: PromptArgumentType.BOOLEAN, required: true },
          },
        },
      ];

      // Valid JSON strings
      const args = {
        tags: '["ts", "jest"]',
        config: '{"debug": "true"}',
      };
      expect(() => PromptValidator.validateInput(argsDef, args)).not.toThrow();
      expect(args.tags).toEqual(['ts', 'jest']);
      expect((args as any).config.debug).toBe(true);

      // Invalid nested property
      const invalidArgs = {
        tags: '["ts"]',
        config: '{"debug": "invalid"}',
      };
      expect(() => PromptValidator.validateInput(argsDef, invalidArgs)).toThrow(PromptValidationError);
    });

    it('should inject default values', () => {
      const argsDef = [
        { name: 'env', type: PromptArgumentType.STRING, defaultValue: 'prod' },
      ];
      const args: any = {};
      PromptValidator.validateInput(argsDef, args);
      expect(args.env).toBe('prod');
    });

    it('should reject empty messages arrays and system messages out of order', () => {
      // Empty messages
      expect(() => PromptValidator.validateOutput({ messages: [] })).toThrow(PromptOutputValidationError);

      // Out of order: system message after non-system message
      const outOfOrderResult = {
        messages: [
          { role: 'user' as const, content: { type: 'text' as const, text: 'hi' } },
          { role: 'system' as const, content: { type: 'text' as const, text: 'system prompt' } },
        ],
      };
      expect(() => PromptValidator.validateOutput(outOfOrderResult)).toThrow(PromptOutputValidationError);
    });

    it('should compile and support caching interfaces', async () => {
      // Define a dummy cache class verifying caching API compliance
      class DummyCache implements PromptCache {
        private storeMap = new Map<string, PromptCacheEntry>();
        async lookup(key: string): Promise<PromptCacheEntry | undefined> {
          return this.storeMap.get(key);
        }
        async store(key: string, entry: PromptCacheEntry): Promise<void> {
          this.storeMap.set(key, entry);
        }
        async invalidate(key: string): Promise<void> {
          this.storeMap.delete(key);
        }
        async clear(): Promise<void> {
          this.storeMap.clear();
        }
      }

      const cache = new DummyCache();
      const entry: PromptCacheEntry = {
        key: 'test-key',
        result: {
          messages: [{ role: 'user', content: { type: 'text', text: 'cached' } }],
        },
        timestamp: Date.now(),
      };

      await cache.store('test-key', entry);
      const retrieved = await cache.lookup('test-key');
      expect((retrieved?.result.messages[0].content as any).text).toBe('cached');
      await cache.invalidate('test-key');
      expect(await cache.lookup('test-key')).toBeUndefined();
    });
  });
});
