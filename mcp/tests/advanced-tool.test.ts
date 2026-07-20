import { ToolRegistry } from '../src/registry/tool';
import { ToolDispatcher } from '../src/tool/executor';
import { ToolCategory, ToolVisibility } from '../src/types/tool';
import { StructuredLogger } from '../src/logging/logger';
import { ToolValidator } from '../src/tool/validator';
import {
  ToolValidationError,
  ToolExecutionError,
  ToolOutputValidationError,
} from '../src/errors';

describe('Advanced MCP Tool Framework Execution', () => {
  let registry: ToolRegistry;
  let dispatcher: ToolDispatcher;
  const logger = new StructuredLogger('test', 'error');

  const defaultMeta = {
    displayName: 'Add Tool',
    description: 'Adds two numbers',
    longDescription: 'A longer explanation of adding two numbers',
    version: '1.0.0',
    author: 'Memora Team',
    categories: [ToolCategory.UTILITY],
    tags: ['math', 'calc'],
    examples: [{ input: { a: 1, b: 2 }, output: 'Sum is 3' }],
    annotations: { usage: 'general' },
    deprecationFlag: false,
    visibility: ToolVisibility.PUBLIC,
    experimentalFlag: false,
  };

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'req-abc',
    sessionId: 'session-123',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params,
    metadata: new Map<string, unknown>(),
  });

  beforeEach(() => {
    registry = new ToolRegistry();
    dispatcher = new ToolDispatcher(registry);
  });

  describe('Recursive Validation & Constraints', () => {
    it('should validate string patterns and constraints', () => {
      const schema = {
        type: 'object' as const,
        properties: {
          username: { type: 'string', minLength: 3, maxLength: 8, pattern: '^[a-z]+$' },
        },
      };

      // Valid
      expect(() => ToolValidator.validateInput(schema, { username: 'john' })).not.toThrow();

      // Too short
      expect(() => ToolValidator.validateInput(schema, { username: 'jo' })).toThrow(ToolValidationError);

      // Too long
      expect(() => ToolValidator.validateInput(schema, { username: 'johnnyboy' })).toThrow(ToolValidationError);

      // Pattern mismatch
      expect(() => ToolValidator.validateInput(schema, { username: 'John' })).toThrow(ToolValidationError);
    });

    it('should validate numeric constraints', () => {
      const schema = {
        type: 'object' as const,
        properties: {
          age: { type: 'number', minimum: 18, maximum: 65 },
        },
      };

      // Valid
      expect(() => ToolValidator.validateInput(schema, { age: 25 })).not.toThrow();

      // Under minimum
      expect(() => ToolValidator.validateInput(schema, { age: 17 })).toThrow(ToolValidationError);

      // Over maximum
      expect(() => ToolValidator.validateInput(schema, { age: 66 })).toThrow(ToolValidationError);
    });

    it('should validate array items', () => {
      const schema = {
        type: 'object' as const,
        properties: {
          scores: { type: 'array', items: { type: 'number' } },
        },
      };

      // Valid
      expect(() => ToolValidator.validateInput(schema, { scores: [10, 20, 30] })).not.toThrow();

      // Invalid item type
      expect(() => ToolValidator.validateInput(schema, { scores: [10, '20', 30] })).toThrow(ToolValidationError);
    });

    it('should validate enums', () => {
      const schema = {
        type: 'object' as const,
        properties: {
          role: { type: 'string', enum: ['admin', 'user', 'guest'] },
        },
      };

      // Valid
      expect(() => ToolValidator.validateInput(schema, { role: 'admin' })).not.toThrow();

      // Invalid enum value
      expect(() => ToolValidator.validateInput(schema, { role: 'superuser' })).toThrow(ToolValidationError);
    });

    it('should detect unknown properties', () => {
      const schema = {
        type: 'object' as const,
        properties: {
          allowed: { type: 'string' },
        },
      };

      // Valid
      expect(() => ToolValidator.validateInput(schema, { allowed: 'yes' })).not.toThrow();

      // Unknown property
      expect(() => ToolValidator.validateInput(schema, { allowed: 'yes', forbidden: true })).toThrow(ToolValidationError);
    });
  });

  describe('Output Validation', () => {
    it('should reject malformed tool result content structures', () => {
      // Invalid content structure (missing content)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(() => ToolValidator.validateOutput({} as any)).toThrow(ToolOutputValidationError);

      // Content item is missing type
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(() => ToolValidator.validateOutput({ content: [{ text: 'no type' }] } as any)).toThrow(ToolOutputValidationError);

      // Content item has invalid text type
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(() => ToolValidator.validateOutput({ content: [{ type: 'text', text: 123 }] } as any)).toThrow(ToolOutputValidationError);
    });

    it('should reject non-JSON-serializable content structures', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const badObj: any = { type: 'text', text: 'cycle' };
      badObj.self = badObj; // Circular reference makes it non-serializable

      expect(() => ToolValidator.validateOutput({ content: [badObj] })).toThrow(ToolOutputValidationError);
    });
  });

  describe('Middleware Execution Pipeline', () => {
    it('should execute middleware hooks in deterministic sequence', async () => {
      const executionOrder: string[] = [];

      registry.registerTool(
        {
          name: 'mid-test',
          description: 'tests middlewares',
          inputSchema: { type: 'object', properties: {} },
          handler: async () => {
            executionOrder.push('handler');
            return { content: [{ type: 'text', text: 'ok' }] };
          },
        },
        defaultMeta
      );

      // Custom middle-1
      dispatcher.use(async (_ctx, _tool, next) => {
        executionOrder.push('mid-1-start');
        const res = await next();
        executionOrder.push('mid-1-end');
        return res;
      });

      // Custom middle-2
      dispatcher.use(async (_ctx, _tool, next) => {
        executionOrder.push('mid-2-start');
        const res = await next();
        executionOrder.push('mid-2-end');
        return res;
      });

      const res = await dispatcher.dispatchCall('mid-test', {}, mockContext({}));
      expect(res.content[0].text).toBe('ok');

      // The pipeline prepends audit/auth/logging/timing before custom ones.
      // So custom ones executionOrder should be: mid-1-start -> mid-2-start -> handler -> mid-2-end -> mid-1-end
      const customIndices = executionOrder.filter(x => x.startsWith('mid') || x === 'handler');
      expect(customIndices).toEqual([
        'mid-1-start',
        'mid-2-start',
        'handler',
        'mid-2-end',
        'mid-1-end',
      ]);
    });

    it('should intercept execution if authorization is blocked in context metadata', async () => {
      registry.registerTool(
        {
          name: 'auth-test',
          description: 'requires authentication',
          inputSchema: { type: 'object', properties: {} },
          handler: async () => ({ content: [{ type: 'text', text: 'secret' }] }),
        },
        defaultMeta
      );

      const ctx = mockContext({});
      ctx.metadata.set('auth_blocked', true);

      await expect(dispatcher.dispatchCall('auth-test', {}, ctx)).rejects.toThrow(ToolExecutionError);
    });
  });

  describe('Tool Metadata Integrity', () => {
    it('should return complete immutable metadata in listTools sorted deterministically', () => {
      registry.registerTool(
        {
          name: 'b-tool',
          description: 'b description',
          inputSchema: { type: 'object', properties: {} },
          handler: async () => ({ content: [] }),
        },
        { ...defaultMeta, displayName: 'B Tool' }
      );

      registry.registerTool(
        {
          name: 'a-tool',
          description: 'a description',
          inputSchema: { type: 'object', properties: {} },
          handler: async () => ({ content: [] }),
        },
        { ...defaultMeta, displayName: 'A Tool' }
      );

      const list = registry.listTools();
      expect(list).toHaveLength(2);
      // Sorted alphabetically: a-tool first, b-tool second
      expect(list[0].name).toBe('a-tool');
      expect(list[1].name).toBe('b-tool');

      // Check fields presence
      const meta = registry.getMetadata('a-tool')!;
      expect(meta.displayName).toBe('A Tool');
      expect(meta.version).toBe('1.0.0');
      expect(meta.author).toBe('Memora Team');
      expect(meta.tags).toContain('math');
      expect(meta.categories).toContain(ToolCategory.UTILITY);
    });
  });
});
