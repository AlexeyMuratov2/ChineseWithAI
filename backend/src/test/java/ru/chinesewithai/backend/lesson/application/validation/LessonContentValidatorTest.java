package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

class LessonContentValidatorTest {

    private final LessonModule testModule = new LessonModule(
            "TestModule",
            "TestModule",
            "prompt",
            1,
            true,
            Instant.now(),
            Instant.now());

    private final LessonContentValidator validator =
            new LessonContentValidator(new ObjectMapper(), new LessonModuleStrategyCatalog(List.of(new TestModuleLessonStrategy())));

    @Test
    void validatesGenericEnvelopeWithoutModule() {
        var payload = validator.validate(
                """
                {
                  "schemaVersion": 1,
                  "title": "Manual lesson",
                  "studyLanguage": "zh",
                  "explanationLanguage": "en",
                  "translationLanguage": "en",
                  "newWords": [],
                  "sections": []
                }
                """,
                null);

        assertThat(payload.title()).isEqualTo("Manual lesson");
        assertThat(payload.moduleKey()).isNull();
        assertThat(payload.studyLanguage()).isEqualTo("zh");
    }

    @Test
    void validatesTestModuleContract() {
        var payload = validator.validate(
                """
                {
                  "schemaVersion": 1,
                  "moduleKey": "TestModule",
                  "title": "认识和学习",
                  "studyLanguage": "zh",
                  "explanationLanguage": "zh",
                  "translationLanguage": "en",
                  "newWords": [
                    { "word": "认识", "pinyin": "rènshi", "translation": "to know" },
                    { "word": "学习", "pinyin": "xuéxí", "translation": "to study" }
                  ],
                  "sections": [
                    {
                      "type": "word_usage",
                      "title": "先看新词",
                      "items": [
                        { "word": "认识", "sentence": "我认识这个老师。", "translation": "I know this teacher." },
                        { "word": "学习", "sentence": "我每天学习中文。", "translation": "I study Chinese every day." }
                      ]
                    },
                    {
                      "type": "reading",
                      "title": "短文",
                      "text": "我认识这个老师，所以我每天跟他学习中文。",
                      "translation": "I know this teacher, so I study Chinese with him every day."
                    }
                  ]
                }
                """,
                testModule);

        assertThat(payload.moduleKey()).isEqualTo("TestModule");
        assertThat(payload.schemaVersion()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidTestModuleContract() {
        assertThatThrownBy(() -> validator.validate(
                        """
                        {
                          "schemaVersion": 1,
                          "moduleKey": "TestModule",
                          "title": "Broken",
                          "studyLanguage": "en",
                          "explanationLanguage": "zh",
                          "translationLanguage": "en",
                          "newWords": [],
                          "sections": []
                        }
                        """,
                        testModule))
                .isInstanceOf(LessonContentValidationException.class)
                .hasMessageContaining("studyLanguage");
    }
}
