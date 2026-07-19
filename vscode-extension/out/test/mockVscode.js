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
exports.mockHttp = exports.mockVscode = exports.mockOnDidChangeConfiguration = exports.MockUri = exports.MockDiagnostic = exports.MockHover = exports.MockCodeLens = exports.MockRange = exports.MockPosition = exports.MockEventEmitter = exports.MockDisposable = void 0;
const Module = __importStar(require("module"));
class MockDisposable {
    dispose() { }
}
exports.MockDisposable = MockDisposable;
class MockEventEmitter {
    listeners = [];
    event = (listener) => {
        this.listeners.push(listener);
        return new MockDisposable();
    };
    fire(data) {
        this.listeners.forEach(l => l(data));
    }
}
exports.MockEventEmitter = MockEventEmitter;
class MockPosition {
    line;
    character;
    constructor(line, character) {
        this.line = line;
        this.character = character;
    }
}
exports.MockPosition = MockPosition;
class MockRange {
    start;
    end;
    constructor(start, end) {
        this.start = start;
        this.end = end;
    }
}
exports.MockRange = MockRange;
class MockCodeLens {
    range;
    command;
    constructor(range, command) {
        this.range = range;
        this.command = command;
    }
}
exports.MockCodeLens = MockCodeLens;
class MockHover {
    contents;
    range;
    constructor(contents, range) {
        this.contents = contents;
        this.range = range;
    }
}
exports.MockHover = MockHover;
class MockDiagnostic {
    range;
    message;
    severity;
    code;
    constructor(range, message, severity) {
        this.range = range;
        this.message = message;
        this.severity = severity;
    }
}
exports.MockDiagnostic = MockDiagnostic;
class MockUri {
    scheme;
    authority;
    path;
    static file(path) {
        return new MockUri('file', '', path);
    }
    constructor(scheme, authority, path) {
        this.scheme = scheme;
        this.authority = authority;
        this.path = path;
    }
    get fsPath() { return this.path; }
}
exports.MockUri = MockUri;
exports.mockOnDidChangeConfiguration = new MockEventEmitter();
exports.mockVscode = {
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
        label;
        collapsibleState;
        tooltip;
        contextValue;
        description;
        constructor(label, collapsibleState) {
            this.label = label;
            this.collapsibleState = collapsibleState;
        }
    },
    DiagnosticSeverity: {
        Error: 0,
        Warning: 1,
        Information: 2,
        Hint: 3
    },
    MarkdownString: class {
        value = '';
        appendMarkdown(val) { this.value += val; }
    },
    window: {
        activeTextEditor: undefined,
        registerTreeDataProvider: () => new MockDisposable(),
        createWebviewPanel: () => ({
            webview: {
                cspSource: 'vscode-resource',
                html: ''
            },
            onDidDispose: (cb) => cb(),
            dispose: () => { }
        }),
        onDidChangeActiveTextEditor: () => new MockDisposable(),
        onDidChangeWindowState: () => new MockDisposable(),
        showWarningMessage: () => Promise.resolve(),
        showInformationMessage: () => Promise.resolve(),
        showErrorMessage: () => Promise.resolve(),
        showInputBox: () => Promise.resolve('test')
    },
    workspace: {
        workspaceFolders: undefined,
        getConfiguration: () => ({
            get: (key, defVal) => {
                if (key === 'connection.port') {
                    return 9876;
                }
                if (key === 'connection.token') {
                    return 'prod-token';
                }
                if (key === 'ui.renderCodeLens') {
                    return true;
                }
                return defVal;
            }
        }),
        onDidChangeConfiguration: exports.mockOnDidChangeConfiguration.event,
        onDidChangeTextDocument: () => new MockDisposable()
    },
    languages: {
        createDiagnosticCollection: () => ({
            set: () => { },
            clear: () => { },
            dispose: () => { }
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
const http = __importStar(require("http"));
// Pluggable request override structure to circumvent read-only properties
exports.mockHttp = {
    ...http,
    request: (options, callback) => {
        return exports.mockHttp.requestOverride(options, callback);
    },
    requestOverride: (options, callback) => {
        return http.request(options, callback);
    }
};
global.vscode = exports.mockVscode;
const originalRequire = Module.prototype.require;
Module.prototype.require = function (id) {
    if (id === 'vscode') {
        return exports.mockVscode;
    }
    if (id === 'http') {
        return exports.mockHttp;
    }
    return originalRequire.apply(this, arguments);
};
//# sourceMappingURL=mockVscode.js.map