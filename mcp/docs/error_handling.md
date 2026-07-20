# Protocol Error Handling

The Memora MCP Server maps exceptions and protocol violations to standard JSON-RPC 2.0 error specifications.

## Error Codes

| JSON-RPC Code | Error Name | Error Class | Trigger Scenario |
| :--- | :--- | :--- | :--- |
| `-32700` | Parse Error | `JsonRpcParseError` | Malformed JSON input |
| `-32600` | Invalid Request | `JsonRpcInvalidRequestError` | Protocol state violation / invalid request |
| `-32601` | Method Not Found | `JsonRpcMethodNotFoundError` | Calling unregistered methods |
| `-32602` | Invalid Params | `JsonRpcInvalidParamsError` | Missing clientInfo or version mismatch |
| `-32603` | Internal Error | `JsonRpcInternalError` | Uncaught exceptions during execution |
