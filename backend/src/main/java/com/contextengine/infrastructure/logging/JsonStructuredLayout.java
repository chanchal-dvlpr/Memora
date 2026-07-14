package com.contextengine.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Logback Layout formatting log events as structured JSON-LD / newline-delimited JSON objects.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Logging Subsystem (LG-SUB)
 * </p>
 */
public class JsonStructuredLayout extends LayoutBase<ILoggingEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String doLayout(ILoggingEvent event) {
        if (event == null) {
            return "";
        }

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        logMap.put("level", event.getLevel().toString());

        // Extract correlation, request, trace, and span IDs from MDC
        Map<String, String> mdc = event.getMDCPropertyMap();
        String correlationId = mdc != null ? mdc.get("correlationId") : null;
        String requestId = mdc != null ? mdc.get("requestId") : null;
        String traceId = mdc != null ? mdc.get("traceId") : null;
        String spanId = mdc != null ? mdc.get("spanId") : null;

        logMap.put("correlationId", correlationId != null ? correlationId : "");
        logMap.put("requestId", requestId != null ? requestId : "");
        logMap.put("traceId", traceId != null ? traceId : "");
        logMap.put("spanId", spanId != null ? spanId : "");
        logMap.put("component", event.getLoggerName() != null ? event.getLoggerName() : "");
        logMap.put("thread", event.getThreadName() != null ? event.getThreadName() : "");
        logMap.put("message", event.getFormattedMessage() != null ? event.getFormattedMessage() : "");

        // Include exception details if present
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            logMap.put("exceptionClass", throwableProxy.getClassName());
            logMap.put("exceptionMessage", throwableProxy.getMessage());
        }

        try {
            return objectMapper.writeValueAsString(logMap) + "\n";
        } catch (Exception e) {
            return "{\"level\":\"ERROR\",\"message\":\"Failed to serialize log event to JSON\",\"exceptionMessage\":\"" + e.getMessage() + "\"}\n";
        }
    }
}
