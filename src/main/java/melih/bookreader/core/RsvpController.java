// src/main/java/melih/bookreader/core/RsvpController.java
package melih.bookreader.core;

import javax.swing.Timer; // Timer için doğru import
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RsvpController {
    private Timer timer;
    private List<String> currentWords;
    private int currentWordIndex;
    private int rsvpDelayMilliseconds;
    private boolean isActive = false;

    private Consumer<String> wordDisplayCallback; // Kelimeyi UI'da gösterecek metod
    private Supplier<String> pageContentSupplier; // Mevcut sayfa içeriğini sağlayacak metod
    private Runnable nextPageAction;          // Bir sonraki sayfaya geçme eylemi
    private Runnable endOfBookAction;         // Kitabın sonuna gelindiğinde yapılacak eylem
    private Runnable pageEndedAction;         // Sayfa bittiğinde (ama kitap bitmediyse) yapılacak eylem

    private int lastWordIndexForCurrentPage = 0;

    public RsvpController(int initialDelay,
                          Consumer<String> wordDisplayCallback,
                          Supplier<String> pageContentSupplier,
                          Runnable nextPageAction,
                          Runnable endOfBookAction,
                          Runnable pageEndedAction) {
        this.rsvpDelayMilliseconds = initialDelay;
        this.wordDisplayCallback = wordDisplayCallback;
        this.pageContentSupplier = pageContentSupplier;
        this.nextPageAction = nextPageAction;
        this.endOfBookAction = endOfBookAction;
        this.pageEndedAction = pageEndedAction;

        this.timer = new Timer(rsvpDelayMilliseconds, e -> showNextWord());
        this.timer.setRepeats(true);
    }

    public void start() {
        String pageText = pageContentSupplier.get();
        if (pageText == null || pageText.trim().isEmpty() || pageText.equals("No content available or end of book.")) {
            wordDisplayCallback.accept("Page is empty.");
            isActive = false;
            if (pageEndedAction != null) pageEndedAction.run(); // RSVP'yi durdurmak için UI'a bilgi ver
            return;
        }

        currentWords = new ArrayList<>(Arrays.asList(pageText.trim().split("\\s+")));
        currentWords.removeIf(String::isEmpty);

        currentWordIndex = lastWordIndexForCurrentPage;
        if (currentWordIndex >= currentWords.size() && !currentWords.isEmpty()) {
            currentWordIndex = 0; // Sayfanın sonuna gelinmişse baştan başla
            lastWordIndexForCurrentPage = 0;
        }


        if (currentWords.isEmpty()) {
            wordDisplayCallback.accept("Page is effectively empty.");
            isActive = false;
            if (pageEndedAction != null) pageEndedAction.run();
            return;
        }

        isActive = true;
        timer.setDelay(rsvpDelayMilliseconds); // Mevcut hızı kullan
        timer.start();
        // İlk kelimeyi hemen göster
        if (currentWordIndex < currentWords.size()) {
            wordDisplayCallback.accept(currentWords.get(currentWordIndex));
            // currentWordIndex timer tarafından bir sonraki adımda artırılacak.
            // Ya da ilk kelime için hemen artırıp timer'ın ilkinde aynı kelimeyi göstermesini engelleyebiliriz.
            // Şimdilik timer'ın ilk tetiklemesinde ikinci kelimeye geçmesini sağlayalım.
            // VEYA: showNextWord'ü burada bir kez çağırıp, timer'ın ilk tetiklenmesini initialDelay ile yapabiliriz.
            // En temizi: currentWordIndex'i burada artırmayalım, showNextWord her zaman currentWordIndex'i gösterip sonra artırsın.
        } else {
            showNextWord(); // Sayfa sonu veya kitap sonu mantığını tetikle
        }
    }

    public void stop() {
        timer.stop();
        isActive = false;
        if (currentWords != null && currentWordIndex > 0 && currentWordIndex <= currentWords.size()) {
            lastWordIndexForCurrentPage = currentWordIndex -1; // En son gösterilen
            if (lastWordIndexForCurrentPage < 0) lastWordIndexForCurrentPage = 0;
        } else if (currentWords != null && currentWordIndex == 0 && !currentWords.isEmpty()) {
            lastWordIndexForCurrentPage = 0;
        }
    }

    private void showNextWord() {
        if (currentWords == null || currentWordIndex >= currentWords.size()) {
            lastWordIndexForCurrentPage = 0; // Sayfa bitti, bir sonraki başlangıç için sıfırla
            if (nextPageAction != null) {
                nextPageAction.run(); // Bu, ReadingFrame'de currentBook.nextPage() ve startRsvp() çağıracak
            } else if (endOfBookAction != null) { // nextPageAction null ise kitap sonu olabilir
                endOfBookAction.run();
            }
            return;
        }

        String word = currentWords.get(currentWordIndex);
        wordDisplayCallback.accept(word);
        currentWordIndex++;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setDelay(int delayMilliseconds) {
        this.rsvpDelayMilliseconds = delayMilliseconds;
        if (timer != null) {
            timer.setDelay(this.rsvpDelayMilliseconds);
        }
    }

    public int getDelay() {
        return rsvpDelayMilliseconds;
    }

    public void resetWordIndexForNewPage() {
        this.lastWordIndexForCurrentPage = 0;
        this.currentWordIndex = 0; // Mevcut kelimeyi de sıfırla
    }
}