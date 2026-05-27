package com.java_template.application.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // 34 chars — above the 32-byte minimum required for HS256
    private static final String SECRET = "test-signing-secret-32-chars-ok!!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET);
    }

    @Test
    void generatedTokenHasJwtFormat() {
        String token = provider.generateToken("alice", "ADMIN");

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validatesGeneratedToken() {
        String token = provider.generateToken("alice", "ADMIN");

        assertThat(provider.validateToken(token)).isTrue();
    }

    // ---- CR-01 core security test ----
    @Test
    void rejectsForgedBase64Token() {
        // This reproduces the exact attack: crafting a "token" by base64-encoding
        // username|role|issuedAt|expiresAt without knowing the signing secret.
        long now = System.currentTimeMillis();
        String payload = "alice|ADMIN|" + now + "|" + (now + 86_400_000);
        String forged = Base64.getEncoder().encodeToString(payload.getBytes());

        assertThat(provider.validateToken(forged)).isFalse();
    }

    @Test
    void rejectsTamperedPayload() {
        String token = provider.generateToken("alice", "TESTER");

        // Replace the payload part with an ADMIN role claim, keep original header+signature
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"alice\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThat(provider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("alice")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(key)
                .compact();

        assertThat(provider.validateToken(expired)).isFalse();
    }

    @Test
    void extractsUsernameFromValidToken() {
        String token = provider.generateToken("bob", "TESTER");

        assertThat(provider.getUsernameFromToken(token)).isEqualTo("bob");
    }

    @Test
    void extractsRoleFromValidToken() {
        String token = provider.generateToken("bob", "TESTER");

        assertThat(provider.getRoleFromToken(token)).isEqualTo("TESTER");
    }

    @Test
    void returnsNullUsernameForInvalidToken() {
        assertThat(provider.getUsernameFromToken("not.a.valid.token")).isNull();
    }

    @Test
    void returnsNullRoleForInvalidToken() {
        assertThat(provider.getRoleFromToken("not.a.valid.token")).isNull();
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtTokenProvider otherProvider = new JwtTokenProvider("completely-different-secret-xyz!!");
        String tokenFromOther = otherProvider.generateToken("alice", "ADMIN");

        assertThat(provider.validateToken(tokenFromOther)).isFalse();
    }

    // ---- algorithm allow-list tests (S2) ----

    @Test
    void rejectsTokenSignedWithRsaAlgorithm() throws Exception {
        // Algorithm-confusion attack: an RSA-signed token must be rejected by an HMAC verifier.
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String rsaToken = Jwts.builder()
                .subject("alice")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 86_400_000))
                .signWith(pair.getPrivate())
                .compact();

        assertThat(provider.validateToken(rsaToken)).isFalse();
    }

    @Test
    void generateToken_withDisplayName_storesDisplayNameAsClaimNotSubject() {
        // When a 3-arg token is generated, getDisplayNameFromToken must return the display
        // name while getUsernameFromToken still returns the userId (JWT sub).
        String token = provider.generateToken("user-uuid-123", "alice", "ADMIN");

        assertThat(provider.getDisplayNameFromToken(token)).isEqualTo("alice");
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("user-uuid-123");
    }

    @Test
    void rejectsUnsignedAlgNoneToken() {
        // Craft a token with alg=none in the header — must always be rejected.
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"alice\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
        String algNoneToken = header + "." + payload + ".";

        assertThat(provider.validateToken(algNoneToken)).isFalse();
    }
}
