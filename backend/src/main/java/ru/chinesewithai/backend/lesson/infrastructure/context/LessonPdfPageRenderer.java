package ru.chinesewithai.backend.lesson.infrastructure.context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

@Component
public class LessonPdfPageRenderer {

    private static final float RENDER_DPI = 144.0f;
    private static final int MAX_PDF_BYTES = 5 * 1024 * 1024;

    public List<String> renderPngDataUrls(byte[] pdfBytes, int maxPages, int maxImageBytes) {
        if (pdfBytes.length > MAX_PDF_BYTES) {
            throw new IllegalStateException("Lesson source PDF must be at most 5 MB for multimodal input");
        }
        try (var document = Loader.loadPDF(pdfBytes)) {
            var renderer = new PDFRenderer(document);
            var pageCount = Math.min(maxPages, document.getNumberOfPages());
            var dataUrls = new ArrayList<String>(pageCount);
            for (var pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                dataUrls.add(renderPage(renderer, pageIndex, maxImageBytes));
            }
            return List.copyOf(dataUrls);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render lesson source PDF pages for multimodal input", ex);
        }
    }

    private String renderPage(PDFRenderer renderer, int pageIndex, int maxImageBytes) throws IOException {
        var pageImage = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB);
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(pageImage, "png", out);
            var bytes = out.toByteArray();
            if (bytes.length > maxImageBytes) {
                throw new IllegalStateException("Rendered PDF page must be at most %d bytes".formatted(maxImageBytes));
            }
            return "data:image/png;base64,%s".formatted(Base64.getEncoder().encodeToString(bytes));
        }
    }
}
