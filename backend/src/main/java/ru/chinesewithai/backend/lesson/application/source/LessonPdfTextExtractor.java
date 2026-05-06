package ru.chinesewithai.backend.lesson.application.source;

import java.io.IOException;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class LessonPdfTextExtractor {

    public Optional<String> extractText(byte[] pdfBytes) {
        try (var document = Loader.loadPDF(pdfBytes)) {
            var stripper = new PDFTextStripper();
            return normalize(stripper.getText(document));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static Optional<String> normalize(String value) {
        if (value == null) {
            return Optional.empty();
        }
        var normalized = value.trim();
        return normalized.isBlank() ? Optional.empty() : Optional.of(normalized);
    }
}
