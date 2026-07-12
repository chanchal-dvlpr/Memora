# Context Engine Backend

Backend foundation for Context Engine, a local-first AI Context Platform.

## Technology

- Java 21
- Spring Boot 3.5.16
- Maven
- Jar packaging

## Prerequisites

- JDK 21

## Run locally

```bash
./mvnw spring-boot:run
```

## Build

```bash
./mvnw clean package
```

The project intentionally contains only the application bootstrap and framework configuration. No domain, API, persistence, event, scanning, parsing, or MCP implementation is included at this stage.
