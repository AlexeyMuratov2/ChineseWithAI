package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepCatalog;

@Component
public class SpringPreGenerationStepCatalog implements PreGenerationStepCatalog {

    private final Map<String, PreGenerationStep> stepsByKey;

    public SpringPreGenerationStepCatalog(List<PreGenerationStep> steps) {
        var indexed = new LinkedHashMap<String, PreGenerationStep>();
        for (var step : steps) {
            var previous = indexed.put(step.key(), step);
            if (previous != null) {
                throw new IllegalStateException("Duplicate pre-generation step key: " + step.key());
            }
        }
        this.stepsByKey = Map.copyOf(indexed);
    }

    @Override
    public boolean contains(String stepKey) {
        return stepsByKey.containsKey(stepKey);
    }

    @Override
    public PreGenerationStep getRequired(String stepKey) {
        var step = stepsByKey.get(stepKey);
        if (step == null) {
            throw new IllegalStateException("Unknown pre-generation step: " + stepKey);
        }
        return step;
    }
}
