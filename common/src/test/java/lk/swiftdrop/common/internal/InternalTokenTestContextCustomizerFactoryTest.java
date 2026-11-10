package lk.swiftdrop.common.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextCustomizer;

class InternalTokenTestContextCustomizerFactoryTest {

    private final ContextCustomizer customizer =
            new InternalTokenTestContextCustomizerFactory().createContextCustomizer(getClass(), List.of());

    @Test
    void suppliesTestTokenWhenNoneIsConfigured() {
        assertThat(tokenAfterCustomizing(Map.of())).isEqualTo(InternalTokenTestContextCustomizerFactory.TEST_TOKEN);
    }

    @Test
    void suppliesTestTokenWhenTheEnvironmentVariableIsMissing() {
        // What application.yml's ${INTERNAL_API_TOKEN:} resolves to, and the form without a default
        assertThat(tokenAfterCustomizing(Map.of("swiftdrop.internal.token", "${INTERNAL_API_TOKEN:}")))
                .isEqualTo(InternalTokenTestContextCustomizerFactory.TEST_TOKEN);
        assertThat(tokenAfterCustomizing(Map.of("swiftdrop.internal.token", "${INTERNAL_API_TOKEN}")))
                .isEqualTo(InternalTokenTestContextCustomizerFactory.TEST_TOKEN);
    }

    @Test
    void keepsAConfiguredToken() {
        assertThat(tokenAfterCustomizing(Map.of("swiftdrop.internal.token", "from-env"))).isEqualTo("from-env");
    }

    @Test
    void customizersAreEqualSoTestContextsAreCached() {
        ContextCustomizer other = new InternalTokenTestContextCustomizerFactory().createContextCustomizer(String.class,
                List.of());
        assertThat(customizer).isEqualTo(other).hasSameHashCodeAs(other);
    }

    private String tokenAfterCustomizing(Map<String, Object> properties) {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addLast(new MapPropertySource("application", properties));
            customizer.customizeContext(context, null);
            return context.getEnvironment().getProperty("swiftdrop.internal.token");
        }
    }
}
