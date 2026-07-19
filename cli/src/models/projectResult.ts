export interface ProjectSummary {
  readonly id: string;
  readonly name: string;
  readonly rootPath: string;
}

export interface ProjectDetails {
  readonly id: string;
  readonly name: string;
  readonly rootPath: string;
  readonly registeredAt?: string;
}

export interface ProjectRegistrationResult {
  readonly success: boolean;
  readonly project: ProjectSummary;
}

export interface ProjectRefreshResult {
  readonly success: boolean;
  readonly projectId: string;
  readonly filesScanned: number;
  readonly snapshotGenerated: boolean;
}

export interface ProjectRemovalResult {
  readonly type?: 'ProjectRemovalResult';
  readonly success: boolean;
  readonly projectId: string;
  readonly rootPath?: string;
}

export interface ProjectListResult {
  readonly projects: ProjectSummary[];
}
