import { ToolRegistry } from '../src/registry/tool';
import { ResourceRegistry } from '../src/registry/resource';
import { PromptRegistry } from '../src/registry/prompt';

describe('Registries', () => {
  describe('ToolRegistry', () => {
    let registry: ToolRegistry;

    beforeEach(() => {
      registry = new ToolRegistry();
    });

    it('should register and look up tools', () => {
      const mockTool = {
        name: 'test-tool',
        description: 'a test tool',
        inputSchema: { type: 'object' as const, properties: {} },
        handler: jest.fn(),
      };

      registry.register(mockTool);
      expect(registry.lookup('test-tool')).toBe(mockTool);

      const allTools = registry.getAll();
      expect(allTools).toHaveLength(1);
      expect(allTools[0]).toBe(mockTool);
    });

    it('should throw error on duplicate tool registration', () => {
      const mockTool = {
        name: 'duplicate',
        inputSchema: {},
        handler: jest.fn(),
      };

      registry.register(mockTool);
      expect(() => registry.register(mockTool)).toThrow(
        'Tool "duplicate" is already registered.'
      );
    });

    it('should return immutable read-only view of tools', () => {
      const mockTool = {
        name: 'test',
        inputSchema: {},
        handler: jest.fn(),
      };

      registry.register(mockTool);
      const all = registry.getAll();
      
      expect(() => {
        (all as unknown as unknown[])[0] = null;
      }).toThrow();
    });
  });

  describe('ResourceRegistry', () => {
    let registry: ResourceRegistry;

    beforeEach(() => {
      registry = new ResourceRegistry();
    });

    it('should register and look up resources', () => {
      const mockResource = {
        uri: 'file://test',
        name: 'test-resource',
        handler: jest.fn(),
      };

      registry.register(mockResource);
      expect(registry.lookup('file://test')).toBe(mockResource);
      expect(registry.getAll()).toHaveLength(1);
    });

    it('should throw error on duplicate resource registration', () => {
      const mockResource = {
        uri: 'dup',
        name: 'dup',
        handler: jest.fn(),
      };

      registry.register(mockResource);
      expect(() => registry.register(mockResource)).toThrow(
        'Resource with URI "dup" is already registered.'
      );
    });
  });

  describe('PromptRegistry', () => {
    let registry: PromptRegistry;

    beforeEach(() => {
      registry = new PromptRegistry();
    });

    it('should register and look up prompts', () => {
      const mockPrompt = {
        name: 'test-prompt',
        handler: jest.fn(),
      };

      registry.register(mockPrompt);
      expect(registry.lookup('test-prompt')).toBe(mockPrompt);
      expect(registry.getAll()).toHaveLength(1);
    });

    it('should throw error on duplicate prompt registration', () => {
      const mockPrompt = {
        name: 'dup',
        handler: jest.fn(),
      };

      registry.register(mockPrompt);
      expect(() => registry.register(mockPrompt)).toThrow(
        'Prompt "dup" is already registered.'
      );
    });
  });
});
