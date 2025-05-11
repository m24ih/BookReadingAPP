// src/main/java/melih/bookreader/core/RsvpController.java
package melih.bookreader.core;

import javax.swing.Timer;
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

    private Consumer<String> wordDisplayCallback;
    private Supplier<String> pageContentSupplier;
    private Runnable nextPageAction;
    private Runnable endOfBookAction;
    private Runnable pageEndedAction;

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
            if (pageEndedAction != null) pageEndedAction.run();
            return;
        }

        currentWords = new ArrayList<>(Arrays.asList(pageText.trim().split("\\s+")));
        currentWords.removeIf(String::isEmpty);

        currentWordIndex = lastWordIndexForCurrentPage;
        if (currentWordIndex >= currentWords.size() && !currentWords.isEmpty()) {
            currentWordIndex = 0;
            lastWordIndexForCurrentPage = 0;
        }


        if (currentWords.isEmpty()) {
            wordDisplayCallback.accept("Page is effectively empty.");
            isActive = false;
            if (pageEndedAction != null) pageEndedAction.run();
            return;
        }

        isActive = true;
        timer.setDelay(rsvpDelayMilliseconds);
        timer.start();
        if (currentWordIndex < currentWords.size()) {
            wordDisplayCallback.accept(currentWords.get(currentWordIndex));
        } else {
            showNextWord();
        }
    }

    public void stop() {
        timer.stop();
        isActive = false;
        if (currentWords != null && currentWordIndex > 0 && currentWordIndex <= currentWords.size()) {
            lastWordIndexForCurrentPage = currentWordIndex -1;
            if (lastWordIndexForCurrentPage < 0) lastWordIndexForCurrentPage = 0;
        } else if (currentWords != null && currentWordIndex == 0 && !currentWords.isEmpty()) {
            lastWordIndexForCurrentPage = 0;
        }
    }

    private void showNextWord() {
        if (currentWords == null || currentWordIndex >= currentWords.size()) {
            lastWordIndexForCurrentPage = 0;
            if (nextPageAction != null) {
                nextPageAction.run();
            } else if (endOfBookAction != null) {
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
        this.currentWordIndex = 0;
    }
}