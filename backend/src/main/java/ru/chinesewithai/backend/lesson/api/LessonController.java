package ru.chinesewithai.backend.lesson.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.config.OpenApiConfig;
import ru.chinesewithai.backend.lesson.api.dto.CreateLessonRequest;
import ru.chinesewithai.backend.lesson.api.dto.GenerateLessonRequest;
import ru.chinesewithai.backend.lesson.api.dto.LessonModuleResponse;
import ru.chinesewithai.backend.lesson.api.dto.LessonResponse;
import ru.chinesewithai.backend.lesson.application.command.CreateLessonFromJsonCommand;
import ru.chinesewithai.backend.lesson.application.command.GenerateLessonFromDraftCommand;
import ru.chinesewithai.backend.lesson.application.command.GetLessonQuery;
import ru.chinesewithai.backend.lesson.application.port.in.CreateLessonFromJsonUseCase;
import ru.chinesewithai.backend.lesson.application.port.in.GenerateLessonFromDraftUseCase;
import ru.chinesewithai.backend.lesson.application.port.in.GetLessonUseCase;
import ru.chinesewithai.backend.lesson.application.port.in.ListLessonModulesUseCase;
import ru.chinesewithai.backend.lesson.application.view.LessonModuleSummaryView;
import ru.chinesewithai.backend.lesson.application.view.LessonView;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final CreateLessonFromJsonUseCase createLessonFromJsonUseCase;
    private final GenerateLessonFromDraftUseCase generateLessonFromDraftUseCase;
    private final GetLessonUseCase getLessonUseCase;
    private final ListLessonModulesUseCase listLessonModulesUseCase;
    private final ObjectMapper objectMapper;

    public LessonController(
            CreateLessonFromJsonUseCase createLessonFromJsonUseCase,
            GenerateLessonFromDraftUseCase generateLessonFromDraftUseCase,
            GetLessonUseCase getLessonUseCase,
            ListLessonModulesUseCase listLessonModulesUseCase,
            ObjectMapper objectMapper) {
        this.createLessonFromJsonUseCase = createLessonFromJsonUseCase;
        this.generateLessonFromDraftUseCase = generateLessonFromDraftUseCase;
        this.getLessonUseCase = getLessonUseCase;
        this.listLessonModulesUseCase = listLessonModulesUseCase;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody CreateLessonRequest request) {
        validateContent(request.content());
        var lesson = createLessonFromJsonUseCase.createFromJson(
                new CreateLessonFromJsonCommand(request.moduleKey(), request.sourceDraftId(), writeJson(request.content())));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(lesson));
    }

    @PostMapping("/generate")
    public ResponseEntity<LessonResponse> generateLesson(@Valid @RequestBody GenerateLessonRequest request) {
        if (request.draftId() == null) {
            throw new IllegalArgumentException("draftId must not be null");
        }
        var lesson = generateLessonFromDraftUseCase.generateFromDraft(
                new GenerateLessonFromDraftCommand(request.draftId(), request.moduleKey(), request.modelKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(lesson));
    }

    @GetMapping("/modules")
    public List<LessonModuleResponse> listLessonModules() {
        return listLessonModulesUseCase.listAll().stream().map(this::toModuleResponse).toList();
    }

    @GetMapping("/{lessonId}")
    public LessonResponse getLesson(@PathVariable UUID lessonId) {
        return toResponse(getLessonUseCase.getLesson(new GetLessonQuery(lessonId)));
    }

    private void validateContent(JsonNode content) {
        if (content == null || !content.isObject()) {
            throw new IllegalArgumentException("content must be a JSON object");
        }
    }

    private LessonModuleResponse toModuleResponse(LessonModuleSummaryView view) {
        return new LessonModuleResponse(view.moduleKey(), view.displayName(), view.schemaVersion(), view.active());
    }

    private LessonResponse toResponse(LessonView view) {
        return new LessonResponse(
                view.id(),
                view.ownerId(),
                view.moduleKey(),
                view.sourceDraftId(),
                view.generatorSessionId(),
                view.title(),
                view.studyLanguage(),
                view.explanationLanguage(),
                view.translationLanguage(),
                readJson(view.contentJson()),
                view.createdAt(),
                view.updatedAt(),
                view.version());
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read lesson JSON", ex);
        }
    }

    private String writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write lesson JSON", ex);
        }
    }
}
