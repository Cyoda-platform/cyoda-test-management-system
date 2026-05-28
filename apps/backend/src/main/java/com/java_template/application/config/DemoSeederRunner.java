package com.java_template.application.config;

import com.java_template.application.service.DemoSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Automatically seeds the "E-commerce Platform" demo project once,
 * right after the application is fully started.
 *
 * Enabled by default — disable with {@code APP_DEMO_SEED_ON_STARTUP=false} in the environment.
 * Safe to leave enabled in production: the seed is skipped silently if Cyoda already
 * contains any projects (i.e. it only runs once, on a completely fresh tenant).
 *
 * Idempotent: if Cyoda is non-empty the seed is skipped silently.
 */
@Component
@ConditionalOnProperty(name = "app.demo.seed-on-startup", havingValue = "true", matchIfMissing = true)
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
                    log.info("[DemoSeederRunner] Cyoda already has projects — seed skipped.");
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
