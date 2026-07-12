package com.contextengine.infrastructure.storage;

import com.contextengine.infrastructure.exception.InfrastructureException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Storage adapter implementing technical raw disk write and read operations.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class LocalStorageAdapter {

    /**
     * Writes data to a local storage file path.
     *
     * @param targetPath the destination path
     * @param content content to write
     */
    public void writeData(String targetPath, String content) {
        Objects.requireNonNull(targetPath, "Target path must not be null");
        Objects.requireNonNull(content, "Content must not be null");
        try {
            Path path = Paths.get(targetPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new InfrastructureException("Failed to write data to path: " + targetPath, e);
        }
    }

    /**
     * Reads text data from a local storage file path.
     *
     * @param sourcePath the source path
     * @return the read content string
     */
    public String readData(String sourcePath) {
        Objects.requireNonNull(sourcePath, "Source path must not be null");
        try {
            return Files.readString(Paths.get(sourcePath));
        } catch (IOException e) {
            throw new InfrastructureException("Failed to read data from path: " + sourcePath, e);
        }
    }
}
