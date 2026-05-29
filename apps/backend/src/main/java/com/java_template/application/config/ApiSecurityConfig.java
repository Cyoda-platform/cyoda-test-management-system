package com.java_template.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Application security configuration.
 * Overrides the permissive default in common/config/SecurityConfig.java.
 *
 * Token sources (in precedence order):
 *   1. auth-token httpOnly cookie  (set by AuthController on login)
 *   2. Authorization: Bearer header (Swagger UI, API clients)
 *
 * Algorithm is pinned to HS256 — alg=none and asymmetric algorithms are rejected.
 */
@Configuration
@EnableMethodSecurity
public class ApiSecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/auth/login", "/auth/logout",
            "/actuator/**",
            "/swagger-ui/**", "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/admin/grpc/import-workflows"
    };

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            TmsBearerTokenResolver bearerTokenResolver,
            @Value("${app.auth.secret}") String secret) throws Exception {

        SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();

        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver)
                .jwt(jwt -> jwt
                    .decoder(decoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Bridge: propagates 'role' and 'username' from SecurityContextHolder into
            // request attributes so controllers that use request.getAttribute("role") keep working.
            .addFilterAfter(new RoleAttributeBridgeFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins,
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "Origin", "X-Requested-With", "Cache-Control"));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Reads the 'role' and 'sub' claims from the validated JWT and sets them as request
     * attributes, preserving backward compatibility with controllers that use
     * request.getAttribute("role") and request.getAttribute("username").
     */
    static class RoleAttributeBridgeFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwt) {
                String role            = jwt.getToken().getClaimAsString("role");
                String sub             = jwt.getToken().getClaimAsString("sub");
                String displayName     = jwt.getToken().getClaimAsString("username");
                if (role != null) request.setAttribute("role", role);
                // Prefer the embedded display name; fall back to sub (userId) for old tokens.
                String identity = displayName != null ? displayName : sub;
                if (identity != null) request.setAttribute("username", identity);
            }
            chain.doFilter(request, response);
        }
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // JWT tokens issued by JwtTokenProvider contain a single "role" claim (e.g. "ADMIN", "TESTER").
        // Must match the claim name exactly — "roles" (plural) would silently yield no authorities.
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
