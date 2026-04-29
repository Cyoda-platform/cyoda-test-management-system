package com.java_template;

import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test utility to clean up all entity data from tenant.
 * Run manually with: gradle test --tests CleanupTest
 * Requires a live Cyoda instance — disabled in the standard unit-test suite.
 */
@Disabled("Requires a live Cyoda instance — run manually when needed")
@SpringBootTest
@DisplayName("Tenant Cleanup Utility")
class CleanupTest {

    private static final Logger logger = LoggerFactory.getLogger(CleanupTest.class);

    @Autowired
    private EntityService entityService;

    @Test
    @DisplayName("Clean up all entity data")
    void cleanupTenant() throws InterruptedException {
        logger.info("\n=== 🗑️  TENANT CLEANUP STARTED ===\n");
        
        String[] entities = {
            "TestRunStep",
            "TestRunCase",
            "TestRun",
            "Attachment",
            "Defect",
            "TestCase",
            "Suite",
            "Project",
            "ProjectCounter"
        };

        int totalDeleted = 0;

        for (String entityName : entities) {
            try {
                ModelSpec spec = new ModelSpec().withName(entityName).withVersion(1);
                Integer deleted = entityService.deleteAll(spec);
                
                if (deleted > 0) {
                    logger.info("✅ {} → Deleted {}", entityName, deleted);
                    totalDeleted += deleted;
                } else {
                    logger.info("⏭️  {} → (none)", entityName);
                }
                
                Thread.sleep(200);
            } catch (Exception e) {
                logger.warn("⚠️  {} → Error: {}", entityName, e.getMessage());
            }
        }

        logger.info("\n✨ Cleanup complete! Deleted {} total entities\n", totalDeleted);
    }
}
