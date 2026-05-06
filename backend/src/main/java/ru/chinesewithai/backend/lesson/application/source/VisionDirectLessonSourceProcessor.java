package ru.chinesewithai.backend.lesson.application.source;

import org.springframework.stereotype.Component;

@Component
public class VisionDirectLessonSourceProcessor implements LessonSourceProcessor {

    private final LessonSourceBundleFactory bundleFactory;

    public VisionDirectLessonSourceProcessor(LessonSourceBundleFactory bundleFactory) {
        this.bundleFactory = bundleFactory;
    }

    @Override
    public LessonSourceProcessingMode mode() {
        return LessonSourceProcessingMode.VISION_DIRECT;
    }

    @Override
    public LessonSourceProcessingResult process(LessonSourceProcessingRequest request) {
        return new LessonSourceProcessingResult(mode(), bundleFactory.build(request.draft(), request.policy()), null);
    }
}
