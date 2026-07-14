package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strategy matching entities by identical file paths.
 */
public class ExactPathComparisonStrategy implements SnapshotComparisonAlgorithm {

    @Override
    public void compare(
        List<EngineeringEvidence> prev,
        List<EngineeringEvidence> curr,
        SnapshotComparisonConfiguration config,
        List<SnapshotDifference> differences,
        Set<EngineeringEvidence> matchedPrev,
        Set<EngineeringEvidence> matchedCurr
    ) {
        Map<Path, EngineeringEvidence> prevByPath = new HashMap<>();
        for (EngineeringEvidence e : prev) {
            prevByPath.put(e.filePath(), e);
        }

        for (EngineeringEvidence c : curr) {
            EngineeringEvidence p = prevByPath.get(c.filePath());
            if (p != null) {
                matchedPrev.add(p);
                matchedCurr.add(c);

                boolean hashesMatch = !config.compareHashes() ||
                    Objects.equals(p.fileContentHash().value(), c.fileContentHash().value());
                
                boolean metadataMatch = !config.compareMetadata() ||
                    (p.startLine() == c.startLine() && p.endLine() == c.endLine());

                if (hashesMatch && metadataMatch) {
                    differences.add(new SnapshotDifference(SnapshotDifferenceType.UNCHANGED, p, c, "Entity is unchanged"));
                } else {
                    differences.add(new SnapshotDifference(SnapshotDifferenceType.MODIFIED, p, c, "Entity content or line range was modified"));
                }
            }
        }
    }
}
