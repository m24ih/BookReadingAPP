package melih.bookreader.utils;

import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.domain.Spine;
import io.documentnode.epub4j.domain.SpineReference;
import io.documentnode.epub4j.domain.TableOfContents;
import io.documentnode.epub4j.domain.TocEntry;
import io.documentnode.epub4j.epub.EpubReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets; // Genellikle UTF-8 iyidir
import java.util.ArrayList;
import java.util.List;

public class EpubLoader {

    public static melih.bookreader.model.Book loadEpubBook(File epubFile) {
        try (InputStream epubInputStream = new FileInputStream(epubFile)) {
            EpubReader epubReader = new EpubReader();
            Book epub = epubReader.read(epubInputStream); // epub4j'de read() metodu

            String title = epub.getTitle();
            if (title == null || title.isEmpty()) {
                // Epub'da başlık yoksa dosya adından al
                if (!epub.getMetadata().getTitles().isEmpty()) {
                    title = epub.getMetadata().getTitles().get(0);
                } else {
                    title = epubFile.getName().replaceFirst("[.][^.]+$", "");
                }
            }

            List<String> pages = new ArrayList<>();
            Spine spine = epub.getSpine();

            // SpineReference üzerinden kaynaklara erişim
            for (SpineReference spineReference : spine.getSpineReferences()) {
                Resource resource = spineReference.getResource();
                try (InputStream contentStream = resource.getInputStream()) {
                    String htmlContent = new String(contentStream.readAllBytes(), resource.getMediaType().getDefaultEncoding());

                    String plainText = htmlContent.replaceAll("<[^>]*>", "").trim();
                    plainText = plainText.replaceAll(" ", " ")
                            .replaceAll("&", "&")
                            .replaceAll("<", "<")
                            .replaceAll(">", ">")
                            .replaceAll(""", "\"")
                                         .replaceAll("'", "'");
                    plainText = plainText.replaceAll("\\s+", " ");

                    if (!plainText.isEmpty()) {
                        pages.add(plainText);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading resource " + resource.getHref() + ": " + e.getMessage());
                }
            }

            if (pages.isEmpty()) {
                System.err.println("No content pages found in EPUB spine.");
                return null;
            }

            javabookreader.model.Book myAppBook = new javabookreader.model.Book(title, pages);
            if (!epub.getMetadata().getAuthors().isEmpty()) {
                myAppBook.setAuthor(epub.getMetadata().getAuthors().get(0).getFirstname() + " " + epub.getMetadata().getAuthors().get(0).getLastname());
            }
            return myAppBook;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void printTableOfContents(Book epubBook) {
        TableOfContents toc = epubBook.getTableOfContents();
        if (toc != null) {
            System.out.println("Table of Contents:");
            printTocEntries(toc.getTocReferences(), 0); // getTocReferences() hala kullanılabilir veya getTocEntries()
        }
    }

    // TocEntry için güncellenmiş metod
    private static void printTocEntries(List<TocEntry> tocEntries, int depth) {
        if (tocEntries == null) {
            return;
        }
        for (TocEntry tocEntry : tocEntries) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append("  ");
            }
            System.out.println(indent.toString() + tocEntry.getTitle());
            if (tocEntry.getResource() != null) {
                // System.out.println(indent.toString() + "  --> " + tocEntry.getResource().getHref());
            }
            printTocEntries(tocEntry.getChildren(), depth + 1);
        }
    }
}