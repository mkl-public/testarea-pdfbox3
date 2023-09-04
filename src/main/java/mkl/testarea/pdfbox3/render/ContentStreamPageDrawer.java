package mkl.testarea.pdfbox3.render;

import java.io.IOException;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;

/**
 * <a href="https://stackoverflow.com/questions/77002400/extract-signature-image-from-pdf-signed-with-adobe-sign">
 * Extract signature image from PDF signed with Adobe Sign
 * </a>
 * <p>
 * This class is a helper class of {@link ContentStreamRenderer} which in turn
 * uses the {@link PDFRenderer} capabilities to render individual content streams,
 * e.g. the content streams of form XObjects.
 * </p>
 * @author mkl
 */
public class ContentStreamPageDrawer extends PageDrawer {
    public ContentStreamPageDrawer(PageDrawerParameters parameters) throws IOException {
        super(parameters);
    }

    public PDContentStream getContentStream() {
        return contentStream;
    }

    public void setContentStream(PDContentStream contentStream) {
        this.contentStream = contentStream;
    }

    //
    // Overrides that make drawPage only draw the set content stream
    // if one is set. Otherwise, these overrides delegate to PageDrawer.
    //
    @Override
    public void processPage(PDPage page) throws IOException {
        if (contentStream == null) {
            super.processPage(page);
        } else {
            processChildStream(contentStream, page);
        }
    }

    @Override
    public void showAnnotation(PDAnnotation annotation) throws IOException {
        if (contentStream == null) {
            super.showAnnotation(annotation);
        }
    }

    PDContentStream contentStream = null;
}
