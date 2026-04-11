package com.java_template.common.grpc.client;

import com.java_template.common.auth.Authentication;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 * ABOUTME: gRPC client interceptor that adds OAuth2 authorization headers
 * to outgoing requests for secure communication with Cyoda services.
 */
public class ClientAuthorizationInterceptor implements ClientInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAuthorizationInterceptor.class);

    private final Authentication authentication;

    public ClientAuthorizationInterceptor(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                try {
                    // pull the access token from the authentication service.
                    // This will re-use an existing cached token if it is still valid for a short period (minute)
                    OAuth2AccessToken accessToken = authentication.getAccessToken();
                    if (accessToken == null) {
                        LOG.error("Access token is null!");
                    } else {
                        String freshToken = accessToken.getTokenValue();
                        if (freshToken == null || freshToken.isEmpty()) {
                            LOG.error("Access token value is null or empty!");
                        } else {
                            Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
                            headers.put(authKey, "Bearer " + freshToken);
                            LOG.debug("Authorization header added successfully. Token starts with: {}", freshToken.substring(0, Math.min(10, freshToken.length())));
                        }
                    }
                } catch (ClientAuthorizationException e) {
                    LOG.error("Failed to get access token (ClientAuthorizationException). Will not set the Bearer Token: {}", e.getError().getDescription());
                } catch (Exception e) {
                    LOG.error("Failed to get access token (Exception). Will not set the Bearer Token: {}", e.getMessage(), e);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
