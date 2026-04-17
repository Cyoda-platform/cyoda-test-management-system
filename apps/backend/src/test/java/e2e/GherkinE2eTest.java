package e2e;

import com.java_template.Application;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@DirtiesContext
@CucumberContextConfiguration
@SpringBootTest(classes = { Application.class, E2eTestConfig.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "e2e")
public class GherkinE2eTest {

    /** Exposed so step-definition classes can build http://localhost:{port}/api/... URLs. */
    @LocalServerPort
    public int serverPort;
}
