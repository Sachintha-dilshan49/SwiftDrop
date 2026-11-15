package lk.swiftdrop.ai.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.ai.chat.controller.AssistantController;
import lk.swiftdrop.ai.chat.dto.ChatResponse;
import lk.swiftdrop.ai.chat.dto.SourceRef;
import lk.swiftdrop.ai.chat.service.AssistantService;
import lk.swiftdrop.ai.model.AssistantUnavailableException;
import lk.swiftdrop.ai.model.ChatModelRegistry;
import lk.swiftdrop.ai.model.UnknownModelException;
import lk.swiftdrop.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    private static final UUID CONVERSATION = UUID.fromString("0b6c2f8e-4a51-4d6b-9a3e-7c1d2e3f4a5b");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @Test
    void customerGetsReplyWithToolsAndSources() throws Exception {
        given(assistantService.chat(any(), any())).willReturn(new ChatResponse(CONVERSATION, "groq",
                "Your parcel is out for delivery.", List.of("track_parcel"),
                List.of(new SourceRef("tracking-status-guide", "Out for delivery"))));

        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Where is SD-7K3Q-9P2M?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION.toString()))
                .andExpect(jsonPath("$.model").value("groq"))
                .andExpect(jsonPath("$.toolsUsed[0]").value("track_parcel"))
                .andExpect(jsonPath("$.sources[0].article").value("tracking-status-guide"));
    }

    @Test
    void courierCanChatToo() throws Exception {
        given(assistantService.chat(any(), any()))
                .willReturn(new ChatResponse(CONVERSATION, "groq", "Hi", List.of(), List.of()));

        mockMvc.perform(post("/api/v1/assistant/chat").with(user("COURIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What does FAILED mean?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void blankMessageReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("message"));
        verifyNoInteractions(assistantService);
    }

    @Test
    void tooLongMessageReturns400() throws Exception {
        String message = "a".repeat(2001);
        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedModelNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"model\":\"https://evil.example\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("model"));
    }

    @Test
    void unknownModelNameReturns400() throws Exception {
        given(assistantService.chat(any(), any())).willThrow(new UnknownModelException("gpt-9"));

        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"model\":\"gpt-9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("model"));
    }

    @Test
    void providerFailureReturns503ProblemDetail() throws Exception {
        given(assistantService.chat(any(), any()))
                .willThrow(new AssistantUnavailableException("The AI assistant could not answer right now."));

        mockMvc.perform(post("/api/v1/assistant/chat").with(user("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Assistant unavailable"));
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(assistantService);
    }

    @Test
    void tokenWithoutSwiftDropRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assistantService);
    }

    @Test
    void modelsListsEnabledModels() throws Exception {
        given(assistantService.models()).willReturn(List.of(
                new ChatModelRegistry.ModelSummary("groq", "openai/gpt-oss-120b", true, true)));

        mockMvc.perform(get("/api/v1/assistant/models").with(user("MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("groq"))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void deleteConversationReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/assistant/conversations/{id}", CONVERSATION).with(user("CUSTOMER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingAnotherUsersConversationReturns404() throws Exception {
        willThrow(new NotFoundException("No conversation with that id."))
                .given(assistantService).deleteConversation(eq(CONVERSATION), any());

        mockMvc.perform(delete("/api/v1/assistant/conversations/{id}", CONVERSATION).with(user("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
