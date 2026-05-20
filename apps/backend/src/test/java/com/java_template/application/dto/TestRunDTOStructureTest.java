package com.java_template.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IM-08: TestRunDTO must not own a static ObjectMapper.
 * Serialisation of stepStatuses belongs in service layer where Spring's
 * configured ObjectMapper (with all registered modules) is available.
 */
class TestRunDTOStructureTest {

    @Test
    void testRunDtoHasNoStaticObjectMapperField() {
        long count = Arrays.stream(TestRunDTO.class.getDeclaredFields())
                .filter(f -> f.getType().equals(ObjectMapper.class))
                .count();

        assertThat(count)
                .as("TestRunDTO must not own a static ObjectMapper — move serialisation to service layer")
                .isZero();
    }
}
