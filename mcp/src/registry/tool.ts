import { ToolDefinition, ToolMetadata, ToolCategory, ToolVisibility } from '../types/tool';

export class ToolRegistry {
  private readonly tools = new Map<string, { tool: ToolDefinition; metadata: ToolMetadata }>();
  private cachedList: ReadonlyArray<ToolDefinition> | null = null;

  /**
   * Registers a new tool. Throws if a tool with the same name already exists or metadata is invalid.
   */
  public registerTool(tool: ToolDefinition, metadata: ToolMetadata): void {
    if (!tool.name) {
      throw new Error('Tool name is required.');
    }
    if (!metadata || !metadata.categories || !metadata.visibility) {
      throw new Error(`Tool metadata is required and must be valid for tool "${tool.name}".`);
    }
    if (this.tools.has(tool.name)) {
      throw new Error(`Tool "${tool.name}" is already registered.`);
    }
    if (!tool.requiredPermission) {
      tool.requiredPermission = `memora://tools/${tool.name}`;
    }
    this.tools.set(tool.name, { tool, metadata });
    this.cachedList = null;
  }

  /**
   * Unregisters a tool from the registry. Throws if the tool is not found.
   */
  public unregisterTool(name: string): void {
    if (!this.tools.has(name)) {
      throw new Error(`Tool "${name}" is not registered.`);
    }
    this.tools.delete(name);
    this.cachedList = null;
  }

  /**
   * Looks up a registered tool by name.
   */
  public getTool(name: string): ToolDefinition | undefined {
    return this.tools.get(name)?.tool;
  }

  /**
   * Returns metadata for a registered tool.
   */
  public getMetadata(name: string): ToolMetadata | undefined {
    return this.tools.get(name)?.metadata;
  }

  /**
   * Checks if a tool with the given name is registered.
   */
  public hasTool(name: string): boolean {
    return this.tools.has(name);
  }

  /**
   * Returns an immutable read-only view of all registered tool definitions, sorted alphabetically.
   */
  public listTools(): ReadonlyArray<ToolDefinition> {
    if (!this.cachedList) {
      const list = Array.from(this.tools.values())
        .map((entry) => entry.tool)
        .sort((a, b) => a.name.localeCompare(b.name));
      this.cachedList = Object.freeze(list);
    }
    return this.cachedList;
  }

  /**
   * Clears the registry.
   */
  public clear(): void {
    this.tools.clear();
    this.cachedList = null;
  }

  // --- Backward-Compatible Aliases ---

  /**
   * Alias for registerTool to satisfy existing tests and legacy references.
   */
  public register(tool: ToolDefinition): void {
    const defaultMeta: ToolMetadata = {
      categories: [ToolCategory.SYSTEM],
      visibility: ToolVisibility.PUBLIC,
    };
    this.registerTool(tool, defaultMeta);
  }

  /**
   * Alias for getTool to satisfy existing tests and legacy references.
   */
  public lookup(name: string): ToolDefinition | undefined {
    return this.getTool(name);
  }

  /**
   * Alias for listTools to satisfy existing tests and legacy references.
   */
  public getAll(): ReadonlyArray<ToolDefinition> {
    return this.listTools();
  }
}
