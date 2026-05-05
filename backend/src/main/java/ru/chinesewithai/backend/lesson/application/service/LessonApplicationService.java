package ru.chinesewithai.backend.lesson.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.lesson.application.generation.LessonGenerationInputFactory;
import ru.chinesewithai.backend.lesson.application.generation.LessonGenerationPipelineCatalog;
import ru.chinesewithai.backend.lesson.application.generation.LessonGenerationPipelineRequest;
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
import ru.chinesewithai.backend.lesson.application.port.in.ListLessonsByModuleUseCase;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;
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

@Service
public class LessonApplicationService
        implements CreateLessonFromJsonUseCase, GenerateLessonFromDraftUseCase, GetLessonUseCase,
                ListLessonsByModuleUseCase {

    private static final String GENERATE_TASK = "Generate a lesson JSON from the provided lesson draft.";

    private final LessonRepository lessonRepository;
    private final LessonModuleRepository lessonModuleRepository;
    private final LessonContentValidator lessonContentValidator;
    private final LessonModuleStrategyCatalog strategyCatalog;
    private final LessonGenerationPromptFactory promptFactory;
    private final LessonGenerationProperties generationProperties;
    private final GetLessonDraftUseCase getLessonDraftUseCase;
    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final LessonGenerationPipelineCatalog pipelineCatalog;
    private final LessonGenerationInputFactory generationInputFactory;
    private final GeneratedLessonPersister generatedLessonPersister;
    private final LessonGenerationTraceRepository generationTraceRepository;
    private final LessonVocabularyTrackingService lessonVocabularyTrackingService;
    private final ObjectMapper objectMapper;

    public LessonApplicationService(
            LessonRepository lessonRepository,
            LessonModuleRepository lessonModuleRepository,
            LessonContentValidator lessonContentValidator,
            LessonModuleStrategyCatalog strategyCatalog,
            LessonGenerationPromptFactory promptFactory,
            LessonGenerationProperties generationProperties,
            GetLessonDraftUseCase getLessonDraftUseCase,
            StartAgentSessionUseCase startAgentSessionUseCase,
            LessonGenerationPipelineCatalog pipelineCatalog,
            LessonGenerationInputFactory generationInputFactory,
            GeneratedLessonPersister generatedLessonPersister,
            LessonGenerationTraceRepository generationTraceRepository,
            LessonVocabularyTrackingService lessonVocabularyTrackingService,
            ObjectMapper objectMapper) {
        this.lessonRepository = lessonRepository;
        this.lessonModuleRepository = lessonModuleRepository;
        this.lessonContentValidator = lessonContentValidator;
        this.strategyCatalog = strategyCatalog;
        this.promptFactory = promptFactory;
        this.generationProperties = generationProperties;
        this.getLessonDraftUseCase = getLessonDraftUseCase;
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.pipelineCatalog = pipelineCatalog;
        this.generationInputFactory = generationInputFactory;
        this.generatedLessonPersister = generatedLessonPersister;
        this.generationTraceRepository = generationTraceRepository;
        this.lessonVocabularyTrackingService = lessonVocabularyTrackingService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LessonView createFromJson(CreateLessonFromJsonCommand command) {
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
                module == null ? null : module.moduleKey(),
                command.sourceDraftId(),
                null,
                payload.title(),
                LanguageTag.of(payload.studyLanguage()),
                LanguageTag.of(payload.explanationLanguage()),
                LanguageTag.of(payload.translationLanguage()),
                payload.contentJson(),
                Instant.now()));
        lessonVocabularyTrackingService.recordLessonVocabulary(lesson, payload.newWords());
        lessonVocabularyTrackingService.recordReviewedVocabulary(lesson, payload.reviewWords());
        return toView(lesson);
    }

    @Override
    public LessonView generateFromDraft(GenerateLessonFromDraftCommand command) {
        var module = requireActiveModule(command.moduleKey());
        var draft = getLessonDraftUseCase.getDraft(new GetLessonDraftQuery(command.draftId()));
        strategyCatalog.getRequired(module.moduleKey()).validateDraftForGeneration(draft);
        var modelKey = resolveModelKey(command.modelKey());

        if (module.generationPipelineKey() != null) {
            var pipelineResult = pipelineCatalog
                    .getRequired(module.generationPipelineKey())
                    .generate(new LessonGenerationPipelineRequest(module, draft, modelKey));
            final ValidatedLessonPayload payload;
            try {
                payload = lessonContentValidator.validate(pipelineResult.finalOutputJson(), module);
            } catch (LessonContentValidationException ex) {
                generationTraceRepository.markRunFailed(pipelineResult.generationRunId(), ex.getMessage(), Instant.now());
                throw new LessonGenerationFailedException(pipelineResult.finalGeneratorSessionId(), ex.getMessage());
            }
            try {
                var lesson = generatedLessonPersister.persistGeneratedLesson(
                        module, draft.id(), pipelineResult.finalGeneratorSessionId(), payload, Instant.now());
                generationTraceRepository.markRunCompleted(
                        pipelineResult.generationRunId(), lesson.id(), pipelineResult.finalGeneratorSessionId(), Instant.now());
                return lesson;
            } catch (RuntimeException ex) {
                generationTraceRepository.markRunFailed(pipelineResult.generationRunId(), failureMessage(ex), Instant.now());
                throw ex;
            }
        }

        var session = startAgentSessionUseCase.startSession(new StartAgentSessionCommand(
                module.generatorProfileKey(),
                modelKey,
                GENERATE_TASK,
                writeJson(generationInputFactory.build(draft, module)),
                promptFactory.buildSystemPromptAppendix(module),
                module.generatorWorkflowVariantKey()));

        if (!"COMPLETED".equals(session.status()) || session.finalOutputJson() == null) {
            throw new LessonGenerationFailedException(session.sessionId(), session.failureReason());
        }

        final ValidatedLessonPayload payload;
        try {
            payload = lessonContentValidator.validate(session.finalOutputJson(), module);
        } catch (LessonContentValidationException ex) {
            throw new LessonGenerationFailedException(session.sessionId(), ex.getMessage());
        }

        return generatedLessonPersister.persistGeneratedLesson(module, draft.id(), session.sessionId(), payload, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonView getLesson(GetLessonQuery query) {
        var lesson = lessonRepository
                .findById(new LessonId(query.lessonId()))
                .orElseThrow(() -> new LessonNotFoundException(query.lessonId()));
        return toView(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<LessonView> listByModuleKey(String moduleKey) {
        return lessonRepository.findAllByModuleKeyOrderByCreatedAtDesc(normalizeOptional(moduleKey))
                .stream()
                .map(LessonApplicationService::toView)
                .toList();
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize lesson generation input", ex);
        }
    }

    private String failureMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Lesson generation failed";
        }
        return ex.getMessage();
    }

    private static LessonView toView(Lesson lesson) {
        return new LessonView(
                lesson.id().value(),
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
