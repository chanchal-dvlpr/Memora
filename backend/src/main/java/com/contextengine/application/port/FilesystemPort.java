package com.contextengine.application.port;

import com.contextengine.domain.valueobject.Path;
import java.util.List;

/**
 * Port interface abstracting physical filesystem operations.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner
 * </p>
 */
public interface FilesystemPort {
    
    /**
     * Checks if the specified directory path exists on the host workstation.
     *
     * @param path the path to check
     * @return true if exists, false otherwise
     */
    boolean exists(Path path);

    /**
     * Checks if the specified path points to a directory.
     *
     * @param path the path to check
     * @return true if directory, false otherwise
     */
    boolean isDirectory(Path path);

    /**
     * Verifies if the application has read/write permissions for the target path.
     *
     * @param path the path to check
     * @return true if permissions are valid, false otherwise
     */
    boolean hasReadWritePermissions(Path path);

    /**
     * Recursively lists all file paths under the registered root directory, applying exclusions.
     *
     * @param root the project root path
     * @param exclusions list of glob pattern exclusions
     * @return list of relative file paths
     */
    List<Path> listFiles(Path root, List<String> exclusions);

    /**
     * Reads the content of a file as a String.
     *
     * @param filePath the file path
     * @return the string content of the file
     */
    String readFile(Path filePath);
}
