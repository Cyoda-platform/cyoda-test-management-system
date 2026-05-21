package com.java_template.application.config;

import com.java_template.application.entity.UserEntity;
import com.java_template.application.service.UserService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Imports users from users-seed.yml (or the file at APP_USERS_SEED_FILE) at startup.
 * Existing users are skipped — only absent usernames are created.
 * Safe to re-run on every startup.
 */
@Component
public class UserSeederRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeederRunner.class);

    private final UserService userService;
    private final List<SeedEntry> seedEntries;

    /** Production constructor: reads seed from file path or classpath resource. */
    @org.springframework.beans.factory.annotation.Autowired
    public UserSeederRunner(
            UserService userService,
            @Value("${app.seed-file:}") String seedFilePath) {
        this.userService = userService;
        this.seedEntries = loadEntries(seedFilePath);
    }

    /** Test constructor: accepts entries directly. */
    UserSeederRunner(UserService userService, List<SeedEntry> entries) {
        this.userService = userService;
        this.seedEntries = entries;
    }

    @Override
    public void run(String... args) {
        if (seedEntries.isEmpty()) {
            log.debug("No seed entries configured — skipping user seeding");
            return;
        }

        int present = 0;
        int created = 0;

        for (SeedEntry entry : seedEntries) {
            try {
                if (userService.findByUsername(entry.getUsername()).isPresent()) {
                    present++;
                    log.debug("User '{}' already exists — skipping", entry.getUsername());
                } else {
                    UserEntity user = toEntity(entry);
                    userService.createUser(user);
                    created++;
                    log.info("Seeded user '{}'", entry.getUsername());
                }
            } catch (Exception ex) {
                log.error("Failed to seed user '{}': {}", entry.getUsername(), ex.getMessage());
            }
        }

        log.info("User seeding complete — {} already present, {} created", present, created);
    }

    private UserEntity toEntity(SeedEntry entry) {
        UserEntity user = new UserEntity();
        user.setUsername(entry.getUsername());
        user.setEmail(entry.getEmail() != null ? entry.getEmail() : "");
        user.setPasswordHash(entry.getPasswordHash());
        user.setRoles(entry.getRoles() != null ? entry.getRoles() : List.of());
        user.setCreatedAt(Instant.now().toString());
        return user;
    }

    @SuppressWarnings("unchecked")
    private List<SeedEntry> loadEntries(String seedFilePath) {
        try {
            InputStream stream = resolveStream(seedFilePath);
            if (stream == null) {
                log.warn("No users-seed.yml found (checked path='{}', classpath). Skipping seeding.",
                        seedFilePath);
                return List.of();
            }
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(stream);
            if (root == null || !root.containsKey("users")) return List.of();

            List<Map<String, Object>> users = (List<Map<String, Object>>) root.get("users");
            List<SeedEntry> result = new ArrayList<>();
            for (Map<String, Object> u : users) {
                SeedEntry e = new SeedEntry();
                e.setUsername((String) u.get("username"));
                e.setEmail((String) u.get("email"));
                e.setPasswordHash((String) u.get("passwordHash"));
                Object roles = u.get("roles");
                e.setRoles(roles instanceof List ? (List<String>) roles : List.of());
                result.add(e);
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to load seed file: {}", ex.getMessage());
            return List.of();
        }
    }

    private InputStream resolveStream(String seedFilePath) throws Exception {
        if (seedFilePath != null && !seedFilePath.isBlank()) {
            Path path = Path.of(seedFilePath);
            if (Files.isReadable(path)) return Files.newInputStream(path);
        }
        return getClass().getClassLoader().getResourceAsStream("users-seed.yml");
    }

    @Data
    public static class SeedEntry {
        private String username;
        private String email;
        private String passwordHash;
        private List<String> roles = new ArrayList<>();
    }
}
