package com.java_template.common.tool;

import org.cyoda.cloud.api.event.common.ModelSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.java_template.common.service.EntityService;

/**
 * Utility to clean up tenant data.
 * Usage: java -cp ... com.java_template.common.tool.CleanupTenantTool
 */
public class CleanupTenantTool {
    private static final Logger logger = LoggerFactory.getLogger(CleanupTenantTool.class);

    private final EntityService entityService;

    public CleanupTenantTool(EntityService entityService) {
        this.entityService = entityService;
    }

    public void cleanup() {
        logger.info("\n=== 🗑️  TENANT CLEANUP ===\n");
        
        String[] entities = {
            "TestRunStep",
            "TestRunCase",
            "TestRun",
            "Attachment",
            "Defect",
            "TestCase",
            "Suite",
            "Project",
            "TestStep",
            "ProjectCounter"
        };

        int totalDeleted = 0;

        for (String entityName : entities) {
            try {
                ModelSpec spec = new ModelSpec().withName(entityName).withVersion(1);
                Integer deleted = entityService.deleteAll(spec);
                
                if (deleted > 0) {
                    logger.info("✅ {} → {}", entityName, deleted);
                    totalDeleted += deleted;
                } else {
                    logger.info("⏭️  {} → (none)", entityName);
                }
            } catch (Exception e) {
                logger.warn("⚠️  {} → {}", entityName, e.getMessage());
            }
        }

        logger.info("\n✨ Cleanup done! Deleted {} total\n", totalDeleted);
    }

    public static void main(String[] args) {
        logger.info("This tool should be invoked via Spring context");
        System.exit(1);
    }
}
