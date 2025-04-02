package mkl.testarea.pdfbox3.extract;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;

/**
 * <p>
 * This class extracts and displays the marked content structure of a document.
 * It essentially is a generalized copy of the {@link mkl.testarea.pdfbox2.extract.ExtractMarkedContent}
 * test in the PDFBox 2 playground.
 * </p>
 * 
 * @author mkl
 */
public class MarkedContentExtractor {
    public static void main(String[] args) throws IOException {
        for (String arg: args) {
            System.out.printf("***\n*** %s\n***\n\n", arg);
            final File file = new File(arg);
            if (file.exists()) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    MarkedContentExtractor extractor = new MarkedContentExtractor(document);
                    extractor.extract();
                    extractor.show();
                }
            } else
                System.err.println("!!! File does not exist: " + file);
        }
    }

    public MarkedContentExtractor(PDDocument document) {
        this.document = document;
    }

    Map<PDPage, Map<Integer, PDMarkedContent>> extract() throws IOException {
        Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();

        for (PDPage page : document.getPages()) {
            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
            markedContents.put(page, theseMarkedContents);
            for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                theseMarkedContents.put(markedContent.getMCID(), markedContent);
            }
        }

        this.markedContents = markedContents;
        return markedContents;
    }

    void show() {
        if (document == null || markedContents == null) {
            System.out.println("--");
            return;
        }

        PDStructureNode root = document.getDocumentCatalog().getStructureTreeRoot();

        showStructure(root, markedContents);
    }

    void showStructure(PDStructureNode node, Map<PDPage, Map<Integer, PDMarkedContent>> markedContents) {
        String structType = null;
        PDPage page = null;
        if (node instanceof PDStructureElement) {
            PDStructureElement element = (PDStructureElement) node;
            structType = element.getStructureType();
            page = element.getPage();
        }
        Map<Integer, PDMarkedContent> theseMarkedContents = markedContents.get(page);
        System.out.printf("<%s>\n", structType);
        for (Object object : node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        showStructure(PDStructureNode.create((COSDictionary) base), markedContents);
                    } else if (base instanceof COSNumber) {
                        showContent(((COSNumber)base).intValue(), theseMarkedContents);
                    } else {
                        System.out.printf("?%s\n", base);
                    }
                }
            } else if (object instanceof PDStructureNode) {
                showStructure((PDStructureNode) object, markedContents);
            } else if (object instanceof Integer) {
                showContent((Integer)object, theseMarkedContents);
            } else {
                System.out.printf("?%s\n", object);
            }

        }
        System.out.printf("</%s>\n", structType);
    }

    /**
     * @see #showStructure(PDStructureNode, Map)
     * @see #testExtractTestWPhromma()
     */
    void showContent(int mcid, Map<Integer, PDMarkedContent> theseMarkedContents) {
        PDMarkedContent markedContent = theseMarkedContents != null ? theseMarkedContents.get(mcid) : null;
        List<Object> contents = markedContent != null ? markedContent.getContents() : Collections.emptyList();
        StringBuilder textContent =  new StringBuilder();
        for (Object object : contents) {
            if (object instanceof TextPosition) {
                textContent.append(((TextPosition)object).getUnicode());
            } else {
                textContent.append("?" + object);
            }
        }
        System.out.printf("%s\n", textContent);
    }

    final PDDocument document;
    Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = null;
}
