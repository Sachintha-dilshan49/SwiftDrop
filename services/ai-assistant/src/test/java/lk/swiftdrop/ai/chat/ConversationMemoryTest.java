package lk.swiftdrop.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import lk.swiftdrop.ai.chat.service.ConversationMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationMemoryTest {

    private static final UUID CONVERSATION = UUID.randomUUID();

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-02T08:00:00Z"));
    private ChatMemory chatMemory;
    private ConversationMemory memory;

    @BeforeEach
    void setUp() {
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        memory = new ConversationMemory(chatMemory, Duration.ofMinutes(30), clock);
    }

    @Test
    void keyIsScopedToUser() {
        assertThat(memory.touch("user-a", CONVERSATION)).isEqualTo("user-a:" + CONVERSATION);
        assertThat(memory.touch("user-b", CONVERSATION)).isNotEqualTo(memory.touch("user-a", CONVERSATION));
    }

    @Test
    void idleConversationIsEvictedAfterTimeout() {
        String key = memory.touch("user-a", CONVERSATION);
        chatMemory.add(key, new UserMessage("Where is my parcel?"));

        clock.advance(Duration.ofMinutes(29));
        memory.evictIdle();
        assertThat(chatMemory.get(key)).hasSize(1);

        clock.advance(Duration.ofMinutes(2));
        memory.evictIdle();
        assertThat(chatMemory.get(key)).isEmpty();
    }

    @Test
    void activeConversationIsKept() {
        String key = memory.touch("user-a", CONVERSATION);
        chatMemory.add(key, new UserMessage("hi"));

        clock.advance(Duration.ofMinutes(25));
        memory.touch("user-a", CONVERSATION);
        clock.advance(Duration.ofMinutes(25));
        memory.evictIdle();

        assertThat(chatMemory.get(key)).hasSize(1);
    }

    @Test
    void customerCannotDeleteAnotherUsersConversation() {
        String key = memory.touch("user-a", CONVERSATION);
        chatMemory.add(key, new UserMessage("hi"));

        assertThat(memory.delete("user-b", CONVERSATION)).isFalse();
        assertThat(chatMemory.get(key)).hasSize(1);
        assertThat(memory.delete("user-a", CONVERSATION)).isTrue();
        assertThat(chatMemory.get(key)).isEmpty();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
