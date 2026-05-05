package ru.chinesewithai.backend.agentruntime.infrastructure.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationState;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

class DefaultAgentContextBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultAgentContextBuilder builder = new DefaultAgentContextBuilder(objectMapper);

    @Test
    void appendsSessionPromptAppendixToSystemPrompt() {
        var profile = new AgentProfile(
                "lesson-generator:v1",
                "Lesson Generator v1",
                "Base system prompt",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of("title", OutputFieldType.STRING)),
                false,
                false);
        var session = AgentSession.createNew(
                profile.profileKey(),
                "fake-model",
                "Generate a lesson",
                "{\"draftId\":\"x\"}",
                "Module appendix",
                Instant.now());

        var messages = builder.buildContext(new AgentContextBuildRequest(profile, session, List.of()));

        assertThat(messages.getFirst().content()).contains("Base system prompt");
        assertThat(messages.getFirst().content()).contains("Module appendix");
    }

    @Test
    void rendersPreGeneratedSystemAndUserSections() {
        var profile = new AgentProfile(
                "assistant:v1",
                "Assistant v1",
                "Base system prompt",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of("answer", OutputFieldType.STRING)),
                false,
                true);
        var session = AgentSession.createNew(
                profile.profileKey(),
                "fake-model",
                "Answer the user",
                "{\"question\":\"hi\"}",
                Instant.now());
        var state = new PreGenerationState(
                List.of(
                        new PreGenerationContextSection(
                                PreGenerationContextSectionTarget.SYSTEM, "Learner profile", "displayName: Alice"),
                        new PreGenerationContextSection(
                                PreGenerationContextSectionTarget.USER, "Learner level", "learnerLevel: HSK2")),
                Map.of());

        var messages = builder.buildContext(new AgentContextBuildRequest(profile, session, List.of(), state));

        assertThat(messages.getFirst().content()).contains("Pre-generated system context");
        assertThat(messages.getFirst().content()).contains("displayName: Alice");
        assertThat(messages.get(1).content()).contains("Pre-generated user context");
        assertThat(messages.get(1).content()).contains("learnerLevel: HSK2");
    }
}
