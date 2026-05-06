package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageContentPart;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.ModelResponseType;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class QwenModelGatewayTest {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsNoSupportedModelsWhenApiKeyIsMissing() {
        var gateway = new QwenModelGateway(
                new QwenProperties(BASE_URL, "", false),
                new AgentRuntimeProperties(false),
                RestClient.builder(),
                objectMapper);

        assertThat(gateway.supportedModels()).isEmpty();
    }

    @Test
    void sendsOpenAiCompatibleImageContentToQwen() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new QwenModelGateway(
                new QwenProperties(BASE_URL, "test-qwen-key", false),
                new AgentRuntimeProperties(false),
                builder,
                objectMapper);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-qwen-key"))
                .andExpect(content().string(containsString("\"model\":\"qwen3.6-plus\"")))
                .andExpect(content().string(containsString("\"type\":\"image_url\"")))
                .andExpect(content().string(containsString("\"url\":\"data:image/png;base64,AAAA\"")))
                .andExpect(content().string(containsString("\"type\":\"text\"")))
                .andExpect(content().string(containsString("\"enable_thinking\":false")))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "{\\"answer\\":\\"image understood\\"}"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var response = gateway.generate(request());

        assertThat(response.responseType()).isEqualTo(ModelResponseType.FINAL_OUTPUT);
        assertThat(response.finalOutputJson()).isEqualTo("{\"answer\":\"image understood\"}");
        server.verify();
    }

    private AgentModelRequest request() {
        var profile = new AgentProfile(
                "assistant:v1",
                "Assistant v1",
                "Help the user",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of("answer", OutputFieldType.STRING)),
                false,
                true);
        var session = AgentSession.createNew(
                profile.profileKey(),
                "qwen3.6-plus",
                "Describe the image",
                null,
                java.time.Instant.now());
        return new AgentModelRequest(
                new AgentModelDescriptor("qwen3.6-plus", "Qwen3.6 Plus", "qwen", true),
                profile,
                session,
                List.of(AgentModelMessage.user(List.of(
                        AgentModelMessageContentPart.imageUrl("data:image/png;base64,AAAA"),
                        AgentModelMessageContentPart.text("Describe this image as JSON")))),
                List.of());
    }
}
