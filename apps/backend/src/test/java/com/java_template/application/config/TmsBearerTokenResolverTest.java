package com.java_template.application.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TmsBearerTokenResolverTest {

    private final TmsBearerTokenResolver resolver = new TmsBearerTokenResolver();

    @Test
    void extractsTokenFromBearerHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer my-token-value");

        assertThat(resolver.resolve(request)).isEqualTo("my-token-value");
    }

    @Test
    void extractsTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("auth-token", "cookie-token-value"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token-value");
    }

    @Test
    void prefersCookieOverBearerHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie("auth-token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void returnsNullWhenNoTokenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void returnsNullForNonBearerAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertThat(resolver.resolve(request)).isNull();
    }
}
