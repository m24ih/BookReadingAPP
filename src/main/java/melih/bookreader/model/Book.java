// src/main/java/melih/bookreader/model/Book.java
package melih.bookreader.model;

import java.util.List;

public class Book {
    private String title;
    private String author;
    private List<String> pages;
    private int currentPageIndex;

    public Book(String title, List<String> pages) {
        this.title = title;
        this.pages = pages;
        this.currentPageIndex = 0;
        this.author = "Unknown Author"; // Varsayılan değer
    }

    // Getter'lar...
    public String getTitle() { return title; }
    public String getAuthor() { return author; } // Author için getter
    public List<String> getPages() { return pages; }
    public int getCurrentPageIndex() { return currentPageIndex; }

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

    public int getTotalPages() {
        return pages != null ? pages.size() : 0;
    }

    // Setter
    public void setAuthor(String author) { // Author için setter
        this.author = author;
    }
}