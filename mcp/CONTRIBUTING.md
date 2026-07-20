# Contributing to Memora MCP Server

Thank you for your interest in contributing to the Memora MCP Server!

## Development Setup

1. Clone the repository and navigate to the `mcp` folder:
   ```bash
   git clone https://github.com/chanchal-dvlpr/memora.git
   cd memora/mcp
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Run type checks and tests:
   ```bash
   npm run typecheck
   npm run lint
   npm test
   ```

4. Build the project:
   ```bash
   npm run build
   ```

## Pull Request Guidelines

- Ensure `npm run typecheck`, `npm run lint`, and `npm test` all pass cleanly before submitting a PR.
- Maintain high test coverage and include integration tests for new tool, resource, or prompt behaviors.
- Follow established TypeScript and Clean Architecture conventions.
