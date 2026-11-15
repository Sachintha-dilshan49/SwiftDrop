package lk.swiftdrop.ai.chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lk.swiftdrop.ai.chat.dto.ChatRequest;
import lk.swiftdrop.ai.chat.dto.ChatResponse;
import lk.swiftdrop.ai.chat.dto.SourceRef;
import lk.swiftdrop.ai.knowledge.KnowledgeMetadata;
import lk.swiftdrop.ai.model.AssistantUnavailableException;
import lk.swiftdrop.ai.model.ChatModelRegistry;
import lk.swiftdrop.ai.tools.SwiftDropTools;
import lk.swiftdrop.ai.tools.ToolCallTracker;
import lk.swiftdrop.ai.tools.ToolContextKeys;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.SwiftDropException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Answers chat messages: conversation memory, help-article retrieval (RAG) and the SwiftDrop tools,
 * on the model the request names (or the default).
 *
 * <p>Prompts and replies are never logged: they can contain addresses and phone numbers. Only the
 * model, the tools used and the time taken are logged.
 */
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    static final String PROVIDER_FAILED =
            "The AI assistant could not answer right now. Please try again in a moment.";

    private final ChatModelRegistry models;
    private final ConversationMemory memory;
    private final SwiftDropTools tools;
    private final List<Advisor> retrievalAdvisors;
    private final Map<String, ChatClient> clients = new ConcurrentHashMap<>();

    /**
     * @param retrievalAdvisors RAG advisors added to every chat; empty if the knowledge base is off
     */
    public AssistantService(ChatModelRegistry models, ConversationMemory memory, SwiftDropTools tools,
            List<Advisor> retrievalAdvisors) {
        this.models = models;
        this.memory = memory;
        this.tools = tools;
        this.retrievalAdvisors = List.copyOf(retrievalAdvisors);
    }

    public ChatResponse chat(ChatRequest request, Jwt caller) {
        ChatTurn turn = prepare(request, caller);
        long started = System.nanoTime();
        ChatClientResponse response;
        try {
            response = turn.prompt().call().chatClientResponse();
        } catch (SwiftDropException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw providerFailed(turn.model(), ex);
        }
        String reply = response.chatResponse() == null || response.chatResponse().getResult() == null
                ? "" : response.chatResponse().getResult().getOutput().getText();
        log.info("Chat answered: model={} tools={} durationMs={}", turn.model(), turn.tracker().toolsUsed(),
                (System.nanoTime() - started) / 1_000_000);
        return new ChatResponse(turn.conversationId(), turn.model(), reply == null ? "" : reply,
                turn.tracker().toolsUsed(), sources(response.context()));
    }

    /**
     * Streams the reply as server-sent events: {@code token} events with {@code {"text": ...}}, then one
     * {@code done} event with the same fields as {@link ChatResponse} except the reply. A failure after
     * the stream has started becomes an {@code error} event, because the status code is already sent.
     * Invalid requests and unknown models fail before streaming, as a normal ProblemDetail response.
     */
    public Flux<ServerSentEvent<Object>> stream(ChatRequest request, Jwt caller) {
        ChatTurn turn = prepare(request, caller);
        AtomicReference<List<SourceRef>> sources = new AtomicReference<>(List.of());
        long started = System.nanoTime();

        Flux<ServerSentEvent<Object>> tokens = turn.prompt().stream().chatClientResponse()
                .doOnNext(response -> {
                    List<SourceRef> found = sources(response.context());
                    if (!found.isEmpty()) {
                        sources.set(found);
                    }
                })
                .mapNotNull(AssistantService::text)
                .filter(text -> !text.isEmpty())
                .map(text -> event(StreamEvents.TOKEN, new StreamEvents.Token(text)));

        Mono<ServerSentEvent<Object>> done = Mono.fromSupplier(() -> {
            log.info("Chat streamed: model={} tools={} durationMs={}", turn.model(), turn.tracker().toolsUsed(),
                    (System.nanoTime() - started) / 1_000_000);
            return event(StreamEvents.DONE, new StreamEvents.Done(turn.conversationId(), turn.model(),
                    turn.tracker().toolsUsed(), sources.get()));
        });

        return tokens.concatWith(done)
                .onErrorResume(ex -> {
                    String detail = ex instanceof SwiftDropException known ? known.getMessage()
                            : providerFailed(turn.model(), ex).getMessage();
                    return Flux.just(event(StreamEvents.ERROR, new StreamEvents.Error(detail)));
                });
    }

    /** Clears one of the caller's conversations. */
    public void deleteConversation(UUID conversationId, Jwt caller) {
        if (!memory.delete(caller.getSubject(), conversationId)) {
            throw new NotFoundException("No conversation with that id.");
        }
    }

    public List<ChatModelRegistry.ModelSummary> models() {
        return models.summaries();
    }

    /** Resolves the model and builds the request; shared by the blocking and streaming endpoints. */
    ChatTurn prepare(ChatRequest request, Jwt caller) {
        ChatModelRegistry.ResolvedModel model = models.resolve(request.model());
        UUID conversationId = request.conversationId() == null ? UUID.randomUUID() : request.conversationId();
        String memoryKey = memory.touch(caller.getSubject(), conversationId);
        ToolCallTracker tracker = new ToolCallTracker();

        ChatClient.ChatClientRequestSpec prompt = client(model)
                .prompt()
                .user(request.message())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, memoryKey))
                .toolContext(Map.of(
                        ToolContextKeys.BEARER_TOKEN, caller.getTokenValue(),
                        ToolContextKeys.TRACKER, tracker));
        return new ChatTurn(model.name(), conversationId, tracker, prompt);
    }

    AssistantUnavailableException providerFailed(String model, Throwable ex) {
        // The provider's message may echo the prompt, so only the type is logged
        log.warn("Chat failed: model={} error={}", model, ex.getClass().getSimpleName());
        return new AssistantUnavailableException(PROVIDER_FAILED);
    }

    private static String text(ChatClientResponse response) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null) {
            return null;
        }
        return response.chatResponse().getResult().getOutput().getText();
    }

    private static ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder(data).event(name).build();
    }

    static List<SourceRef> sources(Map<String, Object> context) {
        if (context != null && context.get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT) instanceof List<?> list) {
            return KnowledgeMetadata.sources(list.stream()
                    .filter(Document.class::isInstance)
                    .map(Document.class::cast)
                    .toList());
        }
        return List.of();
    }

    private ChatClient client(ChatModelRegistry.ResolvedModel model) {
        return clients.computeIfAbsent(model.name(), name -> ChatClient.builder(model.chatModel())
                .defaultSystem(SystemPrompts.ASSISTANT)
                .defaultAdvisors(advisors())
                .defaultTools(tools)
                .build());
    }

    private List<Advisor> advisors() {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(MessageChatMemoryAdvisor.builder(memory.chatMemory()).build());
        advisors.addAll(retrievalAdvisors);
        return advisors;
    }

    /** One prepared chat request. */
    record ChatTurn(String model, UUID conversationId, ToolCallTracker tracker,
            ChatClient.ChatClientRequestSpec prompt) {
    }
}
