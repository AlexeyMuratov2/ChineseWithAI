package ru.chinesewithai.backend.lesson.application.generation;

import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@Component
public class LessonGenerationInputFactory {

    public LinkedHashMap<String, Object> build(LessonDraftView draft, LessonModule module) {
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
}
