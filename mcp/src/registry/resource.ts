import { ResourceDefinition, ResourceMetadata, ResourceCategory, ResourceVisibility } from '../types/resource';
import { ResourceRegistrationError, ResourceValidationError, ResourceNotFoundError } from '../errors';

export class ResourceRegistry {
  private readonly resources = new Map<string, { resource: ResourceDefinition; metadata: ResourceMetadata }>();
  private cachedList: ReadonlyArray<ResourceDefinition> | null = null;

  private getBaseUri(uri: string): string {
    return uri.split('?')[0].split('#')[0];
  }

  /**
   * Registers a new resource. Throws if a resource with the same URI already exists.
   */
  public registerResource(resource: ResourceDefinition, metadata?: ResourceMetadata): void {
    if (!resource.uri) {
      throw new ResourceValidationError('Resource URI is required.');
    }
    const finalMeta = metadata || {
      categories: [ResourceCategory.SYSTEM],
      visibility: ResourceVisibility.PUBLIC,
    };
    const baseUri = this.getBaseUri(resource.uri);
    if (this.resources.has(baseUri)) {
      throw new ResourceRegistrationError(`Resource with URI "${resource.uri}" is already registered.`);
    }
    if (!resource.requiredPermission) {
      resource.requiredPermission = `memora://resources/${resource.name}`;
    }
    this.resources.set(baseUri, { resource, metadata: finalMeta });
    this.cachedList = null;
  }

  /**
   * Unregisters a resource from the registry. Throws if the resource is not found.
   */
  public unregisterResource(uri: string): void {
    const baseUri = this.getBaseUri(uri);
    if (!this.resources.has(baseUri)) {
      throw new ResourceNotFoundError(`Resource with URI "${uri}" is not registered.`);
    }
    this.resources.delete(baseUri);
    this.cachedList = null;
  }

  /**
   * Looks up a registered resource by URI.
   */
  public getResource(uri: string): ResourceDefinition | undefined {
    const baseUri = this.getBaseUri(uri);
    return this.resources.get(baseUri)?.resource;
  }

  /**
   * Returns metadata for a registered resource.
   */
  public getMetadata(uri: string): ResourceMetadata | undefined {
    const baseUri = this.getBaseUri(uri);
    return this.resources.get(baseUri)?.metadata;
  }

  /**
   * Checks if a resource with the given URI is registered.
   */
  public hasResource(uri: string): boolean {
    const baseUri = this.getBaseUri(uri);
    return this.resources.has(baseUri);
  }

  /**
   * Returns an immutable read-only view of all registered resource definitions, sorted alphabetically by URI.
   */
  public listResources(): ReadonlyArray<ResourceDefinition> {
    if (!this.cachedList) {
      const list = Array.from(this.resources.values())
        .map((entry) => entry.resource)
        .sort((a, b) => a.uri.localeCompare(b.uri));
      this.cachedList = Object.freeze(list);
    }
    return this.cachedList;
  }

  /**
   * Clears the registry.
   */
  public clear(): void {
    this.resources.clear();
    this.cachedList = null;
  }

  // --- Backward-Compatible Aliases ---

  /**
   * Alias for registerResource.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public register(resource: any): void {
    this.registerResource(resource);
  }

  /**
   * Alias for getResource.
   */
  public lookup(uri: string): ResourceDefinition | undefined {
    return this.getResource(uri);
  }

  /**
   * Alias for listResources.
   */
  public getAll(): ReadonlyArray<ResourceDefinition> {
    return this.listResources();
  }
}
