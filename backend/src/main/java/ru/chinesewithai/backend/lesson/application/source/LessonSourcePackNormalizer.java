package ru.chinesewithai.backend.lesson.application.source;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LessonSourcePackNormalizer {

    public LessonSourcePack normalizeLocally(LessonSourceBundle bundle, LessonSourceProcessingPolicy policy) {
        var items = new ArrayList<LessonSourcePackItem>();
        var refs = new ArrayList<LessonSourceRef>();
        var combined = new StringBuilder();

        for (var source : bundle.sources()) {
            var normalizedText = source.textContent() == null ? "" : source.textContent();
            var warnings = warningsFor(source, policy, normalizedText);
            items.add(new LessonSourcePackItem(
                    source.sourceId(),
                    source.position(),
                    source.mediaCategory(),
                    source.originalFileName(),
                    normalizedText,
                    warnings));
            refs.add(new LessonSourceRef(source.sourceId(), source.position(), "source-" + source.position()));
            if (!normalizedText.isBlank()) {
                if (!combined.isEmpty()) {
                    combined.append("\n\n");
                }
                combined.append(normalizedText);
            }
        }

        return new LessonSourcePack(1, items, combined.toString(), refs);
    }

    private List<String> warningsFor(
            LessonSourceManifestItem source,
            LessonSourceProcessingPolicy policy,
            String normalizedText) {
        if ("text".equals(source.mediaCategory())) {
            return List.of();
        }
        if ("image".equals(source.mediaCategory()) && normalizedText.isBlank()) {
            return List.of("Image source requires vision/OCR normalization.");
        }
        if ("pdf".equals(source.mediaCategory()) && normalizedText.isBlank()) {
            if (policy.pdfHandlingMode() == PdfHandlingMode.RENDER_TO_IMAGES_WHEN_NO_TEXT) {
                return List.of("PDF source has no extracted text; page rendering/OCR is required.");
            }
            return List.of("PDF source has no extracted text.");
        }
        return List.of();
    }
}
