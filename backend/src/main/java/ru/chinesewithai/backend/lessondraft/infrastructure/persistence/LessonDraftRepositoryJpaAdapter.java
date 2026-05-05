package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftPage;
import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftRepository;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftId;

@Repository
public class LessonDraftRepositoryJpaAdapter implements LessonDraftRepository {

    private final SpringDataLessonDraftJpaRepository repository;

    public LessonDraftRepositoryJpaAdapter(SpringDataLessonDraftJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public LessonDraft save(LessonDraft draft) {
        var saved = repository.save(LessonDraftJpaMapper.toEntity(draft));
        return LessonDraftJpaMapper.toDomain(saved);
    }

    @Override
    public java.util.Optional<LessonDraft> findById(LessonDraftId draftId) {
        return repository.findWithSourcesById(draftId.value()).map(LessonDraftJpaMapper::toDomain);
    }

    @Override
    public LessonDraftPage findPage(int page, int size) {
        var result = repository.findPage(PageRequest.of(page, size));
        var items = result.getContent().stream().map(LessonDraftJpaMapper::toListItem).toList();
        return new LessonDraftPage(items, result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    @Override
    public void delete(LessonDraft draft) {
        repository.deleteById(draft.id().value());
    }
}
