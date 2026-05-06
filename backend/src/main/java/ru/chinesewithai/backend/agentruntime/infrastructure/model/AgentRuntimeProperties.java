package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * General agent-runtime settings. When {@link #logModelIo()} is true, model gateways log full outbound request and
 * inbound response JSON (may include user content; use only on trusted environments).
 */
@ConfigurationProperties(prefix = "app.agentruntime")
public record AgentRuntimeProperties(boolean logModelIo) {}
