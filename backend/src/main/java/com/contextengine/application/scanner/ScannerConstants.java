package com.contextengine.application.scanner;

import java.util.Set;

/**
 * Global constant definitions and default values for the Project Scanner.
 * Specifies system-wide exclusions for binary directories, archives, and files.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public final class ScannerConstants {

    private ScannerConstants() {
        // Prevent instantiation of utility constant class
    }

    /**
     * Default list of directory names to skip recursively.
     */
    public static final Set<String> DEFAULT_IGNORED_DIRECTORIES = Set.of(
        ".git",
        "node_modules",
        "vendor",
        "dist",
        "build",
        "target",
        "bin",
        "obj",
        ".idea",
        ".vscode",
        ".gradle",
        ".settings"
    );

    /**
     * Default set of file extensions representing non-text, binary, or compiled formats.
     */
    public static final Set<String> DEFAULT_IGNORED_EXTENSIONS = Set.of(
        "zip", "tar", "gz", "rar", "7z",
        "exe", "class", "jar", "war", "ear",
        "png", "jpg", "jpeg", "gif", "ico", "bmp", "pdf",
        "dll", "so", "dylib", "bin", "o", "a",
        "mp4", "mp3", "wav", "avi", "mov",
        "woff", "woff2", "ttf", "eot"
    );

    /**
     * Maximum scannable file size limit (10MB) to prevent vector index dilution.
     */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L; // 10MB
}
