package mkl.testarea.pdfbox3.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CopyFormattedPageText {
    final static File RESULT_FOLDER = new File("target/test-outputs", "content");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        RESULT_FOLDER.mkdirs();
    }

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
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

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
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

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
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

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
    @Test
    void testCopyAndChangeFromTestDocumentSigned() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/render/test_document_signed.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyText(source, target, list -> searchAndReplace(list, "Test", "Port"));
            target.save(new File(RESULT_FOLDER, "test_document_signed-TextCopyChange.pdf"));
        }
    }

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
    @Test
    void testCopyAndChangeAlternativelyFromTestDocumentSigned() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("/mkl/testarea/pdfbox3/render/test_document_signed.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyText(source, target, list -> searchAndReplaceAlternative(list, "DOCUMENT", "COSTUME"));
            target.save(new File(RESULT_FOLDER, "test_document_signed-TextCopyChangeAlt.pdf"));
        }
    }

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
    void copyText(PDDocument source, PDDocument target) throws IOException {
        copyText(source, target, null);
    }

    /** @see #copyText(PDDocument, int, PDDocument, PDPage, Consumer) */
    void copyText(PDDocument source, PDDocument target, Consumer<List<TextPosition>> updater) throws IOException {
        for (int i = 0; i < source.getNumberOfPages(); i++) {
            PDPage sourcePage = source.getPage(i);
            PDPage targetPage = null;
            if (i < target.getNumberOfPages())
                targetPage = target.getPage(i);
            else
                target.addPage(targetPage = new PDPage(sourcePage.getMediaBox()));
            copyText(source, i, target, targetPage, updater);
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
    void copyText(PDDocument source, int sourcePageNumber, PDDocument target, PDPage targetPage, Consumer<List<TextPosition>> updater) throws IOException {
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

        if (updater != null)
            updater.accept(allTextPositions);

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

    /**
     * <a href="https://stackoverflow.com/questions/77706995/how-can-i-copy-extracted-text-page-by-page-in-a-document-to-a-new-pdf-document-w">
     * How can I copy extracted text page by page in a document to a new PDF document with PDFBox?
     * </a>
     * <p>
     * This method replaces a search word in a list of {@link TextPosition} objects
     * by replacing the letters in each instance by the same number of letters from
     * the replacement word (as long as available). This is appropriate if the word
     * is especially formatted (e.g. spaced out) and this special formatting shall be
     * kept.
     * </p>
     * @see #searchAndReplaceAlternative(List, String, String)
     */
    void searchAndReplace(List<TextPosition> textPositions, String searchWord, String replacement) {
        if (searchWord == null || searchWord.length() == 0)
            return;

        int candidatePosition = 0;
        String candidate = "";
        for (int i = 0; i < textPositions.size(); i++) {
            candidate += textPositions.get(i).getUnicode();
            if (!searchWord.startsWith(candidate)) {
                candidate = "";
                candidatePosition = i+1;
            } else if (searchWord.length() == candidate.length()) {
                for (int j = 0; j < searchWord.length();) {
                    TextPosition textPosition = textPositions.get(candidatePosition);
                    int length = textPosition.getUnicode().length();
                    String replacementHere = "";
                    if (length > 0 && j < replacement.length()) {
                        int end = j + length;
                        if (end > replacement.length())
                            end = replacement.length();
                        replacementHere = replacement.substring(j, end);
                        
                    }
                    TextPosition newTextPosition = new TextPosition(textPosition.getRotation(),
                            textPosition.getPageWidth(), textPosition.getPageHeight(), textPosition.getTextMatrix(),
                            textPosition.getEndX(), textPosition.getEndY(), textPosition.getHeight(),
                            textPosition.getIndividualWidths()[0], textPosition.getWidthOfSpace(),
                            replacementHere,
                            textPosition.getCharacterCodes(), textPosition.getFont(),
                            textPosition.getFontSize(), (int) textPosition.getFontSizeInPt());
                    textPositions.set(candidatePosition, newTextPosition);
                    candidatePosition++;
                    j += length;
                }
            }
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/77706995/how-can-i-copy-extracted-text-page-by-page-in-a-document-to-a-new-pdf-document-w">
     * How can I copy extracted text page by page in a document to a new PDF document with PDFBox?
     * </a>
     * <p>
     * This method replaces a search word in a list of {@link TextPosition} objects
     * by replacing the letters in the first instance by the whole replacement word
     * and removing the other instances. This is appropriate if the word is not
     * formatted (e.g. spaced out) and shall be printed naturally.
     * </p>
     * @see #searchAndReplace(List, String, String)
     */
    void searchAndReplaceAlternative(List<TextPosition> textPositions, String searchWord, String replacement) {
        if (searchWord == null || searchWord.length() == 0)
            return;

        int candidatePosition = 0;
        String candidate = "";
        for (int i = 0; i < textPositions.size(); i++) {
            candidate += textPositions.get(i).getUnicode();
            if (!searchWord.startsWith(candidate)) {
                candidate = "";
                candidatePosition = i+1;
            } else if (searchWord.length() == candidate.length()) {
                TextPosition textPosition = textPositions.get(candidatePosition);
                TextPosition newTextPosition = new TextPosition(textPosition.getRotation(),
                        textPosition.getPageWidth(), textPosition.getPageHeight(), textPosition.getTextMatrix(),
                        textPosition.getEndX(), textPosition.getEndY(), textPosition.getHeight(),
                        textPosition.getIndividualWidths()[0], textPosition.getWidthOfSpace(),
                        replacement,
                        textPosition.getCharacterCodes(), textPosition.getFont(),
                        textPosition.getFontSize(), (int) textPosition.getFontSizeInPt());
                textPositions.set(candidatePosition, newTextPosition);

                while (i > candidatePosition) {
                    textPositions.remove(i--);
                }
                candidatePosition++;
            }
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/77939174/how-to-fix-text-positioning-during-recreation-of-pdf">
     * How to Fix text positioning during recreation of pdf
     * </a>
     * <br/>
     * <a href="https://www.princexml.com/howcome/2016/samples/invoice/index.pdf">
     * index.pdf
     * </a> as "InvoiceYesLogic.pdf"
     * <p>
     * This is the code of the OP with simplifications and additions due to
     * incomplete code provided by the OP. After fixing it to use the correct
     * text matrix and making <code>getActualFontSize</code> return 1 all the
     * time, it works. Also the document looks very much like the screenshot
     * of the document the OP provided.
     * </p>
     * <p>
     * But even without these changes the result looked different from the OP's
     * screen shots. Thus, a major issue most likely still is in the code pieces
     * not provided by the OP.
     * </p>
     * 
     * @see #copyTextLikeNitishKumar(PDDocument, int, PDDocument, PDPage)
     */
    @Test
    void testCopyLikeNitishKumarFromInvoiceYesLogic() throws IOException {
        try (
            InputStream resource = getClass().getResourceAsStream("InvoiceYesLogic.pdf");
            PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(resource));
            PDDocument target = new PDDocument()
        ) {
            copyTextLikeNitishKumar(source, target);
            target.save(new File(RESULT_FOLDER, "InvoiceYesLogic-TextCopyLikeNitishKumar.pdf"));
        }
    }

    /** @see #copyTextLikeNitishKumar(PDDocument, int, PDDocument, PDPage) */
    void copyTextLikeNitishKumar(PDDocument source, PDDocument target) throws IOException {
        for (int i = 0; i < source.getNumberOfPages(); i++) {
            PDPage sourcePage = source.getPage(i);
            PDPage targetPage = null;
            if (i < target.getNumberOfPages())
                targetPage = target.getPage(i);
            else
                target.addPage(targetPage = new PDPage(sourcePage.getMediaBox()));

            PDResources targetResources = targetPage.getResources();
            if (targetResources == null)
                targetPage.setResources(targetResources = new PDResources());

            copyTextLikeNitishKumar(source, i, target, targetPage);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/77939174/how-to-fix-text-positioning-during-recreation-of-pdf">
     * How to Fix text positioning during recreation of pdf
     * </a>
     * <p>
     * This is the code of the OP with simplifications and additions due to
     * incomplete code provided by the OP. After fixing it to use the correct
     * text matrix and making <code>getActualFontSize</code> return 1 all the
     * time, it works. But even without these changes the result looked
     * different from the OP's screen shots. Thus, a major issue most likely
     * still is in the code pieces not provided by the OP.
     * </p>
     * @see #testCopyLikeNitishKumarFromInvoiceYesLogic()
     */
    void copyTextLikeNitishKumar(PDDocument source, int sourcePageNumber, PDDocument target, PDPage targetPage) throws IOException {
        List<TextPositionsInfo> textPositions = new ArrayList<>();
        PDFTextStripper pdfTextStripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
//                textPositionPDGraphicsStatesMap.put(text, getGraphicsState());
                PDGraphicsState state = getGraphicsState();
                PDTextState textState = state.getTextState();
                float fontSize = textState.getFontSize();
                float horizontalScaling = textState.getHorizontalScaling() / 100f;
                float charSpacing = textState.getCharacterSpacing();

                // put the text state parameters into matrix form
                Matrix parameters = new Matrix(
                           fontSize * horizontalScaling, 0, // 0
                           0, fontSize,                     // 0
                           0, textState.getRise());         // 1
               
               // text rendering matrix (text space -> device space)
               Matrix ctm = state.getCurrentTransformationMatrix();
               Matrix textRenderingMatrix = parameters.multiply(/*text*/state.getTextMatrix()).multiply(ctm);

               TextPositionsInfo txtInfo = new TextPositionsInfo();
               txtInfo.xDir = text.getXDirAdj();
               txtInfo.yDir = text.getYDirAdj();
               txtInfo.x =  textRenderingMatrix.getTranslateX();
               txtInfo.y = textRenderingMatrix.getTranslateY();
               txtInfo.textMatrix = textRenderingMatrix;
               txtInfo.height= text.getHeightDir();
               txtInfo.width = text.getWidthDirAdj(); 
               txtInfo.unicode = text.getUnicode();
               txtInfo.fontName = text.getFont().getFontDescriptor().getFontName();
               txtInfo.fontSize = getActualFontSize(text, getGraphicsState());
               /*pdfGraphicContent.*/textPositions.add(txtInfo);

// font provisioning not provided by OP. Simple stub.
targetPage.getResources().put(COSName.getPDFName(txtInfo.fontName), text.getFont());
            }

            // not provided by OP. Simple stub
            private float getActualFontSize(TextPosition text, PDGraphicsState graphicsState) {
                return 1;
            }
        };
        pdfTextStripper.setStartPage(sourcePageNumber + 1);
        pdfTextStripper.setEndPage(sourcePageNumber + 1);
        pdfTextStripper.getText(source);

        try (PDPageContentStream contentStream = new PDPageContentStream(target, targetPage, AppendMode.APPEND, true, true)) {
            addTextCharByChar(textPositions, targetPage, contentStream);
        }
    }

    /** @see #copyTextLikeNitishKumar(PDDocument, int, PDDocument, PDPage) */
    private void addTextCharByChar(/*String string,*/ List<TextPositionsInfo> textinfoList, /*TextBBoxinfo textBBoxinfo,*/ PDPage page, PDPageContentStream currentContentStream) throws IOException {
        PDResources res = page.getResources();

        currentContentStream.beginText(); 
//        if (textBBoxinfo._ElementType.toLowerCase().equals("h2")) {
            currentContentStream.beginMarkedContent(COSName.P);
            for(TextPositionsInfo textInfo : textinfoList) {
                PDFont font = getFont(res, textInfo.fontName);
                currentContentStream.setFont(font, textInfo.fontSize);
                Matrix _tm = textInfo.textMatrix;
                currentContentStream.newLineAtOffset(_tm.getTranslateX(), _tm.getTranslateY());
                currentContentStream.setTextMatrix(_tm);
                currentContentStream.showText(textInfo.unicode);
            }
            currentContentStream.endMarkedContent();
//            addContentToCurrentSection(COSName.P, StandardStructureTypes.H2);
//        } else if (textBBoxinfo._ElementType.toLowerCase().equals("h1")) {
//            beginMarkedConent(COSName.P);
//            for(TextPositionsInfo textInfo : textinfoList) {
//                PDFont font = getFont(res, textInfo.fontName);
//                currentContentStream.setFont(font, textInfo.fontSize);
//                currentContentStream.newLineAtOffset(textInfo.textMatrix.getTranslateX(), textInfo.textMatrix.getTranslateY());
//                currentContentStream.setTextMatrix(textInfo.textMatrix);
//                currentContentStream.showText(textInfo.unicode);
//            }
//            currentContentStream.endMarkedContent();
//            addContentToCurrentSection(COSName.P, StandardStructureTypes.H1);
//        }
        currentContentStream.endText();
    }

    /** @see #copyTextLikeNitishKumar(PDDocument, int, PDDocument, PDPage) */
    // not provided by OP. Simple stub
    private PDFont getFont(PDResources res, String fontName) throws IOException {
        return res.getFont(COSName.getPDFName(fontName));
    }

    /** @see #copyTextLikeNitishKumar(PDDocument, int, PDDocument, PDPage) */
    // not provided by OP. Simple stub
    class TextPositionsInfo {
        float xDir, yDir, x, y, height, width, fontSize;
        Matrix textMatrix;
        String unicode, fontName;
    }

}
