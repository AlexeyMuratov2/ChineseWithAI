package ru.chinesewithai.backend.lesson.application.source;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LessonSourceProcessorCatalog {

    private final Map<LessonSourceProcessingMode, LessonSourceProcessor> processorsByMode;

    public LessonSourceProcessorCatalog(List<LessonSourceProcessor> processors) {
        var indexed = new LinkedHashMap<LessonSourceProcessingMode, LessonSourceProcessor>();
        for (var processor : processors) {
            var previous = indexed.put(processor.mode(), processor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate lesson source processor: " + processor.mode());
            }
        }
        this.processorsByMode = Map.copyOf(indexed);
    }

    public LessonSourceProcessor getRequired(LessonSourceProcessingMode mode) {
        var processor = processorsByMode.get(mode);
        if (processor == null) {
            throw new IllegalStateException("Unknown lesson source processor: " + mode);
        }
        return processor;
    }
}
