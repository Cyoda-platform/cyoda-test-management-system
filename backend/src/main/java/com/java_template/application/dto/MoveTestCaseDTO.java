package com.java_template.application.dto;

import java.util.UUID;

/**
 * Payload for moving a test case to a different suite.
 * Carries the destination suite ID and the new sort position within that suite.
 */
public record MoveTestCaseDTO(UUID targetSuiteId, Integer sortOrder) {}
