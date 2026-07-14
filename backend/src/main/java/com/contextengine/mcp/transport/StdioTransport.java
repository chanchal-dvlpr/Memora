package com.contextengine.mcp.transport;

import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.protocol.McpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Standard input/output transport channel listener implementation for local agent environments.
 * Uses a non-blocking check on System.in to allow clean shutdown.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Local Transport Isolation)
 * Responsibility: Run a background listener thread parsing JSON-RPC line streams from System.in, routing requests
 * to McpProtocolHandler, and writing JSON responses to System.out.
 * Dependencies: {@link McpProtocolHandler}, {@link ObjectMapper}
 * Future Usage: Launched on startup if stdio transport configuration flags are enabled.
 */
public class StdioTransport implements McpTransport, Runnable {

    private final McpProtocolHandler protocolHandler;
    private final ObjectMapper objectMapper;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private Thread listenerThread;
    private volatile boolean running;

    /**
     * Constructs a StdioTransport instance.
     *
     * @param protocolHandler protocol execution gateway handler
     * @param objectMapper JSON utility mapper
     */
    public StdioTransport(McpProtocolHandler protocolHandler, ObjectMapper objectMapper) {
        this(protocolHandler, objectMapper, System.in, System.out);
    }

    /**
     * Constructs a transport over caller-supplied streams.
     * The caller retains ownership of both streams; stopping this transport never closes them.
     *
     * @param protocolHandler protocol execution gateway handler
     * @param objectMapper JSON utility mapper
     * @param inputStream stream from which JSON-RPC requests are read
     * @param outputStream stream to which JSON-RPC responses are written
     */
    public StdioTransport(
        McpProtocolHandler protocolHandler,
        ObjectMapper objectMapper,
        InputStream inputStream,
        OutputStream outputStream
    ) {
        this.protocolHandler = Objects.requireNonNull(protocolHandler, "McpProtocolHandler must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
        this.inputStream = Objects.requireNonNull(inputStream, "Input stream must not be null");
        this.outputStream = Objects.requireNonNull(outputStream, "Output stream must not be null");
        this.running = false;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        listenerThread = new Thread(this, "mcp-stdio-transport-thread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            listenerThread = null;
        }
    }

    @Override
    public void run() {
        // Standard streams are process-owned (and, under Surefire, System.in is its command
        // channel). This transport must never close streams it did not create.
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        try {
            while (running) {
                // Prevent blocking on inputStream.read() to allow clean thread stop during tests/shutdown
                if (inputStream.available() > 0) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        McpRequest request = objectMapper.readValue(line, McpRequest.class);
                        Object response = protocolHandler.handleRequest(request);
                        String rawResponse = objectMapper.writeValueAsString(response);
                        writer.println(rawResponse);
                        writer.flush();
                    } catch (Exception ex) {
                        writer.println("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32700,\"message\":\"Parse error: Invalid JSON-RPC structure\"},\"id\":null}");
                        writer.flush();
                    }
                } else {
                    // Sleep to prevent high CPU utilization
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            // Terminate background loop gracefully on stream close
        }
    }

    /**
     * Helper to verify if the transport is actively listening.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
