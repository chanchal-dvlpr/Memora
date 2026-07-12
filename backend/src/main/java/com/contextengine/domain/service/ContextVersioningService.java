package com.contextengine.domain.service;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.Version;
import java.util.Collection;
import java.util.Objects;

/**
 * Governs differential delta encoding, sequence tracing, and rollback operations for context snapshot repositories.
 */
public class ContextVersioningService {
    
    /**
     * Reconstructs a historical snapshot at the target version sequence marker.
     *
     * @param targetVersion the target version sequence marker to retrieve
     * @param history the collection of historical snapshots
     * @return the reconstructed ContextSnapshot
     * @throws VersionNotFoundException if the target sequence version is missing in the history
     * @throws NullPointerException if any argument is null
     */
    public ContextSnapshot rollbackToVersion(Version targetVersion, Collection<ContextSnapshot> history) {
        Objects.requireNonNull(targetVersion, "Target version must not be null");
        Objects.requireNonNull(history, "History must not be null");
        
        return history.stream()
            .filter(s -> s.version().equals(targetVersion))
            .findFirst()
            .orElseThrow(() -> new VersionNotFoundException("Target version sequence marker '" + targetVersion.value() + "' not found in snapshot history"));
    }
}
