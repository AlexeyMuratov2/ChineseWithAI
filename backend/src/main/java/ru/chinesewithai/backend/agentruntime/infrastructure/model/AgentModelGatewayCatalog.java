package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelCatalog;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;

@Component
public class AgentModelGatewayCatalog implements AgentModelCatalog {

    private final Map<String, AgentModelDescriptor> descriptorsByKey;
    private final Map<String, AgentModelGateway> gatewaysByModelKey;

    public AgentModelGatewayCatalog(List<AgentModelGateway> gateways) {
        var indexedDescriptors = new LinkedHashMap<String, AgentModelDescriptor>();
        var indexedGateways = new LinkedHashMap<String, AgentModelGateway>();
        for (var gateway : gateways) {
            for (var descriptor : gateway.supportedModels()) {
                if (!gateway.providerKey().equals(descriptor.providerKey())) {
                    throw new IllegalStateException("Model descriptor provider mismatch: " + descriptor.modelKey());
                }
                var previousDescriptor = indexedDescriptors.put(descriptor.modelKey(), descriptor);
                if (previousDescriptor != null) {
                    throw new IllegalStateException("Duplicate model key: " + descriptor.modelKey());
                }
                indexedGateways.put(descriptor.modelKey(), gateway);
            }
        }
        this.descriptorsByKey = Map.copyOf(indexedDescriptors);
        this.gatewaysByModelKey = Map.copyOf(indexedGateways);
    }

    @Override
    public Optional<AgentModelDescriptor> findByModelKey(String modelKey) {
        return Optional.ofNullable(descriptorsByKey.get(modelKey));
    }

    @Override
    public List<AgentModelDescriptor> findVisibleModels() {
        return descriptorsByKey.values().stream().filter(AgentModelDescriptor::visible).toList();
    }

    public boolean contains(String modelKey) {
        return descriptorsByKey.containsKey(modelKey);
    }

    public AgentModelDescriptor getRequiredDescriptor(String modelKey) {
        var descriptor = descriptorsByKey.get(modelKey);
        if (descriptor == null) {
            throw new IllegalStateException("Unknown model: " + modelKey);
        }
        return descriptor;
    }

    public AgentModelGateway getRequiredGateway(String modelKey) {
        var gateway = gatewaysByModelKey.get(modelKey);
        if (gateway == null) {
            throw new IllegalStateException("Unknown model gateway: " + modelKey);
        }
        return gateway;
    }
}
