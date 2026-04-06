package com.java_template.application.dto;

import java.util.UUID;

/**
 * Payload item for bulk reorder operations.
 * Contains the entity UUID and its new sequential sort position.
 */
public record ReorderItemDTO(UUID id, Integer sortOrder) {}
