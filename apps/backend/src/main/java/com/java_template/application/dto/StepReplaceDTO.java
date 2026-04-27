package com.java_template.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StepReplaceDTO {
    private Integer stepNumber;

    @Size(max = 2000)
    private String action;

    @Size(max = 2000)
    private String expectedResult;
}
