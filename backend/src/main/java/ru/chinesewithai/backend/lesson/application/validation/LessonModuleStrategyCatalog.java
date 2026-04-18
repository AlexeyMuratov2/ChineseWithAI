package ru.chinesewithai.backend.lesson.application.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LessonModuleStrategyCatalog {

    private final Map<String, LessonModuleStrategy> strategiesByModuleKey;

    public LessonModuleStrategyCatalog(List<LessonModuleStrategy> strategies) {
        var indexed = new LinkedHashMap<String, LessonModuleStrategy>();
        for (var strategy : strategies) {
            var previous = indexed.put(strategy.moduleKey(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate lesson module strategy: " + strategy.moduleKey());
            }
        }
        this.strategiesByModuleKey = Map.copyOf(indexed);
    }

    public LessonModuleStrategy getRequired(String moduleKey) {
        var strategy = strategiesByModuleKey.get(moduleKey);
        if (strategy == null) {
            throw new IllegalStateException("Missing lesson module strategy: " + moduleKey);
        }
        return strategy;
    }
}
