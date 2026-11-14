package lk.swiftdrop.ai.chat.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.ai.chat.dto.ChatRequest;
import lk.swiftdrop.ai.chat.dto.ChatResponse;
import lk.swiftdrop.ai.chat.service.AssistantService;
import lk.swiftdrop.ai.model.ChatModelRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/assistant")
@PreAuthorize(AssistantController.ANY_SWIFTDROP_ROLE)
public class AssistantController {

    public static final String ANY_SWIFTDROP_ROLE =
            "hasAnyRole('CUSTOMER', 'MERCHANT', 'COURIER', 'DISPATCHER', 'ADMIN')";

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, @AuthenticationPrincipal Jwt caller) {
        return assistantService.chat(request, caller);
    }

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chatStream(@Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt caller) {
        return assistantService.stream(request, caller);
    }

    @GetMapping("/models")
    public List<ChatModelRegistry.ModelSummary> models() {
        return assistantService.models();
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable UUID conversationId, @AuthenticationPrincipal Jwt caller) {
        assistantService.deleteConversation(conversationId, caller);
    }
}
