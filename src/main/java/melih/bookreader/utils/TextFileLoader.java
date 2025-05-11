// src/main/java/com/yourapp/utils/TextFileLoader.java
package melih.bookreader.utils;

import melih.bookreader.model.Book;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextFileLoader {
    private static final int LINES_PER_PAGE = 30; // Adjustable

    public static Book loadBook(File file) {
        List<String> pages = new ArrayList<>();
        StringBuilder currentPageContent = new StringBuilder();
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentPageContent.append(line).append("\n");
                lineCount++;
                if (lineCount >= LINES_PER_PAGE) {
                    pages.add(currentPageContent.toString());
                    currentPageContent.setLength(0);
                    lineCount = 0;
                }
            }
            // Add any remaining content as the last page
            if (currentPageContent.length() > 0) {
                pages.add(currentPageContent.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Extract title from filename (simple approach)
        String title = file.getName().replaceFirst("[.][^.]+$", "");
        return new Book(title, pages);
    }
}