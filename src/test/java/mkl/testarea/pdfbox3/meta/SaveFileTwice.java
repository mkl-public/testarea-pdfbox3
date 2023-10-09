package mkl.testarea.pdfbox3.meta;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
class SaveFileTwice {
    final static File RESULT_FOLDER = new File("target/test-outputs", "meta");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    /**
     * <a href="https://issues.apache.org/jira/browse/PDFBOX-5696">
     * COSStream lost, becomes a COSDictionary
     * </a>
     * <br/>
     * <a href="https://issues.apache.org/jira/secure/attachment/13063398/TemplateTank.pdf">
     * TemplateTank.pdf
     * </a>
     * <br/>
     * <a href="https://issues.apache.org/jira/secure/attachment/13063399/CambriaMath.ttf">
     * CambriaMath.ttf
     * </a>
     * <p>
     * Indeed, the appearance stream loses its stream data. Also important, it
     * is included in an object stream. This might cause the loss of the stream
     * data as streams are not allowed in object streams.
     * </p>
     * <p>
     * Furthermore, another issue becomes apparent already in the first saved object:
     * This single revision document has a cross reference stream with segmented object
     * number ranges. This strictly speaking is not allowed in the first cross reference
     * section of a PDF.
     * </p>
     */
    @Test
    void testSaveTwiceLikePadosAttila() throws IOException {
        try (
            InputStream resourceAsStream = getClass().getResourceAsStream("TemplateTank.pdf");
            PDDocument a1doc = Loader.loadPDF(new RandomAccessReadBuffer(resourceAsStream))
        ) {
            PDAcroForm form = a1doc.getDocumentCatalog().getAcroForm();
            PDResources dr = form.getDefaultResources();

            form.getField("Site Name").setValue("Site Name");
            a1doc.save(new File(RESULT_FOLDER, "30-1.pdf"));

            PDFont font = PDType0Font.load(a1doc, getClass().getResourceAsStream("CambriaMath.ttf"), false);
            dr.add(font);
            a1doc.save(new File(RESULT_FOLDER, "30-2.pdf"));
        }
    }

}
