package com.java_template.application.config;

import com.java_template.application.service.DemoSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Automatically seeds the "E-commerce Platform" demo project once,
 * right after the application is fully started.
 *
 * Runs in a background thread with a short delay so the application
 * is fully ready and Cyoda connections are established before the
 * first entity-service calls are made.
 *
 * Idempotent: if the demo project already exists the seed is skipped silently.
 */
@Component
public class DemoSeederRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeederRunner.class);

    /** Delay before seeding starts — gives Cyoda gRPC connections time to warm up. */
    private static final long STARTUP_DELAY_MS = 5_000;

    private final DemoSeederService demoSeederService;

    public DemoSeederRunner(DemoSeederService demoSeederService) {
        this.demoSeederService = demoSeederService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Thread seederThread = new Thread(() -> {
            try {
                log.info("[DemoSeederRunner] Waiting {}s for Cyoda connections to warm up...", STARTUP_DELAY_MS / 1000);
                Thread.sleep(STARTUP_DELAY_MS);

                log.info("[DemoSeederRunner] Starting demo data seed...");
                Map<String, Object> result = demoSeederService.seed();

                String status = (String) result.get("status");
                if ("created".equals(status)) {
                    log.info("[DemoSeederRunner] Demo project seeded successfully. projectId={}, runId={}",
                            result.get("projectId"), result.get("runId"));
                } else {
                    log.info("[DemoSeederRunner] Demo project already exists — seed skipped. projectId={}",
                            result.get("projectId"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[DemoSeederRunner] Seeder thread interrupted");
            } catch (Exception e) {
                log.error("[DemoSeederRunner] Demo seed failed: {}. " +
                          "The application will continue running; seed can be retried via POST /demo/seed",
                          e.getMessage(), e);
            }
        }, "demo-seeder");

        seederThread.setDaemon(true);
        seederThread.start();
    }
}
