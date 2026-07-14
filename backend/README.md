# Context Engine Backend

Context Engine is a local-first AI Context Platform backend. It provides project
registration, scanning, semantic knowledge and context generation through REST and
MCP integration endpoints. The production artifact remains a self-contained Spring
Boot executable JAR.

## Requirements

- Java 21 runtime (required by the distribution launchers)
- JDK 21 and Maven 3.9.6+ only when building from source

The included Maven Wrapper provides Maven 3.9.16. The build enforces Java 21 and
automatically selects a compatible toolchain for compilation and tests.

## Build

From `backend/`:

```bash
./mvnw clean package
```

The build creates both artifacts in `target/`:

- `context-engine-backend-<version>.jar` - executable Spring Boot JAR
- `context-engine-backend-<version>-distribution.zip` - workstation distribution

## Launch the distribution

Extract the distribution archive. Its root directory is `ContextEngine/`.

On macOS or Linux:

```bash
./bin/context-engine
```

On Windows:

```bat
bin\context-engine.bat
```

The launchers validate Java 21, then run the executable JAR directly. Any additional
arguments are forwarded to Spring Boot; for example:

```bash
./bin/context-engine --spring.profiles.active=prod
```

## Distribution layout

```text
ContextEngine/
├── bin/
│   ├── context-engine
│   └── context-engine.bat
├── config/
│   └── application.yml
├── logs/
├── context-engine-backend.jar
├── README.md
├── LICENSE
└── NOTICE
```

`logs/` is provided as a local diagnostics location. The current logging configuration
continues to write formatted logs to the console; it is not redirected by the launchers.

## Configuration

The distribution uses the existing Spring Boot configuration model. Classpath defaults
are packaged in the JAR. The launcher additionally loads `config/` as an optional external
configuration location, so `config/application.yml` can contain workstation-specific
overrides without modifying the JAR.

Set `CONTEXT_ENGINE_CONFIG_DIR` to use a different configuration directory. For example:

```bash
CONTEXT_ENGINE_CONFIG_DIR="$HOME/.context-engine" ./bin/context-engine
```

The supplied external configuration file is intentionally empty and preserves the current
application behavior. Existing profiles remain available: `local` (the default), `dev`,
`test`, and `prod`.

## Health and metrics

The application exposes Spring Boot Actuator endpoints at the configured server port:

- `GET /health` - application health
- `GET /info` - application build and runtime information

The default server port and all actuator exposure settings continue to be controlled by
the existing application configuration.

## Shutdown

Use `Ctrl+C` in the foreground terminal, or send `SIGTERM` to the Java process on
macOS/Linux. The launch scripts use direct process execution so Spring Boot receives the
signal and performs its configured graceful shutdown.

## Development

Run the backend from source with:

```bash
./mvnw spring-boot:run
```

Run the full automated suite with:

```bash
./mvnw test
```
