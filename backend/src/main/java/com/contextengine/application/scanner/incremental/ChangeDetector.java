package com.contextengine.application.scanner.incremental;

import com.contextengine.application.scanner.ScanCandidate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects changes (added, modified, deleted) in workspace files by comparing current candidates
 * with cached file fingerprints. Prevents cross-project contamination by partitioning the cache by Project ID.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ChangeDetector {

    private final Map<String, Map<String, FileFingerprint>> projectCache = new ConcurrentHashMap<>();

    /**
     * Constructs a ChangeDetector.
     */
    public ChangeDetector() {
    }

    /**
     * Compares current candidates against cached fingerprints to determine added, modified, and deleted files.
     *
     * @param projectId unique project identifier
     * @param currentCandidates discovered candidates in the current run
     * @return the calculated ScanDelta
     */
    public ScanDelta detect(String projectId, Collection<ScanCandidate> currentCandidates) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(currentCandidates, "CurrentCandidates must not be null");

        Map<String, FileFingerprint> cached = projectCache.get(projectId);
        if (cached == null || cached.isEmpty()) {
            // First scan or cache cleared: treat all discovered files as added
            return new ScanDelta(new ArrayList<>(currentCandidates), List.of(), List.of());
        }

        List<ScanCandidate> added = new ArrayList<>();
        List<ScanCandidate> modified = new ArrayList<>();
        Set<String> currentPaths = new HashSet<>();

        for (ScanCandidate candidate : currentCandidates) {
            String path = candidate.relativePath();
            currentPaths.add(path);

            FileFingerprint fingerprint = cached.get(path);
            if (fingerprint == null) {
                added.add(candidate);
            } else if (candidate.size() != fingerprint.size() || 
                       candidate.lastModified().toEpochMilli() != fingerprint.lastModified()) {
                modified.add(candidate);
            }
        }

        List<String> deleted = new ArrayList<>();
        for (String cachedPath : cached.keySet()) {
            if (!currentPaths.contains(cachedPath)) {
                deleted.add(cachedPath);
            }
        }

        return new ScanDelta(added, modified, deleted);
    }

    /**
     * Updates the cached fingerprints for a project after a successful scan.
     *
     * @param projectId unique project identifier
     * @param currentCandidates the current verified file candidates
     */
    public void update(String projectId, Collection<ScanCandidate> currentCandidates) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(currentCandidates, "CurrentCandidates must not be null");

        Map<String, FileFingerprint> newCache = new HashMap<>();
        for (ScanCandidate candidate : currentCandidates) {
            newCache.put(candidate.relativePath(), new FileFingerprint(
                candidate.relativePath(),
                candidate.size(),
                candidate.lastModified().toEpochMilli()
            ));
        }

        projectCache.put(projectId, newCache);
    }

    /**
     * Clears cached fingerprints for a specific project.
     *
     * @param projectId unique project identifier
     */
    public void clear(String projectId) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        projectCache.remove(projectId);
    }
}
