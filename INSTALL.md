# Memora Installation Guide (v1.0.0)

This document provides complete, cross-platform installation and setup instructions for macOS, Linux, and Windows environments.

---

## System Requirements

- **Java Development Kit (JDK)**: JDK 21 or later.
- **Node.js**: Node 18 or later (LTS recommended).
- **Package Manager**: npm (packaged with Node.js).
- **Git**: For version control operations and repository metadata access.

---

## Environment Verification

Before installing, check your installed dependencies:

```bash
java -version   # Should report 21+
node -v         # Should report 18+
npm -v          # Verify npm is reachable
```

---

## 1. Backend Setup

The Memora backend is a Spring Boot application running on Java 21.

1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```
2. Build the application and packages:
   ```bash
   ./mvnw clean package
   ```
3. Run the Spring Boot server:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Note: On Windows, use `mvnw.cmd` instead of `./mvnw`.*
   The server will start and listen on port `8080` (e.g. `http://localhost:8080`).

---

## 2. CLI Setup

The Memora CLI is a TypeScript application that communicates with the backend REST endpoints.

1. Navigate to the `cli/` directory:
   ```bash
   cd cli
   ```
2. Install node dependencies:
   ```bash
   npm install
   ```
3. Build the CLI package:
   ```bash
   npm run build
   ```
4. Create a global symlink so the `memora` executable is available globally:
   ```bash
   npm link
   ```

---

## 3. Platform Specific Guidelines

### macOS
- Install Java using Homebrew:
  ```bash
  brew install openjdk@21
  ```
- Install Node.js:
  ```bash
  brew install node@20
  ```
- If global installation fails due to permission errors, run `sudo npm link` or configure npm prefix directory.

### Linux
- Install JDK 21 (e.g., Ubuntu/Debian):
  ```bash
  sudo apt-get update
  sudo apt-get install openjdk-21-jdk
  ```
- Verify Node.js is installed from NodeSource or your package manager.
- Run `sudo npm link` to create the global executable.

### Windows
- Install JDK 21 from Oracle or Eclipse Adoptium.
- Install Node.js from the official website installer.
- Use Command Prompt or PowerShell (run as Administrator) to execute:
  ```cmd
  cd cli
  npm install
  npm run build
  npm link
  ```

---

## 4. Troubleshooting

### "Backend: OFFLINE" reported by version command
- Check if the Spring Boot server is running on `http://localhost:8080`.
- Verify the server logs for port conflicts or start failure.

### Command permission issues (macOS/Linux)
- If running `memora` prints permission errors, ensure the CLI is compiled correctly:
  ```bash
  cd cli
  chmod +x dist/index.js
  ```

### Overlapping project root registered (PATH_ALREADY_REGISTERED)
- If you cannot register a project because a subdirectory is already registered, unregister it first:
  ```bash
  memora unregister --path <subdirectory-path>
  ```
- Or delete the registered project using the interactive project selection.
