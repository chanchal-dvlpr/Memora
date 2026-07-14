package com.contextengine.api.filter;

import com.contextengine.application.event.EventContext;
import com.contextengine.infrastructure.metrics.TelemetryMetricsRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

/**
 * Servlet Filter executing request correlation lifecycle intercept.
 * Binds/unbinds tracing tokens to MDC and EventContext ThreadLocal.
 * Appends standard tracing response headers: X-ContextEngine-Version, X-Correlation-ID, X-Response-Time-Ms.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String SPAN_ID_HEADER = "X-Span-ID";
    private static final String VERSION_HEADER = "X-ContextEngine-Version";
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time-Ms";
    
    private final TelemetryMetricsRegistry metricsRegistry;
    private static final String VERSION = "0.0.1-SNAPSHOT";

    public RequestCorrelationFilter(TelemetryMetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            long startTime = System.currentTimeMillis();
            metricsRegistry.incrementRestRequest();

            // Extract or generate Correlation ID
            String correlationIdStr = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationIdStr == null || correlationIdStr.trim().isEmpty()) {
                correlationIdStr = UUID.randomUUID().toString();
            }

            // Extract or generate Request ID
            String requestIdStr = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (requestIdStr == null || requestIdStr.trim().isEmpty()) {
                requestIdStr = UUID.randomUUID().toString();
            }

            // Extract or generate Trace ID (keep consistent with Correlation ID if possible)
            String traceIdStr = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceIdStr == null || traceIdStr.trim().isEmpty()) {
                traceIdStr = correlationIdStr;
            }

            // Extract or generate Span ID
            String spanIdStr = httpRequest.getHeader(SPAN_ID_HEADER);
            if (spanIdStr == null || spanIdStr.trim().isEmpty()) {
                spanIdStr = UUID.randomUUID().toString();
            }

            // Bind to MDC
            MDC.put("correlationId", correlationIdStr);
            MDC.put("requestId", requestIdStr);
            MDC.put("traceId", traceIdStr);
            MDC.put("spanId", spanIdStr);

            // Bind to EventContext ThreadLocal
            UUID correlationUuid;
            UUID traceUuid;
            UUID spanUuid;
            try {
                correlationUuid = UUID.fromString(correlationIdStr);
            } catch (IllegalArgumentException e) {
                correlationUuid = UUID.randomUUID();
            }
            try {
                traceUuid = UUID.fromString(traceIdStr);
            } catch (IllegalArgumentException e) {
                traceUuid = correlationUuid;
            }
            try {
                spanUuid = UUID.fromString(spanIdStr);
            } catch (IllegalArgumentException e) {
                spanUuid = UUID.randomUUID();
            }
            
            EventContext.setCorrelationId(correlationUuid);
            EventContext.setTraceId(traceUuid);
            EventContext.setSpanId(spanUuid);

            // Set response headers that are known immediately
            httpResponse.setHeader(VERSION_HEADER, VERSION);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationIdStr);
            httpResponse.setHeader(TRACE_ID_HEADER, traceIdStr);
            httpResponse.setHeader(SPAN_ID_HEADER, spanIdStr);

            try {
                chain.doFilter(request, response);
            } finally {
                // Calculate response time and set response header
                long duration = System.currentTimeMillis() - startTime;
                httpResponse.setHeader(RESPONSE_TIME_HEADER, String.valueOf(duration));

                // Track error status code metrics
                if (httpResponse.getStatus() >= 400) {
                    metricsRegistry.incrementRestError();
                }

                // Clean up ThreadLocal contexts to prevent memory leaks
                MDC.remove("correlationId");
                MDC.remove("requestId");
                MDC.remove("traceId");
                MDC.remove("spanId");
                EventContext.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
