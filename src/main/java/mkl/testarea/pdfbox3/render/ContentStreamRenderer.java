package mkl.testarea.pdfbox3.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.rendering.RenderDestination;

/**
 * <a href="https://stackoverflow.com/questions/77002400/extract-signature-image-from-pdf-signed-with-adobe-sign">
 * Extract signature image from PDF signed with Adobe Sign
 * </a>
 * <p>
 * This class uses the {@link PDFRenderer} capabilities to render individual
 * content streams, e.g. the content streams of form XObjects.
 * </p>
 * @author mkl
 */
public class ContentStreamRenderer extends PDFRenderer {
    public ContentStreamRenderer(PDDocument document) throws NoSuchFieldException, SecurityException {
        super(document);
        pageImageField = PDFRenderer.class.getDeclaredField("pageImage");
        pageImageField.setAccessible(true);
        pageDrawerParametersConstructor = (Constructor<PageDrawerParameters>) PageDrawerParameters.class.getDeclaredConstructors()[0];
        pageDrawerParametersConstructor.setAccessible(true);
    }

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        PageDrawer pageDrawer = new ContentStreamPageDrawer(parameters);
        pageDrawer.setAnnotationFilter(getAnnotationsFilter());
        return pageDrawer;
    }

    public BufferedImage renderImage(PDPage page, PDContentStream contentStream, float scale, ImageType imageType, RenderDestination destination) throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException {
        try {
            PDRectangle bBox = contentStream.getBBox();
            Rectangle2D bounds = bBox.transform(contentStream.getMatrix()).getBounds2D();
            bBox = new PDRectangle((float)bounds.getMinX(), (float)bounds.getMinY(), (float)bounds.getWidth(), (float)bounds.getHeight());
            
            float widthPt = bBox.getWidth();
            float heightPt = bBox.getHeight();

            // PDFBOX-4306 avoid single blank pixel line on the right or on the bottom
            int widthPx = (int) Math.max(Math.floor(widthPt * scale), 1);
            int heightPx = (int) Math.max(Math.floor(heightPt * scale), 1);

            BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
            pageImageField.set(this, image);

            // use a transparent background if the image type supports alpha
            Graphics2D g = image.createGraphics();
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(scale, scale);

            RenderingHints actualRenderingHints =
                    getRenderingHints() == null ? createDefaultRenderingHints(g) : getRenderingHints();
            PageDrawerParameters parameters =
                    pageDrawerParametersConstructor.newInstance(this, page, isSubsamplingAllowed(), destination,
                            actualRenderingHints, getImageDownscalingOptimizationThreshold());
            PageDrawer drawer = createPageDrawer(parameters);
            if (drawer instanceof ContentStreamPageDrawer) {
                ((ContentStreamPageDrawer)drawer).setContentStream(contentStream);
            }
            drawer.drawPage(g, bBox);

            g.dispose();

            if (imageType != ImageType.ARGB)
            {
                int biType = -1;
                switch (imageType) {
                case ARGB:   biType = BufferedImage.TYPE_INT_ARGB; break;
                case BGR:    biType = BufferedImage.TYPE_3BYTE_BGR; break;
                case BINARY: biType = BufferedImage.TYPE_BYTE_BINARY; break;
                case GRAY:   biType = BufferedImage.TYPE_BYTE_GRAY; break;
                case RGB:    biType = BufferedImage.TYPE_INT_RGB; break;
                }
                BufferedImage newImage = 
                        new BufferedImage(image.getWidth(), image.getHeight(), biType);
                Graphics2D dstGraphics = newImage.createGraphics();
                dstGraphics.setBackground(Color.WHITE);
                dstGraphics.clearRect(0, 0, image.getWidth(), image.getHeight());
                dstGraphics.drawImage(image, 0, 0, null);
                dstGraphics.dispose();
                image = newImage;
            }

            return image;
        } finally {
            pageImageField.set(this, null);
        }
    }

    private RenderingHints createDefaultRenderingHints(Graphics2D graphics)
    {
        RenderingHints r = new RenderingHints(null);
        r.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        r.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        r.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return r;
    }

    final Field pageImageField;
    final Constructor<PageDrawerParameters> pageDrawerParametersConstructor;
}
