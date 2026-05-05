package ru.chinesewithai.backend.lesson.application.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LessonGenerationPipelineCatalog {

    private final Map<String, LessonGenerationPipeline> pipelinesByKey;

    public LessonGenerationPipelineCatalog(List<LessonGenerationPipeline> pipelines) {
        var indexed = new LinkedHashMap<String, LessonGenerationPipeline>();
        for (var pipeline : pipelines) {
            var previous = indexed.put(pipeline.key(), pipeline);
            if (previous != null) {
                throw new IllegalStateException("Duplicate lesson generation pipeline key: " + pipeline.key());
            }
        }
        this.pipelinesByKey = Map.copyOf(indexed);
    }

    public LessonGenerationPipeline getRequired(String key) {
        var pipeline = pipelinesByKey.get(key);
        if (pipeline == null) {
            throw new IllegalStateException("Unknown lesson generation pipeline: " + key);
        }
        return pipeline;
    }
}
