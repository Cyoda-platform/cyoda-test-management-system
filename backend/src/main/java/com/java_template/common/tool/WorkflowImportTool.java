package com.java_template.common.tool;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.common.auth.Authentication;
import com.java_template.common.config.Config;
import com.java_template.common.util.HttpUtils;
import com.java_template.common.util.JsonUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * ABOUTME: Command-line tool for importing workflow definitions into Cyoda platform
 */
public class WorkflowImportTool {
    public static void main(String[] args) {
        // Parse command line arguments using JCommander
        CyodaInitConfig initConfig = new CyodaInitConfig();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(initConfig)
                .programName("WorkflowImportTool")
                .build();

        try {
            jCommander.parse(args);
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        // Display help if requested
        if (initConfig.help()) {
            jCommander.usage();
            System.exit(0);
        }

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Authentication.class, HttpUtils.class, JsonUtils.class);
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        context.refresh();

        Authentication auth = context.getBean(Authentication.class);
        HttpUtils httpUtils = context.getBean(HttpUtils.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
        Config config = context.getBean(Config.class);

        CyodaInit init = new CyodaInit(httpUtils, auth, objectMapper,config);
        int exitCode = 0;
        try {
            CyodaInit.ImportReport report = init.initCyoda(initConfig);
            if (!report.success()) {
                System.err.println(report.message());
                report.results().stream()
                        .filter(result -> !result.success())
                        .forEach(result -> System.err.printf(" - %s.%d: %s%n", result.entityName(), result.version(), result.message()));
                exitCode = 2;
            }
        } finally {
            context.close();
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}

