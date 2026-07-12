package com.contextengine.domain.specification;

import com.contextengine.domain.valueobject.SemanticVersion;
import java.util.regex.Pattern;

/**
 * Enforces the SEMVER_COMPLIANCE rule: Package dependencies must conform to semantic versioning patterns.
 */
public class SemVerComplianceSpecification implements Specification<SemanticVersion> {
    
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
        "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    );

    @Override
    public boolean isSatisfiedBy(SemanticVersion version) {
        if (version == null) {
            return false;
        }
        return SEMVER_PATTERN.matcher(version.value()).matches();
    }
}
