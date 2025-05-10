// src/main/java/com/yourapp/model/Book.java
package melih.bookreader.model;

import java.util.List;

public class Book {
    private String title;
    private String author; // Optional for now
    private List<String> pages; // Content, pre-paginated
    private int currentPageIndex;

    public Book(String title, List<String> pages) {
        this.title = title;
        this.pages = pages;
        this.currentPageIndex = 0;
    }

    public String getTitle() {
        return title;
    }

    public String getCurrentPageContent() {
        if (pages != null && !pages.isEmpty() && currentPageIndex >= 0 && currentPageIndex < pages.size()) {
            return pages.get(currentPageIndex);
        }
        return "No content available or end of book.";
    }

    public boolean nextPage() {
        if (currentPageIndex < pages.size() - 1) {
            currentPageIndex++;
            return true;
        }
        return false;
    }

    public boolean previousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            return true;
        }
        return false;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public int getTotalPages() {
        return pages != null ? pages.size() : 0;
    }
}