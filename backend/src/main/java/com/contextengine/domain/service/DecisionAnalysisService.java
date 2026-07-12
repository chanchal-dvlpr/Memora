package com.contextengine.domain.service;

import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

/**
 * Coordinates the parsing, tracking, and bidirectional linking of Architecture Decision Records (ADRs).
 */
public class DecisionAnalysisService {
    
    /**
     * Parses an ADR markdown file and extracts the Decision entity.
     *
     * @param projectId the associated project ID
     * @param adrPath the path to the ADR markdown file
     * @return the parsed Decision entity
     * @throws InvalidADRFormatException if critical metadata or heading is missing
     * @throws NullPointerException if any argument is null
     */
    public Decision parseADR(ProjectId projectId, Path adrPath) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(adrPath, "ADR path must not be null");
        
        File file = new File(adrPath.value());
        if (!file.exists()) {
            throw new InvalidADRFormatException("ADR markdown file not found: " + adrPath.value());
        }
        
        String title = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("# ")) {
                    title = line.substring(2).trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new InvalidADRFormatException("Failed to read ADR file contents: " + e.getMessage());
        }
        
        if (title == null || title.isEmpty()) {
            throw new InvalidADRFormatException("Invalid ADR format: markdown file must start with a level-1 heading title (# Title)");
        }
        
        return new Decision(DecisionId.generate(), projectId, title, adrPath);
    }
}
