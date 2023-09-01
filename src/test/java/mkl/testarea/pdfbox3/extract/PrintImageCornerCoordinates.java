package mkl.testarea.pdfbox3.extract;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.examples.util.PrintImageLocations;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * @author mkl
 */
class PrintImageCornerCoordinates {
    /**
     * This helper class extends the {@link PrintImageLocations} PDFBox utility to print the
     * default user space coordinates of the bitmap images displayed in a {@link PDDocument}.
     */
    static class UserSpaceCoordinatesPrinter extends PrintImageLocations {
        public UserSpaceCoordinatesPrinter() throws IOException {
            super();
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();
            if (OperatorName.DRAW_OBJECT.equals(operation)) {
                COSName objectName = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(objectName);
                if (xobject instanceof PDImageXObject) {
                    System.out.println("\nFound image [" + objectName.getName() + "]");
                    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                    System.out.print("Corner coordinates in user space:");
                    for (Vector corner : UNIT_SQUARE_CORNERS) {
                        System.out.print(" " + ctm.transform(corner));
                    }
                    System.out.println();
                } else if(xobject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject)xobject;
                    showForm(form);
                }
            } else {
                super.processOperator(operator, operands);
            }
        }

        static void print(PDDocument document) throws IOException {
            UserSpaceCoordinatesPrinter printer = new UserSpaceCoordinatesPrinter();
            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                pageNum++;
                System.out.println("\n\nProcessing page: " + pageNum);
                System.out.println("Media box: " + page.getMediaBox());
                System.out.println("Page rotation: " + page.getRotation());
                printer.processPage(page);
            }
        }

        final static List<Vector> UNIT_SQUARE_CORNERS = List.of(
                new Vector(0, 0), new Vector(0, 1), new Vector(1, 0), new Vector(1, 1));
    }

    /**
     * <a href="https://stackoverflow.com/questions/77010598/problems-determining-image-positioning-using-pdfbox">
     * Problems determining image positioning using PDFBox
     * </a>
     * <p>
     * This test shows for an arbitrary PDF how to extract the default user space coordinates
     * of the corners of the bitmap images.
     * </p>
     */
    @Test
    void testUserCoordinatesInput() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/meta/input.pdf");
            PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(resource))
        ) {
            UserSpaceCoordinatesPrinter.print(document);
        }
    }
}
