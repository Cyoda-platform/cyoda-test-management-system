package com.java_template.application.auth;

import com.java_template.application.service.UserService;
import com.java_template.common.auth.AuthClaimsParser;
import com.java_template.common.auth.CloudEventAuthContext;
import com.java_template.common.auth.EventUserIdentity;
import com.java_template.common.auth.EventUserResolutionException;
import com.java_template.common.auth.EventUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Resolves and verifies user identity from Cyoda CloudEvent auth context.
 * Replaces the no-op DefaultEventUserResolver — every user-type event is
 * verified against the User entity in Cyoda before the identity is installed.
 */
@Component
public class TmsEventUserResolver implements EventUserResolver {

    private static final Logger log = LoggerFactory.getLogger(TmsEventUserResolver.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public TmsEventUserResolver(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EventUserIdentity resolve(CloudEventAuthContext authContext) {
        String authId = authContext.authId();

        if (authId == null || authId.isBlank()) {
            throw new EventUserResolutionException(
                    "authtype=user but authid is absent or blank");
        }

        UUID userId;
        try {
            userId = UUID.fromString(authId);
        } catch (IllegalArgumentException ex) {
            throw new EventUserResolutionException(
                    "authid is not a valid UUID: " + authId);
        }

        var userRecord = userService.findById(userId)
                .orElseThrow(() -> new EventUserResolutionException(
                        "User entity not found for authid: " + userId));

        String state = userRecord.getState();
        if (!"ACTIVE".equals(state)) {
            throw new EventUserResolutionException(
                    "User " + userId + " is not ACTIVE (state=" + state + ")");
        }

        List<String> roles = AuthClaimsParser.parseRoles(objectMapper, authContext.authClaimsJson());
        log.debug("Resolved CloudEvent user: id={}, roles={}", userId, roles);
        return new EventUserIdentity(userId.toString(), roles);
    }
}
