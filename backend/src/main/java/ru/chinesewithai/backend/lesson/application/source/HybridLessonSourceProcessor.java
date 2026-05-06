package ru.chinesewithai.backend.lesson.application.source;

import org.springframework.stereotype.Component;

@Component
public class HybridLessonSourceProcessor implements LessonSourceProcessor {

    private final LessonSourceBundleFactory bundleFactory;
    private final LessonSourcePackNormalizer normalizer;

    public HybridLessonSourceProcessor(LessonSourceBundleFactory bundleFactory, LessonSourcePackNormalizer normalizer) {
        this.bundleFactory = bundleFactory;
        this.normalizer = normalizer;
    }

    @Override
    public LessonSourceProcessingMode mode() {
        return LessonSourceProcessingMode.HYBRID;
    }

    @Override
    public LessonSourceProcessingResult process(LessonSourceProcessingRequest request) {
        var bundle = bundleFactory.build(request.draft(), request.policy());
        return new LessonSourceProcessingResult(mode(), bundle, normalizer.normalizeLocally(bundle, request.policy()));
    }
}
