package ru.chinesewithai.backend.agentruntime.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record OutputContract(Map<String, OutputFieldType> requiredFields) {

    public OutputContract {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        requiredFields = Map.copyOf(new LinkedHashMap<>(requiredFields));
    }
}
