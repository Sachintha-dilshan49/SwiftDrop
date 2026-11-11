package lk.swiftdrop.ai.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import java.util.Map;
import lk.swiftdrop.ai.tools.SwiftDropTools;
import lk.swiftdrop.ai.tools.ToolContextKeys;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * MCP server on {@code /mcp} (stateless Streamable HTTP, see {@code spring.ai.mcp.server.*}).
 *
 * <p>The endpoint is protected by the common JWT filter chain like any other path. The tools call
 * the gateway as the caller, so the transport copies the request's {@code Authorization} header into
 * the MCP transport context, where {@code ToolCaller} reads it.
 */
@Configuration(proxyBeanMethods = false)
public class McpServerConfig {

    /** Replaces Spring AI's default transport, which has no context extractor. */
    @Bean
    WebMvcStatelessServerTransport webMvcStatelessServerTransport(JsonMapper jsonMapper,
            McpServerStreamableHttpProperties properties) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint(properties.getMcpEndpoint())
                .contextExtractor(McpServerConfig::extractAuthorization)
                .build();
    }

    @Bean
    ToolCallbackProvider swiftDropToolCallbacks(SwiftDropTools swiftDropTools) {
        return MethodToolCallbackProvider.builder().toolObjects(swiftDropTools).build();
    }

    static McpTransportContext extractAuthorization(ServerRequest request) {
        String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            return McpTransportContext.EMPTY;
        }
        return McpTransportContext.create(Map.of(ToolContextKeys.MCP_AUTHORIZATION, authorization));
    }
}
