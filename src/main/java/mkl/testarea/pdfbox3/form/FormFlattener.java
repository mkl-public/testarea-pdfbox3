package mkl.testarea.pdfbox3.form;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

/**
 * <a href="https://stackoverflow.com/questions/79786529/remove-signature-and-add-picture-of-signature-instead">
 * Remove signature and add picture of signature instead
 * </a>
 * <p>
 * This class flattens the forms in the given files.
 * </p>
 */
public class FormFlattener {
    public static void main(String[] args) throws IOException {
        for (String arg: args) {
            System.out.printf("***\n*** %s\n***\n\n", arg);
            final File file = new File(arg);
            if (file.exists()) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDAcroForm form = document.getDocumentCatalog().getAcroForm();
                    if (form != null) {
                        form.flatten();
                        System.out.println("    Form has been flattened.");
                        document.save(file.getPath() + "-flatten.pdf");
                        System.out.println("    Flattened form has been saved.");
                    } else {
                        System.err.println("!!! File has no AcroForm definition: " + file);
                    }
                }
            } else {
                System.err.println("!!! File does not exist: " + file);
            }
        }
    }
}
