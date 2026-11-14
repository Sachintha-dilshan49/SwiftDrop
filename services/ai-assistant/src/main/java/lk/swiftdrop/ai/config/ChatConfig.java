package lk.swiftdrop.ai.config;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lk.swiftdrop.ai.chat.service.AssistantService;
import lk.swiftdrop.ai.chat.service.ConversationMemory;
import lk.swiftdrop.ai.model.ChatModelRegistry;
import lk.swiftdrop.ai.tools.SwiftDropTools;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ChatConfig {

    @Bean
    ConversationMemory conversationMemory(
            @Value("${swiftdrop.ai.memory.max-messages:20}") int maxMessages,
            @Value("${swiftdrop.ai.memory.idle-timeout:30m}") Duration idleTimeout) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(maxMessages)
                .build();
        return new ConversationMemory(chatMemory, idleTimeout, Clock.systemUTC());
    }

    @Bean
    AssistantService assistantService(ChatModelRegistry models, ConversationMemory memory, SwiftDropTools tools,
            ObjectProvider<RetrievalAugmentationAdvisor> retrievalAdvisors) {
        List<Advisor> advisors = retrievalAdvisors.orderedStream().<Advisor>map(advisor -> advisor).toList();
        return new AssistantService(models, memory, tools, advisors);
    }
}
