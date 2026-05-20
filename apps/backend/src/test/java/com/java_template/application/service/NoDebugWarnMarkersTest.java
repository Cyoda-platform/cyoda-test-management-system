package com.java_template.application.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IM-10: log.warn("===== ... =====") markers were added to diagnose a race condition
 * that has since been fixed. Verifies they have been removed from production code.
 */
class NoDebugWarnMarkersTest {

    @Test
    void testRunServiceHasNoEqualsWarnMarkers() throws IOException {
        Path path = Paths.get("src/main/java/com/java_template/application/service/TestRunService.java");
        List<String> offending = linesWithPattern(path, "log.warn.*=====");

        assertThat(offending)
                .as("TestRunService must not contain log.warn(\"===== ... =====\") debug markers — demote to debug or remove")
                .isEmpty();
    }

    @Test
    void testRunControllerHasNoEqualsWarnMarkers() throws IOException {
        Path path = Paths.get("src/main/java/com/java_template/application/controller/TestRunController.java");
        List<String> offending = linesWithPattern(path, "log.warn.*=====");

        assertThat(offending)
                .as("TestRunController must not contain log.warn(\"===== ... =====\") debug markers")
                .isEmpty();
    }

    private List<String> linesWithPattern(Path file, String pattern) throws IOException {
        return Files.lines(file)
                .filter(line -> line.matches(".*" + pattern + ".*"))
                .collect(Collectors.toList());
    }
}
