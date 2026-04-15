package com.java_template.application.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Priority levels for test cases.
 */
public enum Priority {
    HIGH,
    MEDIUM,
    LOW;

    @JsonValue
    public String getValue() {
        return this.name();
    }
}

