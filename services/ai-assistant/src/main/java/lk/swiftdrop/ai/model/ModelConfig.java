package lk.swiftdrop.ai.model;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiModelProperties.class)
class ModelConfig {
}
