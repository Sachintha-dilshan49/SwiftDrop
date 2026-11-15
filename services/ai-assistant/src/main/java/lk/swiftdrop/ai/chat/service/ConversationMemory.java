package lk.swiftdrop.ai.chat.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Conversation memory in this service's heap (the service has no database). Conversations are
 * keyed by {@code userId:conversationId}, so a user can never load or delete another user's
 * conversation by guessing its id. Idle conversations are dropped, which also bounds memory use.
 */
public class ConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemory.class);

    private final ChatMemory chatMemory;
    private final Duration idleTimeout;
    private final Clock clock;
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();

    public ConversationMemory(ChatMemory chatMemory, Duration idleTimeout, Clock clock) {
        this.chatMemory = chatMemory;
        this.idleTimeout = idleTimeout;
        this.clock = clock;
    }

    public static String key(String userId, UUID conversationId) {
        return userId + ":" + conversationId;
    }

    /** Marks the conversation as used now and returns its memory key. */
    public String touch(String userId, UUID conversationId) {
        String key = key(userId, conversationId);
        lastActivity.put(key, clock.instant());
        return key;
    }

    /** @return false if the user has no such conversation */
    public boolean delete(String userId, UUID conversationId) {
        String key = key(userId, conversationId);
        boolean existed = lastActivity.remove(key) != null || !chatMemory.get(key).isEmpty();
        chatMemory.clear(key);
        return existed;
    }

    @Scheduled(fixedDelayString = "${swiftdrop.ai.memory.cleanup-interval:5m}")
    public void evictIdle() {
        Instant cutoff = clock.instant().minus(idleTimeout);
        int[] evicted = {0};
        lastActivity.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                chatMemory.clear(entry.getKey());
                evicted[0]++;
                return true;
            }
            return false;
        });
        if (evicted[0] > 0) {
            log.debug("Evicted {} idle conversations", evicted[0]);
        }
    }

    public ChatMemory chatMemory() {
        return chatMemory;
    }
}
