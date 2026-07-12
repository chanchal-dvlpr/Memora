package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.List;
import java.util.Objects;

/**
 * Governs order and exclusions within search execution and context ranking paths.
 */
public record FilterCriteria(
    List<Path> excludedPaths,
    List<String> allowedFileExtensions,
    Priority minimumPriority
) implements ValueObject {
    
    /**
     * Constructs a FilterCriteria instance. Performs defensive copying of collection parameters.
     *
     * @param excludedPaths paths to exclude from search or ranking
     * @param allowedFileExtensions file extensions permitted in results
     * @param minimumPriority the minimum priority threshold required
     * @throws NullPointerException if any parameter is null
     */
    public FilterCriteria {
        Objects.requireNonNull(excludedPaths, "Excluded paths must not be null");
        Objects.requireNonNull(allowedFileExtensions, "Allowed file extensions must not be null");
        Objects.requireNonNull(minimumPriority, "Minimum priority must not be null");
        
        excludedPaths = List.copyOf(excludedPaths);
        allowedFileExtensions = List.copyOf(allowedFileExtensions);
    }
}
