import { KnowledgeDocument } from '../api/knowledge';

export interface KnowledgeSearchResult {
  readonly type?: 'KnowledgeSearchResult';
  readonly projectId?: string;
  readonly documents: KnowledgeDocument[];
}

export interface KnowledgeDetailsResult {
  readonly id: string;
  readonly title: string;
  readonly content: string;
}

export interface KnowledgeExplanationResult {
  readonly id: string;
  readonly explanation: string;
}

export interface KnowledgeRefreshResult {
  readonly type?: 'KnowledgeRefreshResult';
  readonly projectId: string;
  readonly documents: KnowledgeDocument[];
}

export interface KnowledgeDeleteResult {
  readonly id: string;
  readonly success: boolean;
}
