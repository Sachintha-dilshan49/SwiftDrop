package lk.swiftdrop.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Full chat path with a fake OpenAI-compatible LLM (WireMock) and a fake gateway (WireMock):
 * the model asks for track_parcel, the tool calls the gateway with the user's token, and the
 * model answers from the tool result.
 */
@SpringBootTest(properties = "swiftdrop.ai.knowledge.enabled=false")
@AutoConfigureMockMvc
class ChatToolCallingIT {

    private static final String USER_TOKEN = "customer-jwt";
    private static final WireMockServer llm = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer gateway = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        llm.start();
        gateway.start();
    }

    @DynamicPropertySource
    static void endpoints(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.gateway.base-url", gateway::baseUrl);
        registry.add("swiftdrop.ai.models.groq.base-url", () -> llm.baseUrl() + "/v1");
        registry.add("swiftdrop.ai.models.groq.api-key", () -> "test-llm-key");
        registry.add("swiftdrop.ai.timeout", () -> "3s");
    }

    @AfterAll
    static void stop() {
        llm.stop();
        gateway.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() {
        llm.resetAll();
        gateway.resetAll();
    }

    @Test
    void modelCallsTrackParcelAndAnswersFromResult() throws Exception {
        gateway.stubFor(get("/api/v1/track/SD-7K3Q-9P2M").willReturn(okJson("""
                {"trackingNo":"SD-7K3Q-9P2M","status":"OUT_FOR_DELIVERY","lastUpdatedAt":"2026-11-01T09:15:00Z",
                 "recipientPhone":"0771234567","history":[{"status":"OUT_FOR_DELIVERY","at":"2026-11-01T09:15:00Z"}]}""")));
        // Second LLM call carries the tool result: answer. First call: ask for the tool.
        llm.stubFor(post("/v1/chat/completions").atPriority(1)
                .withRequestBody(containing("tool_call_id"))
                .willReturn(okJson(completion("Your parcel SD-7K3Q-9P2M is out for delivery."))));
        llm.stubFor(post("/v1/chat/completions").atPriority(2)
                .willReturn(okJson(toolCall("track_parcel", "{\\\"trackingNo\\\":\\\"SD-7K3Q-9P2M\\\"}"))));

        mockMvc.perform(chat("{\"message\":\"Where is SD-7K3Q-9P2M?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Your parcel SD-7K3Q-9P2M is out for delivery."))
                .andExpect(jsonPath("$.model").value("groq"))
                .andExpect(jsonPath("$.toolsUsed[0]").value("track_parcel"))
                .andExpect(jsonPath("$.conversationId").isNotEmpty());

        gateway.verify(getRequestedFor(urlPathEqualTo("/api/v1/track/SD-7K3Q-9P2M"))
                .withHeader("Authorization", equalTo("Bearer " + USER_TOKEN)));
        llm.verify(postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-llm-key")));
        List<LoggedRequest> llmCalls = llm.findAll(postRequestedFor(urlPathEqualTo("/v1/chat/completions")));
        assertThat(llmCalls).hasSize(2);
        String toolResultCall = llmCalls.get(1).getBodyAsString();
        assertThat(toolResultCall).contains("OUT_FOR_DELIVERY").doesNotContain("0771234567");
        // Groq answers 400 if the model's private reasoning is sent back on the next turn
        assertThat(toolResultCall).doesNotContain("reasoning_content");
        assertThat(llmCalls.getFirst().getBodyAsString())
                .contains("track_parcel", "estimate_fee", "my_parcels")
                .doesNotContain(USER_TOKEN);
    }

    @Test
    void secondMessageInConversationSendsHistory() throws Exception {
        llm.stubFor(post("/v1/chat/completions").willReturn(okJson(completion("Hello! How can I help?"))));
        MvcResult first = mockMvc.perform(chat("{\"message\":\"My first question\"}"))
                .andExpect(status().isOk()).andReturn();
        String conversationId = JsonPath.read(first.getResponse().getContentAsString(),
                "$.conversationId");

        mockMvc.perform(chat("{\"message\":\"My second question\",\"conversationId\":\"" + conversationId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId));

        List<LoggedRequest> llmCalls = llm.findAll(postRequestedFor(urlPathEqualTo("/v1/chat/completions")));
        assertThat(llmCalls.get(1).getBodyAsString()).contains("My first question", "My second question");
    }

    @Test
    void otherUserCannotContinueSomeoneElsesConversation() throws Exception {
        llm.stubFor(post("/v1/chat/completions").willReturn(okJson(completion("ok"))));
        MvcResult first = mockMvc.perform(chat("{\"message\":\"Secret question\"}")).andReturn();
        String conversationId = JsonPath.read(first.getResponse().getContentAsString(),
                "$.conversationId");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/assistant/chat")
                        .with(caller("another-user", "other-jwt"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What did I ask?\",\"conversationId\":\"" + conversationId + "\"}"))
                .andExpect(status().isOk());

        llm.verify(postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("What did I ask?"))
                .withRequestBody(notContaining("Secret question")));
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assistant/conversations/{id}", conversationId)
                        .with(caller("another-user", "other-jwt")))
                .andExpect(status().isNoContent());
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/assistant/conversations/{id}",
                        "11111111-2222-3333-4444-555555555555")
                        .with(caller("another-user", "other-jwt")))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerErrorReturns503WithoutDetails() throws Exception {
        llm.stubFor(post("/v1/chat/completions")
                .willReturn(aResponse().withStatus(500).withBody("{\"error\":{\"message\":\"internal llm detail\"}}")));

        mockMvc.perform(chat("{\"message\":\"hi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(not(containsString("internal llm detail"))));
    }

    @Test
    void streamSendsTokensThenDoneEvent() throws Exception {
        llm.stubFor(post("/v1/chat/completions").willReturn(aResponse()
                .withHeader("Content-Type", "text/event-stream")
                .withBody(chunk("{\"role\":\"assistant\",\"content\":\"Hello\"}", null)
                        + chunk("{\"content\":\" there\"}", null)
                        + chunk("{}", "\"stop\"")
                        + "data: [DONE]\n\n")));

        MvcResult started = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/assistant/chat/stream")
                        .with(caller("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44", USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("event:token", "\"text\":\"Hello\"", "\"text\":\" there\"", "event:done",
                "\"model\":\"groq\"");
        assertThat(body.indexOf("event:done")).isGreaterThan(body.indexOf("there"));
    }

    @Test
    void streamProviderFailureBecomesErrorEvent() throws Exception {
        llm.stubFor(post("/v1/chat/completions").willReturn(aResponse().withStatus(500)));

        MvcResult started = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/assistant/chat/stream")
                        .with(caller("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44", USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = mockMvc.perform(asyncDispatch(started)).andReturn().getResponse().getContentAsString();

        assertThat(body).contains("event:error", "could not answer").doesNotContain("event:done");
    }

    @Test
    void streamWithUnknownModelFailsBeforeStreaming() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/assistant/chat/stream")
                        .with(caller("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44", USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_PROBLEM_JSON)
                        .content("{\"message\":\"hi\",\"model\":\"unknown\"}"))
                .andExpect(status().isBadRequest());
    }

    private static String chunk(String delta, String finishReason) {
        return "data: {\"id\":\"c-1\",\"object\":\"chat.completion.chunk\",\"created\":1730000000,"
                + "\"model\":\"test-model\",\"choices\":[{\"index\":0,\"delta\":" + delta
                + ",\"finish_reason\":" + finishReason + "}]}\n\n";
    }

    private static RequestBuilder chat(String body) {
        return MockMvcRequestBuilders.post("/api/v1/assistant/chat")
                .with(caller("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44", USER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static RequestPostProcessor caller(String subject, String tokenValue) {
        return jwt().jwt(token -> token.subject(subject).tokenValue(tokenValue))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static String completion(String text) {
        return """
                {"id":"c-1","object":"chat.completion","created":1730000000,"model":"test-model",
                 "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":"%s"}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}""".formatted(text);
    }

    private static String toolCall(String name, String escapedArguments) {
        return """
                {"id":"c-0","object":"chat.completion","created":1730000000,"model":"test-model",
                 "choices":[{"index":0,"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                   "reasoning":"Need to use the tool.",
                   "tool_calls":[{"id":"call_1","type":"function",
                     "function":{"name":"%s","arguments":"%s"}}]}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}""".formatted(name, escapedArguments);
    }
}
