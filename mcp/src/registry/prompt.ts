import { PromptDefinition, PromptMetadata, PromptCategory, PromptVisibility } from '../types/prompt';
import { PromptRegistrationError, PromptValidationError, PromptNotFoundError } from '../errors';

function deepFreeze<T extends object>(obj: T): T {
  Object.freeze(obj);
  Object.getOwnPropertyNames(obj).forEach((prop) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const val = (obj as any)[prop];
    if (val !== null && (typeof val === 'object' || typeof val === 'function') && !Object.isFrozen(val)) {
      deepFreeze(val);
    }
  });
  return obj;
}

export class PromptRegistry {
  private readonly prompts = new Map<string, { prompt: PromptDefinition; metadata: PromptMetadata }>();
  private cachedList: ReadonlyArray<PromptDefinition> | null = null;

  /**
   * Registers a new prompt. Throws PromptRegistrationError if a prompt with the same name already exists.
   */
  public registerPrompt(prompt: PromptDefinition, metadata?: PromptMetadata): void {
    if (!prompt.name) {
      throw new PromptValidationError('Prompt name is required.');
    }
    const finalMeta = metadata || {
      categories: [PromptCategory.SYSTEM],
      visibility: PromptVisibility.PUBLIC,
    };

    if (this.prompts.has(prompt.name)) {
      throw new PromptRegistrationError(`Prompt "${prompt.name}" is already registered.`);
    }

    // Clone and deep freeze metadata to enforce immutability, freeze original prompt
    const metaClone = { ...finalMeta };

    if (!prompt.requiredPermission) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (prompt as any).requiredPermission = `memora://prompts/${prompt.name}`;
    }

    deepFreeze(prompt);
    deepFreeze(metaClone);

    this.prompts.set(prompt.name, { prompt, metadata: metaClone });
    this.cachedList = null;
  }

  /**
   * Unregisters a prompt from the registry. Throws PromptNotFoundError if not found.
   */
  public unregisterPrompt(name: string): void {
    if (!this.prompts.has(name)) {
      throw new PromptNotFoundError(`Prompt with name "${name}" is not registered.`);
    }
    this.prompts.delete(name);
    this.cachedList = null;
  }

  /**
   * Looks up a registered prompt by name.
   */
  public getPrompt(name: string): PromptDefinition | undefined {
    return this.prompts.get(name)?.prompt;
  }

  /**
   * Returns metadata for a registered prompt.
   */
  public getMetadata(name: string): PromptMetadata | undefined {
    return this.prompts.get(name)?.metadata;
  }

  /**
   * Checks if a prompt with the given name is registered.
   */
  public hasPrompt(name: string): boolean {
    return this.prompts.has(name);
  }

  /**
   * Returns an immutable read-only view of all registered prompt definitions, sorted alphabetically by name.
   */
  public listPrompts(): ReadonlyArray<PromptDefinition> {
    if (!this.cachedList) {
      const list = Array.from(this.prompts.values())
        .map((entry) => entry.prompt)
        .sort((a, b) => a.name.localeCompare(b.name));
      this.cachedList = Object.freeze(list);
    }
    return this.cachedList;
  }

  /**
   * Clears the registry.
   */
  public clear(): void {
    this.prompts.clear();
    this.cachedList = null;
  }

  // --- Backward-Compatible Aliases ---

  /**
   * Alias for registerPrompt.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public register(prompt: any): void {
    this.registerPrompt(prompt);
  }

  /**
   * Alias for getPrompt.
   */
  public lookup(name: string): PromptDefinition | undefined {
    return this.getPrompt(name);
  }

  /**
   * Alias for listPrompts.
   */
  public getAll(): ReadonlyArray<PromptDefinition> {
    return this.listPrompts();
  }
}
