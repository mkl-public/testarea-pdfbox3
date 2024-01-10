package mkl.testarea.pdfbox3.content;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CopyFormattedPageText {
    final static File RESULT_FOLDER = new File("target/test-outputs", "content");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    @Test
    void testCopyFromElonInput() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/meta/input.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyText(source, target);
            target.save(new File(RESULT_FOLDER, "input-TextCopy.pdf"));
        }
    }

    @Test
    void testCopyFromTemplateTank() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/meta/TemplateTank.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyText(source, target);
            target.save(new File(RESULT_FOLDER, "TemplateTank-TextCopy.pdf"));
        }
    }

    @Test
    void testCopyFromTestDocumentSigned() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/render/test_document_signed.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyText(source, target);
            target.save(new File(RESULT_FOLDER, "test_document_signed-TextCopy.pdf"));
        }
    }

    void copyText(PDDocument source, PDDocument target) throws IOException {
        for (int i = 0; i < source.getNumberOfPages(); i++) {
            PDPage sourcePage = source.getPage(i);
            PDPage targetPage = null;
            if (i < target.getNumberOfPages())
                targetPage = target.getPage(i);
            else
                target.addPage(targetPage = new PDPage(sourcePage.getMediaBox()));
            copyText(source, i, target, targetPage);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/77706995/how-can-i-copy-extracted-text-page-by-page-in-a-document-to-a-new-pdf-document-w">
     * How can I copy extracted text page by page in a document to a new PDF document with PDFBox?
     * </a>
     * <p>
     * This method implements a way to copy text content from one document to another
     * following the ideas presented by the OP.
     * </p>
     * <p>
     * Beware, this is just a proof of concept and not a complete implementation. In
     * particular page rotation and non-upright text in general are not supported.
     * Also the only supported style attributes are text font and text size, other
     * details (like text color) are ignored. Different page geometries in source
     * and target also will result in weird appearances.
     * </p>
     */
    void copyText(PDDocument source, int sourcePageNumber, PDDocument target, PDPage targetPage) throws IOException {
        List<TextPosition> allTextPositions = new ArrayList<>();
        PDFTextStripper pdfTextStripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                allTextPositions.addAll(textPositions);
                super.writeString(text, textPositions);
            }
        };
        pdfTextStripper.setStartPage(sourcePageNumber + 1);
        pdfTextStripper.setEndPage(sourcePageNumber + 1);
        pdfTextStripper.getText(source);

        PDRectangle targetPageCropBox = targetPage.getCropBox();
        float yOffset = targetPageCropBox.getUpperRightY() + targetPageCropBox.getLowerLeftY();

        try (PDPageContentStream contentStream = new PDPageContentStream(target, targetPage, AppendMode.APPEND, true, true)) {
            contentStream.beginText();
            float x = 0;
            float y = yOffset;
            for (TextPosition position: allTextPositions) {
                contentStream.setFont(position.getFont(), position.getFontSizeInPt());
                contentStream.newLineAtOffset(position.getX() - x, - (position.getY() - y));
                contentStream.showText(position.getUnicode());
                x = position.getX();
                y = position.getY();
            }
            contentStream.endText();
        }
    }
}
