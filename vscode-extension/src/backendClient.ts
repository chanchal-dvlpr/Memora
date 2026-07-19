import * as http from 'http';
import * as vscode from 'vscode';

export class BackendError extends Error {
    public statusCode?: number;
    public body?: any;

    constructor(message: string, statusCode?: number, body?: any) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
        Object.setPrototypeOf(this, BackendError.prototype);
    }
}

export interface ProjectResponse {
    id: string;
    name: string;
    rootPath: string;
    excludedPaths: string[];
}

export interface ScanStatusResponse {
    filesProcessed: number;
    symbolsProcessed: number;
    status: string;
}

export interface ContextResponse {
    contextId: string;
    tokenUsage: number;
    content: string;
}

/**
 * Client wrapper handling communication with local loopback REST endpoints.
 */
export class BackendClient {
    private port: number;
    private token: string;
    private cache: Map<string, any> = new Map();

    constructor() {
        const config = vscode.workspace.getConfiguration('contextEngine');
        this.port = config.get<number>('connection.port') || 9876;
        this.token = config.get<string>('connection.token') || 'prod-token';
    }

    public updateConfig() {
        const config = vscode.workspace.getConfiguration('contextEngine');
        this.port = config.get<number>('connection.port') || 9876;
        this.token = config.get<string>('connection.token') || 'prod-token';
    }

    private request<T>(method: string, path: string, body?: any, retries: number = 3, delay: number = 500): Promise<T> {
        return new Promise((resolve, reject) => {
            const data = body ? JSON.stringify(body) : '';
            const options: http.RequestOptions = {
                hostname: '127.0.0.1',
                port: this.port,
                path: path,
                method: method,
                headers: {
                    'X-Session-Token': this.token,
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(data)
                },
                timeout: 5000
            };

            const req = http.request(options, (res) => {
                let responseBody = '';
                res.on('data', (chunk) => responseBody += chunk);
                res.on('end', () => {
                    if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
                        try {
                            const parsed = JSON.parse(responseBody);
                            this.cache.set(path, parsed);
                            resolve(parsed);
                        } catch (e) {
                            resolve(responseBody as any);
                        }
                    } else {
                        let parsedBody: any;
                        try {
                            parsedBody = JSON.parse(responseBody);
                        } catch (e) {
                            // ignore non-JSON
                        }
                        reject(new BackendError(`HTTP error ${res.statusCode}: ${responseBody}`, res.statusCode, parsedBody));
                    }
                });
            });

            req.on('error', (err) => {
                if (retries > 0) {
                    setTimeout(() => {
                        this.request<T>(method, path, body, retries - 1, delay * 3)
                            .then(resolve)
                            .catch(reject);
                    }, delay);
                } else {
                    if (this.cache.has(path)) {
                        console.warn(`Request failed. Falling back to cached data for path ${path}`);
                        resolve(this.cache.get(path));
                    } else {
                        reject(err);
                    }
                }
            });

            req.on('timeout', () => {
                req.destroy();
            });

            if (body) {
                req.write(data);
            }
            req.end();
        });
    }

    public getProjects(): Promise<ProjectResponse[]> {
        return this.request<ProjectResponse[]>('GET', '/api/v1/projects');
    }

    public registerProject(name: string, rootPath: string): Promise<ProjectResponse> {
        return this.request<ProjectResponse>('POST', '/api/v1/projects', { name, rootPath, excludedPaths: [] });
    }

    public getProject(projectId: string): Promise<ProjectResponse> {
        return this.request<ProjectResponse>('GET', `/api/v1/projects/${projectId}`);
    }

    public deleteProject(projectId: string): Promise<void> {
        return this.request<void>('DELETE', `/api/v1/projects/${projectId}`);
    }

    public triggerScan(projectId: string, incremental: boolean = true): Promise<void> {
        return this.request<void>('POST', `/api/v1/projects/${projectId}/scans`, { incremental });
    }

    public getScanStatus(projectId: string): Promise<ScanStatusResponse> {
        return this.request<ScanStatusResponse>('GET', `/api/v1/projects/${projectId}/scans`);
    }

    public async generateContext(projectId: string, focusSymbol: string, focusFile: string, tokenBudget: number = 2000): Promise<ContextResponse> {
        const raw = await this.request<any>('POST', '/api/v1/context/assembly', {
            projectId,
            focusSymbol,
            focusFile,
            tokenBudget,
            format: 'MARKDOWN'
        });
        return {
            contextId: raw.contextId,
            tokenUsage: raw.totalTokensConsumed !== undefined ? raw.totalTokensConsumed : (raw.tokenUsage !== undefined ? raw.tokenUsage : 0),
            content: raw.assembledTextPayload !== undefined ? raw.assembledTextPayload : (raw.content !== undefined ? raw.content : '')
        };
    }

    public async getLatestSnapshot(projectId: string): Promise<ContextResponse> {
        const raw = await this.request<any>('GET', `/api/v1/projects/${projectId}/snapshots/latest`);
        return {
            contextId: raw.contextId,
            tokenUsage: raw.totalTokensConsumed !== undefined ? raw.totalTokensConsumed : (raw.tokenUsage !== undefined ? raw.tokenUsage : 0),
            content: raw.assembledTextPayload !== undefined ? raw.assembledTextPayload : (raw.content !== undefined ? raw.content : '')
        };
    }
}
