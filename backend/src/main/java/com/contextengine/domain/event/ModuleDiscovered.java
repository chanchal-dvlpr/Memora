package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ModuleId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Flags the discovery of a logical module boundary, manifest file, or directory layer.
 */
public record ModuleDiscovered(
    ModuleId moduleId,
    ProjectId projectId,
    String moduleName,
    Path manifestPath,
    Instant occurredAt
) implements DomainEvent {
    
    public ModuleDiscovered {
        Objects.requireNonNull(moduleId, "ModuleId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(manifestPath, "Manifest path must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name must not be null or empty");
        }
    }
}
