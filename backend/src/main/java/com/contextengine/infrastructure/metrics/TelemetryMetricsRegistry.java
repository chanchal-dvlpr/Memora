package com.contextengine.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registry coordinating system-wide telemetry counters and Gauges.
 * Exposes metrics to both Micrometer and memory stats exporters.
 */
@Component
public class TelemetryMetricsRegistry {

    private final AtomicLong restRequestsCount = new AtomicLong(0);
    private final AtomicLong restRequestsErrorCount = new AtomicLong(0);
    private final AtomicLong mcpRequestsCount = new AtomicLong(0);
    private final AtomicLong mcpRequestsErrorCount = new AtomicLong(0);

    private final Counter restRequestsCounter;
    private final Counter restErrorsCounter;
    private final Counter mcpRequestsCounter;
    private final Counter mcpErrorsCounter;

    /**
     * Constructs the metrics registry and binds counters to the Micrometer MeterRegistry.
     *
     * @param meterRegistry Micrometer registry bean
     */
    public TelemetryMetricsRegistry(MeterRegistry meterRegistry) {
        this.restRequestsCounter = Counter.builder("contextengine.rest.requests")
            .description("Total HTTP REST requests processed")
            .register(meterRegistry);
        this.restErrorsCounter = Counter.builder("contextengine.rest.errors")
            .description("Total HTTP REST request failures")
            .register(meterRegistry);
        this.mcpRequestsCounter = Counter.builder("contextengine.mcp.requests")
            .description("Total Model Context Protocol requests processed")
            .register(meterRegistry);
        this.mcpErrorsCounter = Counter.builder("contextengine.mcp.errors")
            .description("Total Model Context Protocol request failures")
            .register(meterRegistry);
    }

    public void incrementRestRequest() {
        restRequestsCount.incrementAndGet();
        restRequestsCounter.increment();
    }

    public void incrementRestError() {
        restRequestsErrorCount.incrementAndGet();
        restErrorsCounter.increment();
    }

    public void incrementMcpRequest() {
        mcpRequestsCount.incrementAndGet();
        mcpRequestsCounter.increment();
    }

    public void incrementMcpError() {
        mcpRequestsErrorCount.incrementAndGet();
        mcpErrorsCounter.increment();
    }

    public long getRestRequestsCount() {
        return restRequestsCount.get();
    }

    public long getRestRequestsErrorCount() {
        return restRequestsErrorCount.get();
    }

    public long getMcpRequestsCount() {
        return mcpRequestsCount.get();
    }

    public long getMcpRequestsErrorCount() {
        return mcpRequestsErrorCount.get();
    }
}
