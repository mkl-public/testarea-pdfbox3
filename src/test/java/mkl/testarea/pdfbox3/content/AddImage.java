package mkl.testarea.pdfbox3.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
class AddImage {
    final static File RESULT_FOLDER = new File("target/test-outputs", "content");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    /**
     * <a href="https://stackoverflow.com/questions/78147734/blue-box-appearing-instead-of-digital-signature-and-signature-panel-contains-uns">
     * Blue box appearing instead of digital signature and signature panel contains unsigned signatures
     * </a>
     * <p>
     * In a comment the OP claimed that <i>images are inverted when it is added to the page</i>
     * with this code. I cannot reproduce this.
     * </p>
     */
    @Test
    void testAddImageLikeDeepMorker() throws IOException {
        try (
                InputStream resource = getClass().getResourceAsStream("InvoiceYesLogic.pdf");
                PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(resource))) {
            
            PDPage page = doc.getPage(0);
            float width = 118;
            float height = 110;
            float x = 147;
            float y = 110;
            PDImageXObject pdImage = PDImageXObject.createFromFile("src/test/resources/mkl/testarea/pdfbox3/content/Willi-1.jpg", doc);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
               contents.transform(Matrix.getScaleInstance(1f, 1f));
               contents.drawImage(pdImage, x , y + height , width, height);
            }
            doc.save(new File(RESULT_FOLDER, "InvoiceYesLogic-withImageLikeDeepMorker.pdf"));
         }
    }
}
