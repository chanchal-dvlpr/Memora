package com.contextengine.domain.validation;

import com.contextengine.domain.entity.Dependency;
import com.contextengine.domain.specification.SemVerComplianceSpecification;
import java.util.Objects;

/**
 * Enforces the SEMVER_COMPLIANCE constraint on package dependency structures.
 */
public class DependencyValidator implements Validator<Dependency> {
    
    private final SemVerComplianceSpecification semVerSpec = new SemVerComplianceSpecification();

    @Override
    public void validate(Dependency dependency) throws ValidationException {
        if (dependency == null) {
            throw new ValidationException("Dependency to validate must not be null");
        }
        
        if (!semVerSpec.isSatisfiedBy(dependency.version())) {
            throw new ValidationException("Dependency '" + dependency.packageName() + 
                "' version does not comply with SemVer requirements: " + dependency.version().value());
        }
    }
}
