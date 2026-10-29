package lk.swiftdrop.common.internal;

import java.util.List;
import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Test-only: gives every Spring test context an internal token when none is configured, so
 * {@code @SpringBootTest} and {@code @WebMvcTest} start without {@code INTERNAL_API_TOKEN}.
 * Registered in {@code META-INF/spring.factories}, which only spring-test reads; a running
 * service never sees it and still fails fast without the token.
 *
 * <p>Call your own {@code /internal/**} endpoints in tests with
 * {@code header("X-Internal-Token", InternalTokenTestContextCustomizerFactory.TEST_TOKEN)}.
 * A value from the environment or {@code @SpringBootTest(properties = ...)} takes precedence.
 */
public class InternalTokenTestContextCustomizerFactory implements ContextCustomizerFactory {

    public static final String TEST_TOKEN = "swiftdrop-test-internal-token";
    static final String PROPERTY = "swiftdrop.internal.token";
    static final String PROPERTY_SOURCE = "swiftdropInternalTestToken";

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return new TestTokenCustomizer();
    }

    static final class TestTokenCustomizer implements ContextCustomizer {

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
            ConfigurableEnvironment environment = context.getEnvironment();
            if (isMissing(environment)) {
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE,
                        Map.of(PROPERTY, TEST_TOKEN)));
            }
        }

        private static boolean isMissing(ConfigurableEnvironment environment) {
            try {
                String value = environment.getProperty(PROPERTY);
                return value == null || value.isBlank();
            } catch (IllegalArgumentException unresolvablePlaceholder) {
                return true;
            }
        }

        // One shared customizer keeps Spring's test context cache effective
        @Override
        public boolean equals(Object other) {
            return other instanceof TestTokenCustomizer;
        }

        @Override
        public int hashCode() {
            return TestTokenCustomizer.class.hashCode();
        }
    }
}
