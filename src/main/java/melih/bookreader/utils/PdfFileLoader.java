package melih.bookreader.utils;

import melih.bookreader.model.Book;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfFileLoader {

    public static Book loadBook(File file) {
        List<String> pages = new ArrayList<>();
        String title = file.getName().replaceFirst("[.][^.]+$", "");

        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            int numberOfPages = document.getNumberOfPages();

            for (int i = 1; i <= numberOfPages; i++) {
                pdfStripper.setStartPage(i);
                pdfStripper.setEndPage(i);
                String pageText = pdfStripper.getText(document);
                pages.add(pageText);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Consider throwing a custom exception or returning null with a log
            return null;
        }

        return new Book(title, pages);
    }
}