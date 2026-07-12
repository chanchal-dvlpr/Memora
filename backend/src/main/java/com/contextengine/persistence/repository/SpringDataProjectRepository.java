package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for ProjectEntity persistence operations.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public interface SpringDataProjectRepository extends JpaRepository<ProjectEntity, String> {

    /**
     * Resolves a ProjectEntity by its root path.
     *
     * @param path the root path string
     * @return optional containing the found ProjectEntity, or empty
     */
    @Query("SELECT p FROM ProjectEntity p WHERE p.rootPath = :path")
    Optional<ProjectEntity> findByRootPath(@Param("path") String path);

    /**
     * Finds all project entities marked as ACTIVE.
     *
     * @return collection of active ProjectEntity objects
     */
    @Query("SELECT p FROM ProjectEntity p WHERE p.state = 'ACTIVE'")
    Collection<ProjectEntity> findAllActive();
}
