package mkl.testarea.pdfbox3.sign;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
class PrepareForSigning {
    final static File RESULT_FOLDER = new File("target/test-outputs", "meta");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    /**
     * <a href="https://issues.apache.org/jira/browse/PDFBOX-5717">
     * NullPointerException calling saveIncrementalForExternalSigning
     * </a>
     * <br/>
     * <a href="https://issues.apache.org/jira/secure/attachment/13064607/Cryptomathic_White_Paper_-_eIDAS_Compliant_Remote_eSigning.pdf">
     * Cryptomathic_White_Paper_-_eIDAS_Compliant_Remote_eSigning.pdf
     * </a>
     * <p>
     * Object 3 is referred to from the catalog as root of the JavaScript name
     * tree but there is no object 3 in the PDF. PDFBox should treat this as a
     * <code>null</code> object but unfortunately doesn't.
     * </p>
     */
    @Test
    void testPrepareCryptomathicWhitePaper() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("Cryptomathic_White_Paper_-_eIDAS_Compliant_Remote_eSigning.pdf");
            PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            OutputStream result = new FileOutputStream(new File(RESULT_FOLDER, "Cryptomathic_White_Paper_-_eIDAS_Compliant_Remote_eSigning-prepared.pdf"))
        ) {
            document.addSignature(new PDSignature(), new SignatureOptions());
            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(result);
        }
    }

}
