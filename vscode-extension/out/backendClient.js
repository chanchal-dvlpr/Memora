"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.BackendClient = exports.BackendError = void 0;
const http = __importStar(require("http"));
const vscode = __importStar(require("vscode"));
class BackendError extends Error {
    statusCode;
    body;
    constructor(message, statusCode, body) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
        Object.setPrototypeOf(this, BackendError.prototype);
    }
}
exports.BackendError = BackendError;
/**
 * Client wrapper handling communication with local loopback REST endpoints.
 */
class BackendClient {
    port;
    token;
    cache = new Map();
    constructor() {
        const config = vscode.workspace.getConfiguration('contextEngine');
        this.port = config.get('connection.port') || 9876;
        this.token = config.get('connection.token') || 'prod-token';
    }
    updateConfig() {
        const config = vscode.workspace.getConfiguration('contextEngine');
        this.port = config.get('connection.port') || 9876;
        this.token = config.get('connection.token') || 'prod-token';
    }
    request(method, path, body, retries = 3, delay = 500) {
        return new Promise((resolve, reject) => {
            const data = body ? JSON.stringify(body) : '';
            const options = {
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
                        }
                        catch (e) {
                            resolve(responseBody);
                        }
                    }
                    else {
                        let parsedBody;
                        try {
                            parsedBody = JSON.parse(responseBody);
                        }
                        catch (e) {
                            // ignore non-JSON
                        }
                        reject(new BackendError(`HTTP error ${res.statusCode}: ${responseBody}`, res.statusCode, parsedBody));
                    }
                });
            });
            req.on('error', (err) => {
                if (retries > 0) {
                    setTimeout(() => {
                        this.request(method, path, body, retries - 1, delay * 3)
                            .then(resolve)
                            .catch(reject);
                    }, delay);
                }
                else {
                    if (this.cache.has(path)) {
                        console.warn(`Request failed. Falling back to cached data for path ${path}`);
                        resolve(this.cache.get(path));
                    }
                    else {
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
    getProjects() {
        return this.request('GET', '/api/v1/projects');
    }
    registerProject(name, rootPath) {
        return this.request('POST', '/api/v1/projects', { name, rootPath, excludedPaths: [] });
    }
    getProject(projectId) {
        return this.request('GET', `/api/v1/projects/${projectId}`);
    }
    deleteProject(projectId) {
        return this.request('DELETE', `/api/v1/projects/${projectId}`);
    }
    triggerScan(projectId, incremental = true) {
        return this.request('POST', `/api/v1/projects/${projectId}/scans`, { incremental });
    }
    getScanStatus(projectId) {
        return this.request('GET', `/api/v1/projects/${projectId}/scans`);
    }
    async generateContext(projectId, focusSymbol, focusFile, tokenBudget = 2000) {
        const raw = await this.request('POST', '/api/v1/context/assembly', {
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
    async getLatestSnapshot(projectId) {
        const raw = await this.request('GET', `/api/v1/projects/${projectId}/snapshots/latest`);
        return {
            contextId: raw.contextId,
            tokenUsage: raw.totalTokensConsumed !== undefined ? raw.totalTokensConsumed : (raw.tokenUsage !== undefined ? raw.tokenUsage : 0),
            content: raw.assembledTextPayload !== undefined ? raw.assembledTextPayload : (raw.content !== undefined ? raw.content : '')
        };
    }
}
exports.BackendClient = BackendClient;
//# sourceMappingURL=backendClient.js.map