package lk.swiftdrop.ai.knowledge;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Help articles are loaded at startup, retrieved for chat messages (RAG) and searchable as a tool.
 * Uses a word-hash embedding instead of the ONNX model, so the threshold is lower than in production.
 */
@SpringBootTest(properties = {KnowledgeTestConfig.PROPERTY, "swiftdrop.ai.knowledge.similarity-threshold=0.15"})
@AutoConfigureMockMvc
@Import(KnowledgeTestConfig.class)
class KnowledgeIT {

    private static final WireMockServer llm = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        llm.start();
    }

    @DynamicPropertySource
    static void endpoints(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.ai.models.groq.base-url", () -> llm.baseUrl() + "/v1");
        registry.add("swiftdrop.ai.models.groq.api-key", () -> "test-llm-key");
    }

    @AfterAll
    static void stop() {
        llm.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HelpArticleTools helpArticleTools;

    @BeforeEach
    void reset() {
        llm.resetAll();
    }

    @Test
    void everyArticleIsLoadedAndSearchable() {
        Map<String, String> keywords = Map.of(
                "fees-explained", "promo code discount",
                "tracking-status-guide", "public tracking page live",
                "shipping-rules", "packaging padding",
                "delivery-times-and-zones", "zones route",
                "failed-delivery", "avoiding failed deliveries",
                "prohibited-items", "weapons explosives",
                "merchant-api-faq", "webhook signature");
        keywords.forEach((article, query) -> assertThat(search(query))
                .as(article).extracting(HelpArticleTools.HelpPassage::article).contains(article));
    }

    @Test
    void helpQuestionFindsProhibitedItemsArticle() {
        List<HelpArticleTools.HelpPassage> passages = search("Can I send perfume?");

        assertThat(passages).isNotEmpty();
        assertThat(passages.getFirst().article()).isEqualTo("prohibited-items");
        assertThat(passages).anyMatch(passage -> passage.text().contains("Perfume"));
    }

    @Test
    void helpQuestionInChatAddsPassagesAndReturnsSources() throws Exception {
        llm.stubFor(post("/v1/chat/completions").willReturn(okJson("""
                {"id":"c-1","object":"chat.completion","created":1730000000,"model":"test-model",
                 "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant",
                   "content":"No, perfume is flammable, so SwiftDrop does not accept it."}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}""")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/assistant/chat")
                        .with(jwt().jwt(token -> token.subject("user-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Can I send perfume?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].article").value("prohibited-items"))
                .andExpect(jsonPath("$.toolsUsed").isEmpty());

        String llmRequest = llm.findAll(postRequestedFor(urlPathEqualTo("/v1/chat/completions")))
                .getFirst().getBodyAsString();
        assertThat(llmRequest).contains("alcohol-based sprays", "help-centre passages");
        // RAG is an advisor in chat, not a tool the chat model can call
        assertThat(llmRequest).doesNotContain(HelpArticleTools.SEARCH_HELP_ARTICLES);
    }

    @Test
    void searchRejectsEmptyQuery() {
        assertThatThrownBy(() -> search(" "))
                .hasMessageContaining("question or keywords");
    }

    private List<HelpArticleTools.HelpPassage> search(String query) {
        return helpArticleTools.searchHelpArticles(query, new ToolContext(Map.of()));
    }
}
