package ru.chinesewithai.backend.agentruntime.infrastructure.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategy;

@Component
public class OutputValidationStrategyCatalog {

    private final Map<String, OutputValidationStrategy> strategiesByKey;

    public OutputValidationStrategyCatalog(List<OutputValidationStrategy> strategies) {
        var indexed = new LinkedHashMap<String, OutputValidationStrategy>();
        for (var strategy : strategies) {
            var previous = indexed.put(strategy.key(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate output validation strategy: " + strategy.key());
            }
        }
        this.strategiesByKey = Map.copyOf(indexed);
    }

    public boolean contains(String key) {
        return strategiesByKey.containsKey(key);
    }

    public OutputValidationStrategy getRequired(String key) {
        var strategy = strategiesByKey.get(key);
        if (strategy == null) {
            throw new IllegalStateException("Missing output validation strategy: " + key);
        }
        return strategy;
    }
}
