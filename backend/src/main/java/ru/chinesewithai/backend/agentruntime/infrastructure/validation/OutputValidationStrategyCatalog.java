package ru.chinesewithai.backend.agentruntime.infrastructure.validation;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategy;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;

@Component
public class OutputValidationStrategyCatalog {

    private final List<OutputValidationStrategy> strategies;

    public OutputValidationStrategyCatalog(List<OutputValidationStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public List<OutputValidationStrategy> resolve(OutputValidationStrategyRequest request) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(request))
                .sorted(Comparator.comparingInt(OutputValidationStrategy::order))
                .toList();
    }
}
