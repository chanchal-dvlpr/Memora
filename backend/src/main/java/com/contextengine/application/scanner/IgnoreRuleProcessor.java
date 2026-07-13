package com.contextengine.application.scanner;

import com.contextengine.domain.entity.Project;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses ignore and exclusion patterns, compiling them into path matchers
 * to evaluate whether absolute or relative workspace paths should be skipped.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class IgnoreRuleProcessor {

    private final List<PathMatcher> matchers = new ArrayList<>();
    private final List<String> rawExclusions = new ArrayList<>();

    /**
     * Constructs an IgnoreRuleProcessor compiling default and custom rules.
     *
     * @param project target project
     * @param customExclusions custom exclusion glob patterns
     */
    public IgnoreRuleProcessor(Project project, List<String> customExclusions) {
        Objects.requireNonNull(project, "Project must not be null");

        // Compile default directory patterns
        for (String dir : ScannerConstants.DEFAULT_IGNORED_DIRECTORIES) {
            addPattern("**/" + dir + "/**");
            addPattern("**/" + dir);
        }

        // Compile project configuration rules
        if (customExclusions != null) {
            for (String pattern : customExclusions) {
                addPattern(pattern);
            }
        }

        // Parse and compile local .gitignore if exists in project root
        Path gitIgnorePath = Paths.get(project.rootDirectory().value(), ".gitignore");
        if (Files.exists(gitIgnorePath) && Files.isRegularFile(gitIgnorePath)) {
            try {
                List<String> lines = Files.readAllLines(gitIgnorePath);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        // Translate gitignore to standard glob patterns
                        if (trimmed.startsWith("/")) {
                            addPattern(trimmed.substring(1));
                            addPattern(trimmed.substring(1) + "/**");
                        } else {
                            addPattern("**/" + trimmed);
                            addPattern("**/" + trimmed + "/**");
                        }
                    }
                }
            } catch (IOException ignored) {
                // Fail-safe: ignore gitignore read issues
            }
        }
    }

    private void addPattern(String pattern) {
        String normalized = pattern.replace('\\', '/');
        rawExclusions.add(normalized);
        try {
            // Support both standard matching formats
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + normalized));
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed patterns
        }
    }

    /**
     * Determines if a given path string matches any ignore rules.
     *
     * @param relativePathStr relative path from the project root
     * @param isDirectory whether the path points to a directory node
     * @return true if path matches ignore criteria, false otherwise
     */
    public boolean shouldIgnore(String relativePathStr, boolean isDirectory) {
        Objects.requireNonNull(relativePathStr, "Relative path must not be null");

        String pathStr = relativePathStr.replace('\\', '/');

        // Segment-level checks for default directory names to ensure safe matches
        String[] segments = pathStr.split("/");
        for (String segment : segments) {
            if (ScannerConstants.DEFAULT_IGNORED_DIRECTORIES.contains(segment)) {
                return true;
            }
        }

        Path path = Paths.get(pathStr);
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getRawExclusions() {
        return List.copyOf(rawExclusions);
    }
}
