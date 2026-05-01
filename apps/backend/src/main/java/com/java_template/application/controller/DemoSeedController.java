package com.java_template.application.controller;

import com.java_template.application.service.DemoSeederService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Trigger endpoint for seeding the "E-commerce Platform" demo project.
 *
 * POST /demo/seed
 *   — accessible by any authenticated user (ADMIN or TESTER)
 *   — idempotent: returns 200 + "skipped" if the demo project already exists
 *   — returns 201 + summary on first successful seed
 */
@RestController
@RequestMapping("/demo")
@Tag(name = "Demo", description = "Demo data seeding endpoints")
public class DemoSeedController {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedController.class);

    private final DemoSeederService demoSeederService;

    public DemoSeedController(DemoSeederService demoSeederService) {
        this.demoSeederService = demoSeederService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed the E-commerce Platform demo project",
               description = "Creates a pre-populated demo project with suites, test cases, a completed test run, defects, evidence, and a report. Idempotent — safe to call multiple times.")
    public ResponseEntity<Map<String, Object>> seed(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        String role     = (String) request.getAttribute("role");
        log.info("[DemoSeed] Seed requested by user='{}' role='{}'", username, role);

        Map<String, Object> result = demoSeederService.seed();

        boolean created = "created".equals(result.get("status"));
        return created
                ? ResponseEntity.status(201).body(result)
                : ResponseEntity.ok(result);
    }
}
