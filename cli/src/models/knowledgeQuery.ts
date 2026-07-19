export interface KnowledgeQuery {
  readonly queryText: string;
  readonly projectId: string;
  readonly limit?: number;
  readonly filters?: Record<string, unknown>;
  readonly paginationPlaceholder?: unknown;
  readonly sortingPlaceholder?: unknown;
  readonly semanticSearchPlaceholder?: unknown;
  readonly hybridSearchPlaceholder?: unknown;
  readonly metadataFiltersPlaceholder?: unknown;
}
