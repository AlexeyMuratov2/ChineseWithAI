package ru.chinesewithai.backend.lesson.application.generation;

public interface LessonGenerationPipeline {

    String key();

    LessonGenerationPipelineResult generate(LessonGenerationPipelineRequest request);
}
