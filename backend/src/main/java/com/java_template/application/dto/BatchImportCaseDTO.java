package com.java_template.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for the batch-import endpoint.
 * Each element represents one test case to create, with optional embedded steps.
 * The batch endpoint pre-allocates all display IDs in a single counter round-trip
 * instead of hitting the counter table once per case.
 */
@Data
@NoArgsConstructor
public class BatchImportCaseDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 2000)
    private String preconditions;

    private Priority priority;

    /** Optional ordered list of steps to create along with the case. */
    private List<StepDTO> steps;

    @Data
    @NoArgsConstructor
    public static class StepDTO {
        private Integer stepNumber;
        @Size(max = 2000)
        private String action;
        @Size(max = 2000)
        private String expectedResult;
    }
}
