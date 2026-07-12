# Context Engine Backend

Backend foundation for Context Engine, a local-first AI Context Platform.

## Technology

- Java 21
- Spring Boot 3.5.16
- Maven
- Jar packaging

## Prerequisites

- JDK 21 (required and enforced by the Maven build)
- Maven 3.9.6 or later. The included Maven Wrapper provides Maven 3.9.16, so a separate Maven installation is not required.

## Verify your environment

Run these commands from `backend/` before developing:

```bash
java -version
javac -version
./mvnw -version
```

Expected output includes a Java runtime beginning with `21`, `javac 21`, and Maven 3.9.6 or later with `Java version: 21`. The Maven build fails fast if Maven or the active Java runtime does not meet these requirements.

Maven automatically discovers or selects a Java 21 toolchain for compilation and tests. Developers do not need to edit `pom.xml` to use another local JDK installation.

## IDE configuration

- **IntelliJ IDEA:** Set the Project SDK and Maven runner JDK to Java 21, then reload the Maven project.
- **VS Code:** Install a Java extension pack and select Java 21 as the project runtime/JDK. Do not commit IDE-specific configuration files.

## CI readiness

Future CI pipelines only need to provision Java 21 and invoke `./mvnw clean package`; the same Maven Enforcer and toolchain rules apply in CI.

## Run locally

```bash
./mvnw spring-boot:run
```

The `local` profile is the default. To select another environment explicitly, use:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration and profiles

`src/main/resources/application.yml` contains shared, production-safe defaults for the server, graceful shutdown, console logging, actuator health, Jakarta Validation message resolution, and future Context Engine module settings. The module groups (`project`, `scanner`, `parser`, `knowledge`, `search`, `graph`, `events`, `mcp`, `ai`, `storage`, `cache`, `security`, and `workspace`) are placeholders only; they do not enable functionality.

Environment-specific files remain deliberately small:

- `application-local.yml` enables DEBUG logging for `com.contextengine`.
- `application-dev.yml` shows health details for development diagnostics.
- `application-test.yml` suppresses the banner and reduces log verbosity.
- `application-prod.yml` inherits the shared secure defaults without duplication.

Jakarta Validation is supplied by `spring-boot-starter-validation`. The classpath `ValidationMessages.properties` bundle is ready for standard constraint-message interpolation when DTOs and configuration properties are introduced.

## Build

```bash
./mvnw clean package
```

The project intentionally contains only the application bootstrap and framework configuration. No domain, API, persistence, event, scanning, parsing, or MCP implementation is included at this stage.
