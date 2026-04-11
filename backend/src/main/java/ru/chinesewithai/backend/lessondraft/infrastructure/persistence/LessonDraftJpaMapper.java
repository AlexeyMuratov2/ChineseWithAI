package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftListItem;
import ru.chinesewithai.backend.lessondraft.domain.model.LanguageTag;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftId;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSource;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceId;

final class LessonDraftJpaMapper {

    private LessonDraftJpaMapper() {}

    static LessonDraftJpaEntity toEntity(LessonDraft draft) {
        var entity = new LessonDraftJpaEntity(
                draft.id().value(),
                draft.ownerId(),
                draft.title(),
                draft.description(),
                draft.userInstructions(),
                draft.explanationLanguage().value(),
                draft.translationLanguage().value(),
                draft.createdAt(),
                draft.updatedAt(),
                draft.version());

        var sources = draft.sources().stream().map(LessonDraftJpaMapper::toSourceEntity).toList();
        entity.replaceSources(sources);
        return entity;
    }

    static LessonDraft toDomain(LessonDraftJpaEntity entity) {
        var sources = entity.getSources().stream().map(LessonDraftJpaMapper::toSourceDomain).toList();

        return LessonDraft.reconstitute(
                new LessonDraftId(entity.getId()),
                entity.getOwnerId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getUserInstructions(),
                LanguageTag.of(entity.getExplanationLanguage()),
                LanguageTag.of(entity.getTranslationLanguage()),
                sources,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    static LessonDraftListItem toListItem(LessonDraftListItemJpaProjection projection) {
        return new LessonDraftListItem(
                projection.id(),
                projection.title(),
                projection.explanationLanguage(),
                projection.translationLanguage(),
                Math.toIntExact(projection.sourceCount()),
                projection.createdAt(),
                projection.updatedAt(),
                projection.version());
    }

    private static LessonDraftSourceJpaEntity toSourceEntity(LessonDraftSource source) {
        return new LessonDraftSourceJpaEntity(
                source.id().value(),
                source.type(),
                source.position(),
                source.textContent(),
                source.documentFileId(),
                source.documentOriginalFileName(),
                source.createdAt(),
                source.updatedAt());
    }

    private static LessonDraftSource toSourceDomain(LessonDraftSourceJpaEntity source) {
        return LessonDraftSource.reconstitute(
                new LessonDraftSourceId(source.getId()),
                source.getSourceType(),
                source.getPosition(),
                source.getTextContent(),
                source.getDocumentFileId(),
                source.getDocumentOriginalFileName(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
