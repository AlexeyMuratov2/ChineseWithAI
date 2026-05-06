package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record AgentModelMessageContentPart(AgentModelMessageContentPartType type, String text, String imageUrl) {

    public AgentModelMessageContentPart {
        Objects.requireNonNull(type, "type must not be null");
        switch (type) {
            case TEXT -> {
                requireText(text, "text");
                requireNull(imageUrl, "imageUrl");
            }
            case IMAGE_URL -> {
                requireText(imageUrl, "imageUrl");
                requireNull(text, "text");
            }
        }
    }

    public static AgentModelMessageContentPart text(String text) {
        return new AgentModelMessageContentPart(AgentModelMessageContentPartType.TEXT, text, null);
    }

    public static AgentModelMessageContentPart imageUrl(String imageUrl) {
        return new AgentModelMessageContentPart(AgentModelMessageContentPartType.IMAGE_URL, null, imageUrl);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requireNull(Object value, String fieldName) {
        if (value != null) {
            throw new IllegalArgumentException(fieldName + " must be null");
        }
    }
}
