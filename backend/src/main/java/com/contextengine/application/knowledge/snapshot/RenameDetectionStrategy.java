package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.valueobject.EngineeringEvidence;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Strategy to detect renamed files among unmatched previous and current entities.
 */
public class RenameDetectionStrategy implements SnapshotComparisonAlgorithm {

    @Override
    public void compare(
        List<EngineeringEvidence> prev,
        List<EngineeringEvidence> curr,
        SnapshotComparisonConfiguration config,
        List<SnapshotDifference> differences,
        Set<EngineeringEvidence> matchedPrev,
        Set<EngineeringEvidence> matchedCurr
    ) {
        if (!config.detectRenames() || !config.compareHashes()) {
            return;
        }

        for (EngineeringEvidence c : curr) {
            if (matchedCurr.contains(c)) {
                continue;
            }

            for (EngineeringEvidence p : prev) {
                if (matchedPrev.contains(p)) {
                    continue;
                }

                if (Objects.equals(p.fileContentHash().value(), c.fileContentHash().value())) {
                    String pName = getFilename(p.filePath().value());
                    String cName = getFilename(c.filePath().value());

                    if (!Objects.equals(pName, cName)) {
                        differences.add(new SnapshotDifference(
                            SnapshotDifferenceType.RENAMED, p, c,
                            "Entity renamed from " + p.filePath().value() + " to " + c.filePath().value()
                        ));
                        matchedPrev.add(p);
                        matchedCurr.add(c);
                        break;
                    }
                }
            }
        }
    }

    private String getFilename(String path) {
        if (path == null) return "";
        int index = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return index == -1 ? path : path.substring(index + 1);
    }
}
