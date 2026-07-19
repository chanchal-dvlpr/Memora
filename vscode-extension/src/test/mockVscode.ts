import * as Module from 'module';

export class MockDisposable {
    dispose() {}
}

export class MockEventEmitter<T> {
    private listeners: ((e: T) => any)[] = [];
    event = (listener: (e: T) => any) => {
        this.listeners.push(listener);
        return new MockDisposable();
    };
    fire(data: T) {
        this.listeners.forEach(l => l(data));
    }
}

export class MockPosition {
    constructor(public readonly line: number, public readonly character: number) {}
}

export class MockRange {
    constructor(
        public readonly start: MockPosition,
        public readonly end: MockPosition
    ) {}
}

export class MockCodeLens {
    constructor(public readonly range: MockRange, public readonly command?: any) {}
}

export class MockHover {
    constructor(public readonly contents: any, public readonly range?: MockRange) {}
}

export class MockDiagnostic {
    public code?: string;
    constructor(
        public readonly range: MockRange,
        public readonly message: string,
        public readonly severity: number
    ) {}
}

export class MockUri {
    static file(path: string) {
        return new MockUri('file', '', path);
    }
    constructor(
        public readonly scheme: string,
        public readonly authority: string,
        public readonly path: string
    ) {}
    get fsPath() { return this.path; }
}

export const mockOnDidChangeConfiguration = new MockEventEmitter<any>();

export const mockVscode = {
    Disposable: MockDisposable,
    EventEmitter: MockEventEmitter,
    Position: MockPosition,
    Range: MockRange,
    CodeLens: MockCodeLens,
    Hover: MockHover,
    Diagnostic: MockDiagnostic,
    Uri: MockUri,
    ViewColumn: {
        Active: -1,
        Beside: -2,
        One: 1,
        Two: 2,
        Three: 3
    },
    TreeItemCollapsibleState: {
        None: 0,
        Collapsed: 1,
        Expanded: 2
    },
    TreeItem: class {
        public tooltip?: string;
        public contextValue?: string;
        public description?: string;
        constructor(public readonly label: string, public readonly collapsibleState: any) {}
    },
    DiagnosticSeverity: {
        Error: 0,
        Warning: 1,
        Information: 2,
        Hint: 3
    },
    MarkdownString: class {
        public value: string = '';
        appendMarkdown(val: string) { this.value += val; }
    },
    window: {
        activeTextEditor: undefined as any,
        registerTreeDataProvider: () => new MockDisposable(),
        createWebviewPanel: () => ({
            webview: {
                cspSource: 'vscode-resource',
                html: ''
            },
            onDidDispose: (cb: any) => cb(),
            dispose: () => {}
        }),
        onDidChangeActiveTextEditor: () => new MockDisposable(),
        onDidChangeWindowState: () => new MockDisposable(),
        showWarningMessage: () => Promise.resolve(),
        showInformationMessage: () => Promise.resolve(),
        showErrorMessage: () => Promise.resolve(),
        showInputBox: () => Promise.resolve('test')
    },
    workspace: {
        workspaceFolders: undefined as any,
        getConfiguration: () => ({
            get: (key: string, defVal?: any) => {
                if (key === 'connection.port') { return 9876; }
                if (key === 'connection.token') { return 'prod-token'; }
                if (key === 'ui.renderCodeLens') { return true; }
                return defVal;
            }
        }),
        onDidChangeConfiguration: mockOnDidChangeConfiguration.event,
        onDidChangeTextDocument: () => new MockDisposable()
    },
    languages: {
        createDiagnosticCollection: () => ({
            set: () => {},
            clear: () => {},
            dispose: () => {}
        }),
        registerCodeLensProvider: () => new MockDisposable(),
        registerHoverProvider: () => new MockDisposable()
    },
    commands: {
        registerCommand: () => new MockDisposable(),
        getCommands: () => Promise.resolve([])
    },
    env: {
        clipboard: {
            writeText: () => Promise.resolve()
        }
    }
};

import * as http from 'http';

// Pluggable request override structure to circumvent read-only properties
export const mockHttp = {
    ...http,
    request: (options: http.RequestOptions, callback: (res: any) => void) => {
        return mockHttp.requestOverride(options, callback);
    },
    requestOverride: (options: http.RequestOptions, callback: (res: any) => void) => {
        return http.request(options, callback);
    }
};

(global as any).vscode = mockVscode;

const originalRequire = (Module as any).prototype.require;
(Module as any).prototype.require = function (id: string) {
    if (id === 'vscode') {
        return mockVscode;
    }
    if (id === 'http') {
        return mockHttp;
    }
    return originalRequire.apply(this, arguments);
};
