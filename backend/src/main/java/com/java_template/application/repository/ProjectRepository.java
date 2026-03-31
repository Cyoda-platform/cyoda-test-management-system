package com.java_template.application.repository;

import com.java_template.application.dto.ProjectDTO;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for Projects
 */
@Repository
public class ProjectRepository {
    private final Map<UUID, ProjectDTO> projects = new ConcurrentHashMap<>();

    public ProjectDTO create(ProjectDTO project) {
        UUID id = UUID.randomUUID();
        project.setId(id);
        String now = java.time.LocalDateTime.now().toString() + "Z";
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projects.put(id, project);
        return project;
    }

    public Optional<ProjectDTO> findById(UUID id) {
        return Optional.ofNullable(projects.get(id));
    }

    public List<ProjectDTO> findAll() {
        return new ArrayList<>(projects.values());
    }

    public Optional<ProjectDTO> findByName(String name) {
        return projects.values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    public ProjectDTO update(UUID id, ProjectDTO project) {
        return projects.computeIfPresent(id, (k, existing) -> {
            // Merge: update only non-null fields
            if (project.getName() != null) {
                existing.setName(project.getName());
            }
            if (project.getDescription() != null) {
                existing.setDescription(project.getDescription());
            }
            if (project.getStatus() != null) {
                existing.setStatus(project.getStatus());
            }
            existing.setUpdatedAt(java.time.LocalDateTime.now().toString() + "Z");
            return existing;
        });
    }

    public boolean delete(UUID id) {
        return projects.remove(id) != null;
    }

    public boolean exists(UUID id) {
        return projects.containsKey(id);
    }
}

