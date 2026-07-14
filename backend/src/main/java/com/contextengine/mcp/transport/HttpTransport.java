package com.contextengine.mcp.transport;

import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.protocol.McpRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing local HTTP endpoints serving MCP clients over loopback interface.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Local Sovereign Security)
 * Responsibility: Receive incoming JSON-RPC payload requests, enforce local loopback boundary restrictions, and dispatch to protocol handler.
 * Dependencies: {@link McpProtocolHandler}, {@link HttpServletRequest}
 * Future Usage: Endpoint used by external IDE extensions and orchestrators communicating via TCP loopback.
 */
@RestController
@RequestMapping("/api/v1/mcp")
public class HttpTransport {

    private final McpProtocolHandler protocolHandler;

    /**
     * Constructs a HttpTransport.
     *
     * @param protocolHandler protocol execution gateway handler
     */
    public HttpTransport(McpProtocolHandler protocolHandler) {
        this.protocolHandler = Objects.requireNonNull(protocolHandler, "McpProtocolHandler must not be null");
    }

    /**
     * Entry point executing inbound MCP request envelopes.
     * Enforces Local Sovereign Security check.
     *
     * @param request the deserialized request payload
     * @param servletRequest raw servlet request holding remote addressing metadata
     * @return response mapping wrapper
     */
    @PostMapping
    public ResponseEntity<Object> handleMcpRequest(
        @RequestBody McpRequest request,
        HttpServletRequest servletRequest
    ) {
        String remoteAddr = servletRequest.getRemoteAddr();
        if (!isLoopback(remoteAddr)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of(
                        "code", -32001,
                        "message", "Local Sovereign Security boundary breach: Connections blocked from external IPs: " + remoteAddr
                    ),
                    "id", request != null ? request.getId() : null
                ));
        }

        Object response = protocolHandler.handleRequest(request);
        return ResponseEntity.ok(response);
    }

    private boolean isLoopback(String addr) {
        return addr != null && (
            addr.equals("127.0.0.1") ||
            addr.equals("0:0:0:0:0:0:0:1") ||
            addr.equals("::1") ||
            addr.equals("localhost")
        );
    }
}
