package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolDefinition;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.ModelResponseType;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class DeepSeekModelGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsFinalOutputWhenDeepSeekRespondsWithContent() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new DeepSeekModelGateway(
                new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                new AgentRuntimeProperties(false),
                builder,
                objectMapper);

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"deepseek-chat\"")))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "{\\"answer\\":\\"hello\\"}"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var response = gateway.generate(request(List.of()));

        assertThat(response.responseType()).isEqualTo(ModelResponseType.FINAL_OUTPUT);
        assertThat(response.finalOutputJson()).isEqualTo("{\"answer\":\"hello\"}");
        server.verify();
    }

    @Test
    void returnsSingleToolCallWhenDeepSeekRespondsWithFunctionCall() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new DeepSeekModelGateway(
                new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                new AgentRuntimeProperties(false),
                builder,
                objectMapper);

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [
                                  {
                                    "id": "call-1",
                                    "type": "function",
                                    "function": {
                                      "name": "lookup",
                                      "arguments": "{\\"query\\":\\"hanzi\\"}"
                                    }
                                  }
                                ]
                              },
                              "finish_reason": "tool_calls"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var response = gateway.generate(request(List.of(new AgentToolDefinition(
                "lookup",
                "Lookup a value",
                "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}"))));

        assertThat(response.responseType()).isEqualTo(ModelResponseType.TOOL_CALL);
        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.toolCalls().getFirst().toolCallId()).isEqualTo("call-1");
        assertThat(response.toolCalls().getFirst().toolName()).isEqualTo("lookup");
        assertThat(response.toolCalls().getFirst().argumentsJson()).isEqualTo("{\"query\":\"hanzi\"}");
        server.verify();
    }

    @Test
    void returnsMultipleToolCallsWhenDeepSeekRespondsWithSeveralFunctions() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new DeepSeekModelGateway(
                new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                new AgentRuntimeProperties(false),
                builder,
                objectMapper);

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [
                                  {
                                    "id": "call-1",
                                    "type": "function",
                                    "function": {
                                      "name": "lookup",
                                      "arguments": "{\\"query\\":\\"hanzi\\"}"
                                    }
                                  },
                                  {
                                    "id": "call-2",
                                    "type": "function",
                                    "function": {
                                      "name": "lookup",
                                      "arguments": "{\\"query\\":\\"pinyin\\"}"
                                    }
                                  }
                                ]
                              },
                              "finish_reason": "tool_calls"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var response = gateway.generate(request(List.of(new AgentToolDefinition(
                "lookup",
                "Lookup a value",
                "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}"))));

        assertThat(response.responseType()).isEqualTo(ModelResponseType.TOOL_CALL);
        assertThat(response.toolCalls()).hasSize(2);
        assertThat(response.toolCalls()).extracting(call -> call.toolCallId()).containsExactly("call-1", "call-2");
        server.verify();
    }

    @Test
    void surfacesHttpErrorsWithStatusCode() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new DeepSeekModelGateway(
                new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                new AgentRuntimeProperties(false),
                builder,
                objectMapper);

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"bad key\"}}"));

        assertThatThrownBy(() -> gateway.generate(request(List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("401");
        server.verify();
    }

    @Test
    void logsRequestAndResponseWhenModelIoLoggingEnabled() {
        var logger = (Logger) LoggerFactory.getLogger(DeepSeekModelGateway.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);
        var previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var gateway = new DeepSeekModelGateway(
                    new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                    new AgentRuntimeProperties(true),
                    builder,
                    objectMapper);

            server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                    .andRespond(withSuccess(
                            """
                            {
                              "choices": [
                                {
                                  "message": {
                                    "role": "assistant",
                                    "content": "{\\"logged\\":true}"
                                  },
                                  "finish_reason": "stop"
                                }
                              ]
                            }
                            """,
                            MediaType.APPLICATION_JSON));

            gateway.generate(request(List.of()));
            server.verify();

            var joined = listAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(joined).contains("DeepSeek model request");
            assertThat(joined).contains("\"model\":\"deepseek-chat\"");
            assertThat(joined).contains("DeepSeek model response");
            assertThat(joined).contains("\"choices\"");
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
            logger.setLevel(previousLevel);
        }
    }

    @Test
    void logsHttpErrorWhenModelIoLoggingEnabled() {
        var logger = (Logger) LoggerFactory.getLogger(DeepSeekModelGateway.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);
        var previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var gateway = new DeepSeekModelGateway(
                    new DeepSeekProperties("https://api.deepseek.com", "test-key"),
                    new AgentRuntimeProperties(true),
                    builder,
                    objectMapper);

            server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"invalid\"}"));

            assertThatThrownBy(() -> gateway.generate(request(List.of())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("400");
            server.verify();

            var joined = listAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(joined).contains("DeepSeek model request");
            assertThat(joined).contains("DeepSeek model error");
            assertThat(joined).contains("400");
            assertThat(joined).contains("invalid");
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
            logger.setLevel(previousLevel);
        }
    }

    private AgentModelRequest request(List<AgentToolDefinition> tools) {
        var profile = new AgentProfile(
                "assistant:v1",
                "Assistant v1",
                "Help the user",
                "default",
                tools.stream().map(AgentToolDefinition::name).toList(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of("answer", OutputFieldType.STRING)),
                false,
                true);
        var session = AgentSession.createNew(
                profile.profileKey(),
                "deepseek-chat",
                "Say hello",
                null,
                java.time.Instant.now());
        return new AgentModelRequest(
                new AgentModelDescriptor("deepseek-chat", "DeepSeek Chat", "deepseek", true),
                profile,
                session,
                List.of(AgentModelMessage.system("You are helpful"), AgentModelMessage.user("Task:\nSay hello")),
                tools);
    }
}
