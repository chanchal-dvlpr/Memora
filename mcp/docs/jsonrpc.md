# JSON-RPC 2.0 Protocol Foundation

The Memora MCP Server communication is built on JSON-RPC 2.0.

## Message Types

### 1. Request
Must contain `jsonrpc`, `id` (string/number), `method` (string), and optional `params`.
```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "ping"
}
```

### 2. Success Response
Must contain `jsonrpc`, `id` matching the request, and `result`.
```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "result": "pong"
}
```

### 3. Error Response
Must contain `jsonrpc`, `id`, and `error` object with `code` (number) and `message` (string).
```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "error": {
    "code": -32601,
    "message": "Method not found"
  }
}
```

### 4. Notification
Must contain `jsonrpc`, `method`, and no `id` field.
```json
{
  "jsonrpc": "2.0",
  "method": "initialized"
}
```
