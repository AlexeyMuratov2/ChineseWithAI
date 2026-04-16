package ru.chinesewithai.backend.agentruntime.infrastructure.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuilder;

@Component
public class AgentContextBuilderCatalog {

    private final Map<String, AgentContextBuilder> buildersByKey;

    public AgentContextBuilderCatalog(List<AgentContextBuilder> builders) {
        var indexed = new LinkedHashMap<String, AgentContextBuilder>();
        for (var builder : builders) {
            var previous = indexed.put(builder.key(), builder);
            if (previous != null) {
                throw new IllegalStateException("Duplicate context builder key: " + builder.key());
            }
        }
        this.buildersByKey = Map.copyOf(indexed);
    }

    public boolean contains(String builderKey) {
        return buildersByKey.containsKey(builderKey);
    }

    public AgentContextBuilder getRequired(String builderKey) {
        var builder = buildersByKey.get(builderKey);
        if (builder == null) {
            throw new IllegalStateException("Unknown context builder: " + builderKey);
        }
        return builder;
    }
}
