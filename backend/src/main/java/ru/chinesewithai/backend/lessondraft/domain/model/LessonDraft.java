package ru.chinesewithai.backend.lessondraft.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LessonDraft {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 4_000;
    private static final int MAX_USER_INSTRUCTIONS_LENGTH = 4_000;

    private final LessonDraftId id;
    private final String title;
    private final String description;
    private final String userInstructions;
    private final LanguageTag explanationLanguage;
    private final LanguageTag translationLanguage;
    private final List<LessonDraftSource> sources;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private LessonDraft(
            LessonDraftId id,
            String title,
            String description,
            String userInstructions,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            List<LessonDraftSource> sources,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = normalizeRequiredText(title, "title", MAX_TITLE_LENGTH);
        this.description = normalizeOptionalText(description, "description", MAX_DESCRIPTION_LENGTH);
        this.userInstructions = normalizeOptionalText(userInstructions, "userInstructions", MAX_USER_INSTRUCTIONS_LENGTH);
        this.explanationLanguage = Objects.requireNonNull(explanationLanguage, "explanationLanguage must not be null");
        this.translationLanguage = Objects.requireNonNull(translationLanguage, "translationLanguage must not be null");
        this.sources = normalizeAndValidateSources(sources);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = validateVersion(version);
    }

    public static LessonDraft createNew(
            String title,
            String description,
            String userInstructions,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LessonDraft(
                LessonDraftId.newId(),
                title,
                description,
                userInstructions,
                resolveExplanationLanguage(explanationLanguage),
                resolveTranslationLanguage(translationLanguage),
                List.of(),
                now,
                now,
                0L);
    }

    public static LessonDraft reconstitute(
            LessonDraftId id,
            String title,
            String description,
            String userInstructions,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            List<LessonDraftSource> sources,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new LessonDraft(
                id,
                title,
                description,
                userInstructions,
                explanationLanguage,
                translationLanguage,
                sources,
                createdAt,
                updatedAt,
                version);
    }

    public LessonDraft updateMetadata(
            String title,
            String description,
            String userInstructions,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LessonDraft(
                id,
                title,
                description,
                userInstructions,
                resolveExplanationLanguage(explanationLanguage),
                resolveTranslationLanguage(translationLanguage),
                sources,
                createdAt,
                now,
                version);
    }

    public LessonDraft addTextSource(String textContent, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        var nextPosition = sources.size();
        var updatedSources = new ArrayList<>(sources);
        updatedSources.add(LessonDraftSource.createTextNote(nextPosition, textContent, now));
        return withSources(updatedSources, now);
    }

    public LessonDraft addDocumentSource(UUID documentFileId, String documentOriginalFileName, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        var nextPosition = sources.size();
        var updatedSources = new ArrayList<>(sources);
        updatedSources.add(LessonDraftSource.createDocumentFile(nextPosition, documentFileId, documentOriginalFileName, now));
        return withSources(updatedSources, now);
    }

    public LessonDraft removeSource(LessonDraftSourceId sourceId, Instant now) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (!containsSource(sourceId)) {
            throw new IllegalArgumentException("source not found: " + sourceId.value());
        }

        var updatedSources = new ArrayList<LessonDraftSource>();
        for (var source : sources) {
            if (!source.id().equals(sourceId)) {
                updatedSources.add(source);
            }
        }
        return withSources(reindex(updatedSources, now), now);
    }

    public LessonDraft reorderSources(List<LessonDraftSourceId> orderedSourceIds, Instant now) {
        Objects.requireNonNull(orderedSourceIds, "orderedSourceIds must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (orderedSourceIds.size() != sources.size()) {
            throw new IllegalArgumentException("orderedSourceIds must contain all sources exactly once");
        }

        var sourceIds = sources.stream().map(LessonDraftSource::id).collect(Collectors.toSet());
        var requestedIds = new HashSet<>(orderedSourceIds);
        if (requestedIds.size() != orderedSourceIds.size() || !requestedIds.equals(sourceIds)) {
            throw new IllegalArgumentException("orderedSourceIds must match current source ids exactly");
        }

        var sourceById = sources.stream().collect(Collectors.toMap(LessonDraftSource::id, Function.identity()));
        var reordered = new ArrayList<LessonDraftSource>(orderedSourceIds.size());
        for (int i = 0; i < orderedSourceIds.size(); i++) {
            var source = sourceById.get(orderedSourceIds.get(i));
            reordered.add(source.reposition(i, now));
        }
        return withSources(reordered, now);
    }

    public boolean containsSource(LessonDraftSourceId sourceId) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        return sources.stream().anyMatch(source -> source.id().equals(sourceId));
    }

    public int sourceCount() {
        return sources.size();
    }

    public LessonDraftId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String userInstructions() {
        return userInstructions;
    }

    public LanguageTag explanationLanguage() {
        return explanationLanguage;
    }

    public LanguageTag translationLanguage() {
        return translationLanguage;
    }

    public List<LessonDraftSource> sources() {
        return sources;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private LessonDraft withSources(List<LessonDraftSource> updatedSources, Instant now) {
        return new LessonDraft(
                id,
                title,
                description,
                userInstructions,
                explanationLanguage,
                translationLanguage,
                updatedSources,
                createdAt,
                now,
                version);
    }

    private static List<LessonDraftSource> reindex(List<LessonDraftSource> sourceList, Instant now) {
        var reindexed = new ArrayList<LessonDraftSource>(sourceList.size());
        for (int i = 0; i < sourceList.size(); i++) {
            reindexed.add(sourceList.get(i).reposition(i, now));
        }
        return reindexed;
    }

    private static LanguageTag resolveExplanationLanguage(LanguageTag languageTag) {
        return languageTag == null ? LanguageTag.DEFAULT_EXPLANATION_LANGUAGE : languageTag;
    }

    private static LanguageTag resolveTranslationLanguage(LanguageTag languageTag) {
        return languageTag == null ? LanguageTag.DEFAULT_TRANSLATION_LANGUAGE : languageTag;
    }

    private static long validateVersion(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("version must be >= 0");
        }
        return value;
    }

    private static String normalizeRequiredText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " chars");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " chars");
        }
        return normalized;
    }

    private static List<LessonDraftSource> normalizeAndValidateSources(List<LessonDraftSource> sourceList) {
        Objects.requireNonNull(sourceList, "sources must not be null");
        var copy = sourceList.stream()
                .sorted(Comparator.comparingInt(LessonDraftSource::position))
                .toList();

        var ids = new HashSet<LessonDraftSourceId>();
        for (int i = 0; i < copy.size(); i++) {
            var source = copy.get(i);
            if (source.position() != i) {
                throw new IllegalArgumentException("source positions must be dense and start at 0");
            }
            if (!ids.add(source.id())) {
                throw new IllegalArgumentException("duplicate source id: " + source.id().value());
            }
        }

        return List.copyOf(copy);
    }
}
