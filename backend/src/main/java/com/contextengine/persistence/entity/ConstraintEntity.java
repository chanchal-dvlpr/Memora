package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity representing a project Constraint statement.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(name = "project_constraints")
public class ConstraintEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "constraint_id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "constraint_type", length = 255, nullable = false)
    private String constraintType;

    @Column(name = "rules_serialized", length = 4096, nullable = false)
    private String rulesSerialized; // key:value,key2:value2

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    public String getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(String constraintType) {
        this.constraintType = constraintType;
    }

    public String getRulesSerialized() {
        return rulesSerialized;
    }

    public void setRulesSerialized(String rulesSerialized) {
        this.rulesSerialized = rulesSerialized;
    }
}
