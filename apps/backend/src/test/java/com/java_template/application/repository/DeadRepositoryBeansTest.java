package com.java_template.application.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IM-04: Verifies that in-memory repository classes are not registered as Spring beans.
 * These classes were dead code — not injected by any service — and should not
 * consume heap or appear in the application context.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeadRepositoryBeansTest {

    @Autowired
    private ApplicationContext ctx;

    @Test
    void testRunRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("testRunRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void suiteRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("suiteRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void projectRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("projectRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void testCaseRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("testCaseRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void testRunCaseRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("testRunCaseRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void testRunStepRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("testRunStepRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void attachmentRepositoryIsNotABean() {
        assertThatThrownBy(() -> ctx.getBean("attachmentRepository"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
