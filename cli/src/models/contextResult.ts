export interface GeneratedContextResult {
  readonly projectId: string;
  readonly content: string;
  readonly updatedAt: string;
}

export interface ContextDetailsResult {
  readonly projectId: string;
  readonly content: string;
  readonly updatedAt: string;
}

export interface ExportContextResult {
  readonly projectId: string;
  readonly content: string;
  readonly format: string;
}

export interface RefreshContextResult {
  readonly projectId: string;
  readonly content: string;
  readonly updatedAt: string;
}

export interface DeleteContextResult {
  readonly type?: 'DeleteContextResult';
  readonly projectId: string;
  readonly success: boolean;
}
