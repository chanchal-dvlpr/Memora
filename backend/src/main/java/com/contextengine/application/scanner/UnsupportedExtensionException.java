package com.contextengine.application.scanner;

/**
 * Thrown when the requested file extension is not supported by any registered parser.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class UnsupportedExtensionException extends Exception {

    /**
     * Constructs an UnsupportedExtensionException.
     *
     * @param message diagnostic failure message
     */
    public UnsupportedExtensionException(String message) {
        super(message);
    }
}
