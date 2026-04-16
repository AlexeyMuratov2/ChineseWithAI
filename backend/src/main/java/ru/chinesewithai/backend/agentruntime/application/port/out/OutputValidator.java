package ru.chinesewithai.backend.agentruntime.application.port.out;

import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;

public interface OutputValidator {
    void validate(String outputJson, OutputContract contract);
}
