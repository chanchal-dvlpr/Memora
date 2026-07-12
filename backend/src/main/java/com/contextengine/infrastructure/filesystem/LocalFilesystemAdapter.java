package com.contextengine.infrastructure.filesystem;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.exception.InfrastructureException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Local filesystem adapter implementing the FilesystemPort contract using standard Java NIO libraries.
 * Handles disk checks, file reads, and recursive workspace file traversal.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Port: FilesystemPort
 * </p>
 */
public class LocalFilesystemAdapter implements FilesystemPort {

    @Override
    public boolean exists(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        return Files.exists(Paths.get(path.value()));
    }

    @Override
    public boolean isDirectory(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        return Files.isDirectory(Paths.get(path.value()));
    }

    @Override
    public boolean hasReadWritePermissions(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        java.nio.file.Path target = Paths.get(path.value());
        return Files.isReadable(target) && Files.isWritable(target);
    }

    @Override
    public List<Path> listFiles(Path root, List<String> exclusions) {
        Objects.requireNonNull(root, "Root path must not be null");
        java.nio.file.Path rootPath = Paths.get(root.value());
        List<Path> results = new ArrayList<>();

        List<PathMatcher> matchers = new ArrayList<>();
        if (exclusions != null) {
            for (String pattern : exclusions) {
                try {
                    matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed glob patterns
                }
            }
        }

        try (Stream<java.nio.file.Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> {
                    java.nio.file.Path relative = rootPath.relativize(p);
                    boolean excluded = matchers.stream().anyMatch(matcher -> matcher.matches(relative));
                    if (!excluded) {
                        results.add(new Path(relative.toString()));
                    }
                });
        } catch (IOException e) {
            throw new InfrastructureException("Failed to recursively traverse directory: " + root.value(), e);
        }

        return results;
    }

    @Override
    public String readFile(Path filePath) {
        Objects.requireNonNull(filePath, "File path must not be null");
        try {
            return Files.readString(Paths.get(filePath.value()));
        } catch (IOException e) {
            throw new InfrastructureException("Failed to read file contents: " + filePath.value(), e);
        }
    }
}
