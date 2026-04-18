package ru.chinesewithai.backend.lesson.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.lesson.application.command.CreateLessonFromJsonCommand;
import ru.chinesewithai.backend.lesson.application.command.GenerateLessonFromDraftCommand;
import ru.chinesewithai.backend.lesson.application.command.GetLessonQuery;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.exception.LessonGenerationFailedException;
import ru.chinesewithai.backend.lesson.application.exception.LessonModuleInactiveException;
import ru.chinesewithai.backend.lesson.application.exception.LessonModuleNotFoundException;
import ru.chinesewithai.backend.lesson.application.exception.LessonNotFoundException;
import ru.chinesewithai.backend.lesson.application.port.in.CreateLessonFromJsonUseCase;
import ru.chinesewithai.backend.lesson.application.port.in.GenerateLessonFromDraftUseCase;
import ru.chinesewithai.backend.lesson.application.port.in.GetLessonUseCase;
import ru.chinesewithai.backend.lesson.application.port.out.CurrentLessonOwnerProvider;
import ru.chinesewithai.backend.lesson.application.port.out.LessonModuleRepository;
import ru.chinesewithai.backend.lesson.application.port.out.LessonRepository;
import ru.chinesewithai.backend.lesson.application.validation.LessonContentValidator;
import ru.chinesewithai.backend.lesson.application.validation.LessonGenerationPromptFactory;
import ru.chinesewithai.backend.lesson.application.validation.LessonModuleStrategyCatalog;
import ru.chinesewithai.backend.lesson.application.validation.ValidatedLessonPayload;
import ru.chinesewithai.backend.lesson.application.view.LessonView;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lesson.infrastructure.config.LessonGenerationProperties;
import ru.chinesewithai.backend.lessondraft.application.command.GetLessonDraftQuery;
import ru.chinesewithai.backend.lessondraft.application.port.in.GetLessonDraftUseCase;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@Service
public class LessonApplicationService
        implements CreateLessonFromJsonUseCase, GenerateLessonFromDraftUseCase, GetLessonUseCase {

    private static final String LESSON_GENERATOR_PROFILE_KEY = "lesson-generator:v1";
    private static final String GENERATE_TASK = "Generate a lesson JSON from the provided lesson draft.";

    private final LessonRepository lessonRepository;
    private final LessonModuleRepository lessonModuleRepository;
    private final CurrentLessonOwnerProvider currentLessonOwnerProvider;
    private final LessonContentValidator lessonContentValidator;
    private final LessonModuleStrategyCatalog strategyCatalog;
    private final LessonGenerationPromptFactory promptFactory;
    private final LessonGenerationProperties generationProperties;
    private final GetLessonDraftUseCase getLessonDraftUseCase;
    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final ObjectMapper objectMapper;

    public LessonApplicationService(
            LessonRepository lessonRepository,
            LessonModuleRepository lessonModuleRepository,
            CurrentLessonOwnerProvider currentLessonOwnerProvider,
            LessonContentValidator lessonContentValidator,
            LessonModuleStrategyCatalog strategyCatalog,
            LessonGenerationPromptFactory promptFactory,
            LessonGenerationProperties generationProperties,
            GetLessonDraftUseCase getLessonDraftUseCase,
            StartAgentSessionUseCase startAgentSessionUseCase,
            ObjectMapper objectMapper) {
        this.lessonRepository = lessonRepository;
        this.lessonModuleRepository = lessonModuleRepository;
        this.currentLessonOwnerProvider = currentLessonOwnerProvider;
        this.lessonContentValidator = lessonContentValidator;
        this.strategyCatalog = strategyCatalog;
        this.promptFactory = promptFactory;
        this.generationProperties = generationProperties;
        this.getLessonDraftUseCase = getLessonDraftUseCase;
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LessonView createFromJson(CreateLessonFromJsonCommand command) {
        var ownerId = currentLessonOwnerProvider.getCurrentOwnerId();
        if (command.sourceDraftId() != null) {
            getLessonDraftUseCase.getDraft(new GetLessonDraftQuery(command.sourceDraftId()));
        }

        var requestedModuleKey = normalizeOptional(command.moduleKey());
        var jsonModuleKey = lessonContentValidator.readModuleKeyOrNull(command.contentJson());
        if (requestedModuleKey != null && jsonModuleKey != null && !requestedModuleKey.equals(jsonModuleKey)) {
            throw new LessonContentValidationException("moduleKey in request does not match content.moduleKey");
        }

        var effectiveModuleKey = requestedModuleKey != null ? requestedModuleKey : jsonModuleKey;
        var module = effectiveModuleKey == null ? null : requireActiveModule(effectiveModuleKey);
        var payload = lessonContentValidator.validate(command.contentJson(), module);

        var lesson = lessonRepository.save(Lesson.createNew(
                ownerId,
                module == null ? null : module.moduleKey(),
                command.sourceDraftId(),
                null,
                payload.title(),
                LanguageTag.of(payload.studyLanguage()),
                LanguageTag.of(payload.explanationLanguage()),
                LanguageTag.of(payload.translationLanguage()),
                payload.contentJson(),
                Instant.now()));
        return toView(lesson);
    }

    @Override
    @Transactional
    public LessonView generateFromDraft(GenerateLessonFromDraftCommand command) {
        var ownerId = currentLessonOwnerProvider.getCurrentOwnerId();
        var module = requireActiveModule(command.moduleKey());
        var draft = getLessonDraftUseCase.getDraft(new GetLessonDraftQuery(command.draftId()));
        strategyCatalog.getRequired(module.moduleKey()).validateDraftForGeneration(draft);

        var session = startAgentSessionUseCase.startSession(new StartAgentSessionCommand(
                LESSON_GENERATOR_PROFILE_KEY,
                resolveModelKey(command.modelKey()),
                GENERATE_TASK,
                writeJson(buildGenerationInput(draft, module)),
                promptFactory.buildSystemPromptAppendix(module)));

        if (!"COMPLETED".equals(session.status()) || session.finalOutputJson() == null) {
            throw new LessonGenerationFailedException(session.sessionId(), session.failureReason());
        }

        final ValidatedLessonPayload payload;
        try {
            payload = lessonContentValidator.validate(session.finalOutputJson(), module);
        } catch (LessonContentValidationException ex) {
            throw new LessonGenerationFailedException(session.sessionId(), ex.getMessage());
        }

        var lesson = lessonRepository.save(Lesson.createNew(
                ownerId,
                module.moduleKey(),
                draft.id(),
                session.sessionId(),
                payload.title(),
                LanguageTag.of(payload.studyLanguage()),
                LanguageTag.of(payload.explanationLanguage()),
                LanguageTag.of(payload.translationLanguage()),
                payload.contentJson(),
                Instant.now()));
        return toView(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonView getLesson(GetLessonQuery query) {
        var ownerId = currentLessonOwnerProvider.getCurrentOwnerId();
        var lesson = lessonRepository
                .findByIdAndOwnerId(new LessonId(query.lessonId()), ownerId)
                .orElseThrow(() -> new LessonNotFoundException(query.lessonId()));
        return toView(lesson);
    }

    private LessonModule requireActiveModule(String moduleKey) {
        var module = lessonModuleRepository
                .findByModuleKey(moduleKey)
                .orElseThrow(() -> new LessonModuleNotFoundException(moduleKey));
        if (!module.active()) {
            throw new LessonModuleInactiveException(moduleKey);
        }
        return module;
    }

    private String resolveModelKey(String requestedModelKey) {
        var normalized = normalizeOptional(requestedModelKey);
        return normalized == null ? generationProperties.defaultModelKey() : normalized;
    }

    private Object buildGenerationInput(LessonDraftView draft, LessonModule module) {
        var orderedSources = draft.sources().stream()
                .map(source -> {
                    var payload = new LinkedHashMap<String, Object>();
                    payload.put("id", source.id());
                    payload.put("type", source.type());
                    payload.put("position", source.position());
                    payload.put("textContent", source.textContent());
                    payload.put("documentFileId", source.documentFileId());
                    payload.put("documentOriginalFileName", source.documentOriginalFileName());
                    return payload;
                })
                .toList();

        var draftPayload = new LinkedHashMap<String, Object>();
        draftPayload.put("id", draft.id());
        draftPayload.put("title", draft.title());
        draftPayload.put("description", draft.description());
        draftPayload.put("userInstructions", draft.userInstructions());
        draftPayload.put("explanationLanguage", draft.explanationLanguage());
        draftPayload.put("translationLanguage", draft.translationLanguage());
        draftPayload.put("sources", orderedSources);

        var input = new LinkedHashMap<String, Object>();
        input.put("draftId", draft.id());
        input.put("moduleKey", module.moduleKey());
        input.put("moduleSchemaVersion", module.schemaVersion());
        input.put("draft", draftPayload);
        input.put("orderedSources", List.copyOf(orderedSources));
        return input;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize lesson generation input", ex);
        }
    }

    private static LessonView toView(Lesson lesson) {
        return new LessonView(
                lesson.id().value(),
                lesson.ownerId(),
                lesson.moduleKey(),
                lesson.sourceDraftId(),
                lesson.generatorSessionId(),
                lesson.title(),
                lesson.studyLanguage().value(),
                lesson.explanationLanguage().value(),
                lesson.translationLanguage().value(),
                lesson.contentJson(),
                lesson.createdAt(),
                lesson.updatedAt(),
                lesson.version());
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
