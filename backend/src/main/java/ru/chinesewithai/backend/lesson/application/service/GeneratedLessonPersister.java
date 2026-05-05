package ru.chinesewithai.backend.lesson.application.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.lesson.application.port.out.LessonRepository;
import ru.chinesewithai.backend.lesson.application.validation.ValidatedLessonPayload;
import ru.chinesewithai.backend.lesson.application.view.LessonView;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Service
public class GeneratedLessonPersister {

    private final LessonRepository lessonRepository;
    private final LessonVocabularyTrackingService lessonVocabularyTrackingService;

    public GeneratedLessonPersister(
            LessonRepository lessonRepository,
            LessonVocabularyTrackingService lessonVocabularyTrackingService) {
        this.lessonRepository = lessonRepository;
        this.lessonVocabularyTrackingService = lessonVocabularyTrackingService;
    }

    @Transactional
    public LessonView persistGeneratedLesson(
            LessonModule module,
            UUID draftId,
            UUID generatorSessionId,
            ValidatedLessonPayload payload,
            Instant now) {
        var lesson = lessonRepository.save(Lesson.createNew(
                module.moduleKey(),
                draftId,
                generatorSessionId,
                payload.title(),
                LanguageTag.of(payload.studyLanguage()),
                LanguageTag.of(payload.explanationLanguage()),
                LanguageTag.of(payload.translationLanguage()),
                payload.contentJson(),
                now));
        lessonVocabularyTrackingService.recordLessonVocabulary(lesson, payload.newWords());
        lessonVocabularyTrackingService.recordReviewedVocabulary(lesson, payload.reviewWords());
        return toView(lesson);
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
}
