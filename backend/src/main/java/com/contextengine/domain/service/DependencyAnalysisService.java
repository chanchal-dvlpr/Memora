package com.contextengine.domain.service;

import com.contextengine.domain.entity.Dependency;
import com.contextengine.domain.valueobject.DependencyId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SemanticVersion;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Parses package and project dependency manifests and constructs logical structural dependency trees.
 */
public class DependencyAnalysisService {
    
    /**
     * Parses a manifest file path and returns the list of extracted dependencies.
     * Enforces strict SemVer formats on package references.
     *
     * @param projectId the associated project ID
     * @param manifestPath the path to the manifest file
     * @return collection of extracted dependencies
     * @throws ManifestParseException if manifest file is corrupt, unparseable, or missing
     * @throws NullPointerException if any argument is null
     */
    public Collection<Dependency> parseDependencies(ProjectId projectId, Path manifestPath) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(manifestPath, "Manifest path must not be null");
        
        File file = new File(manifestPath.value());
        if (!file.exists()) {
            throw new ManifestParseException("Manifest file not found: " + manifestPath.value());
        }
        
        List<Dependency> dependencies = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    try {
                        String packageName = parts[0].trim();
                        SemanticVersion version = new SemanticVersion(parts[1].trim());
                        dependencies.add(new Dependency(
                            DependencyId.generate(),
                            projectId,
                            packageName,
                            version,
                            manifestPath
                        ));
                    } catch (IllegalArgumentException e) {
                        throw new ManifestParseException("Invalid semantic version format in manifest line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            throw new ManifestParseException("Error reading manifest file contents: " + e.getMessage());
        }
        return dependencies;
    }
}
