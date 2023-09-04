package mkl.testarea.pdfbox3.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.RenderDestination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
class RenderAcrobatSignSignatures {
    final static File RESULT_FOLDER = new File("target/test-outputs", "render");

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    /**
     * <a href="https://stackoverflow.com/questions/77002400/extract-signature-image-from-pdf-signed-with-adobe-sign">
     * Extract signature image from PDF signed with Adobe Sign
     * </a>
     * <br/>
     * <a href="https://file.io/5UQcJejlOWzQ">
     * test_document_signed.pdf
     * </a>
     * <p>
     * This test illustrates how to render the signature XObjects from the OP's
     * example PDF to bitmaps using the {@link ContentStreamRenderer}.
     * </p>
     */
    @Test
    void testRenderTestDocumentSigned() throws Exception {
        try (
            InputStream resource = getClass().getResourceAsStream("test_document_signed.pdf");
            PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBuffer(resource))
        ) {
            ContentStreamRenderer renderer = new ContentStreamRenderer(pdDocument);
            PDPage pdPage = pdDocument.getPage(0);
            PDResources pdResources = pdPage.getResources();

            for (COSName xObjectName : pdResources.getXObjectNames()) {
                PDXObject pdXObject = pdResources.getXObject(xObjectName);
                if (pdXObject instanceof PDFormXObject) {
                    PDFormXObject pdFormXObject = (PDFormXObject) pdXObject;
                    BufferedImage image = renderer.renderImage(pdPage, pdFormXObject, 4, ImageType.RGB, RenderDestination.VIEW);
                    ImageIO.write(image, "png", new File(RESULT_FOLDER, "test_document_signed-1-" + xObjectName.getName() + ".png"));

                    PDResources innerResources = pdFormXObject.getResources();
                    for (COSName innerXObjectName : innerResources.getXObjectNames()) {
                        PDXObject innerXObject = innerResources.getXObject(innerXObjectName);
                        if (innerXObject instanceof PDFormXObject) {
                            PDFormXObject innerFormXObject = (PDFormXObject) innerXObject;
                            image = renderer.renderImage(pdPage, innerFormXObject, 4, ImageType.RGB, RenderDestination.VIEW);
                            ImageIO.write(image, "png", new File(RESULT_FOLDER, "test_document_signed-1-" + xObjectName.getName() + "-" + innerXObjectName.getName() + ".png"));
                        }
                    }
                }
            }
        }
    }
}
