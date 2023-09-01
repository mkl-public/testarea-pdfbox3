package mkl.testarea.pdfbox3.meta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
public class ResaveFile {
    final static File RESULT_FOLDER = new File("target/test-outputs", "meta");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    @Test
    public void testResaveInputDefault() throws IOException {
        try (   InputStream resource = getClass().getResourceAsStream("input.pdf");
                PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resource))  ) {
            document.save(new File(RESULT_FOLDER, "input-saveDefault.pdf"));
        }
    }

    @Test
    public void testResaveInputNoCompression() throws IOException {
        try (   InputStream resource = getClass().getResourceAsStream("input.pdf");
                PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resource))  ) {
            document.save(new File(RESULT_FOLDER, "input-saveNoCompression.pdf"), CompressParameters.NO_COMPRESSION);
        }
    }

    @Test
    public void testResaveInputDefaultFix() throws IOException {
        try (   InputStream resource = getClass().getResourceAsStream("input.pdf");
                PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resource))  ) {
            document.getDocument().getTrailer().removeItem(COSName.XREF_STM);
            document.save(new File(RESULT_FOLDER, "input-saveDefaultFix.pdf"));
        }
    }
}
