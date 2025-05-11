package melih.bookreader.ui;

import melih.bookreader.model.Book;
import melih.bookreader.utils.TextFileLoader;
import melih.bookreader.utils.PdfFileLoader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class ReadingFrame extends JFrame {
    private JTextArea bookTextArea;
    private JLabel rsvpLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageInfoLabel;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem openMenuItem;
    private JButton rsvpButton;

    private Book currentBook;

    private Timer rsvpTimer;
    private List<String> currentWords;
    private int currentWordIndex;
    private boolean isRsvpActive = false;
    private int rsvpDelayMilliseconds = 500;
    private JSlider speedSlider;
    private JLabel speedLabel;

    private JPanel centerPanel;
    private CardLayout cardLayout;

    private int lastRsvpWordIndexForCurrentPage = 0; // Mevcut sayfa için son kalınan RSVP kelime indeksi

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        initRsvpTimer();
        addListeners();
        updateUI();
    }

    private void initComponents() {
        bookTextArea = new JTextArea();
        bookTextArea.setEditable(false);
        bookTextArea.setLineWrap(true);
        bookTextArea.setWrapStyleWord(true);
        bookTextArea.setFont(new Font("Serif", Font.PLAIN, 16));

        rsvpLabel = new JLabel("", SwingConstants.CENTER);
        rsvpLabel.setFont(new Font("Serif", Font.BOLD, 36));

        prevButton = new JButton("Previous Page");
        nextButton = new JButton("Next Page");
        pageInfoLabel = new JLabel("Page: -/-");
        rsvpButton = new JButton("Start RSVP");

        speedSlider = new JSlider(JSlider.HORIZONTAL, 50, 1000, rsvpDelayMilliseconds);
        speedSlider.setMajorTickSpacing(250);
        speedSlider.setMinorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setToolTipText("Adjust RSVP reading speed (delay between words in ms)");

        speedLabel = new JLabel();

        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        openMenuItem = new JMenuItem("Open Book...");
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        centerPanel.add(new JScrollPane(bookTextArea), "NORMAL_VIEW");
        centerPanel.add(rsvpLabel, "RSVP_VIEW");
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(prevButton);
        bottomPanel.add(pageInfoLabel);
        bottomPanel.add(nextButton);
        add(bottomPanel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(rsvpButton);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(new JLabel("Speed:"));
        topPanel.add(speedSlider);
        topPanel.add(speedLabel);
        add(topPanel, BorderLayout.NORTH);
    }

    private void updateRsvpSpeed() {
        rsvpDelayMilliseconds = speedSlider.getValue();
        if (rsvpTimer != null) {
            rsvpTimer.setDelay(rsvpDelayMilliseconds);
        }
        double wpm = (60.0 * 1000.0) / rsvpDelayMilliseconds;
        speedLabel.setText(String.format("%.0f wpm (%dms)", wpm, rsvpDelayMilliseconds));
    }

    private void initRsvpTimer() {
        updateRsvpSpeed();
        rsvpTimer = new Timer(rsvpDelayMilliseconds, e -> showNextRsvpWord());
        rsvpTimer.setRepeats(true);
    }

    private void addListeners() {
        openMenuItem.addActionListener(e -> openFile());

        nextButton.addActionListener(e -> {
            if (isRsvpActive) toggleRsvp();
            if (currentBook != null && currentBook.nextPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
                lastRsvpWordIndexForCurrentPage = 0; // Yeni sayfa, RSVP indeksini sıfırla
            }
        });

        prevButton.addActionListener(e -> {
            if (isRsvpActive) toggleRsvp();
            if (currentBook != null && currentBook.previousPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
                lastRsvpWordIndexForCurrentPage = 0; // Yeni sayfa, RSVP indeksini sıfırla
            }
        });

        rsvpButton.addActionListener(e -> toggleRsvp());

        speedSlider.addChangeListener(e -> updateRsvpSpeed());
    }

    private void toggleRsvp() {
        if (currentBook == null && !isRsvpActive) {
            JOptionPane.showMessageDialog(this, "Please open a book first.", "RSVP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isRsvpActive = !isRsvpActive;

        if (isRsvpActive) {
            startRsvp();
            if (currentWords == null || currentWords.isEmpty()) {
                isRsvpActive = false; // Başlatılamadı, geri al
                // startRsvp zaten mesaj vermiş olabilir veya rsvpLabel'a yazmış olabilir.
                return;
            }
            cardLayout.show(centerPanel, "RSVP_VIEW");
            rsvpButton.setText("Stop RSVP");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            openMenuItem.setEnabled(false);
            speedSlider.setEnabled(false);
        } else {
            stopRsvp(); // Bu, lastRsvpWordIndexForCurrentPage'i güncelleyecek
            cardLayout.show(centerPanel, "NORMAL_VIEW");
            rsvpButton.setText("Start RSVP");
            updatePageNavigationButtons();
            openMenuItem.setEnabled(true);
            speedSlider.setEnabled(true);
            if (currentBook != null) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
            }
        }
    }

    private void startRsvp() {
        if (currentBook == null || currentBook.getCurrentPageContent() == null) {
            JOptionPane.showMessageDialog(this, "No book or page content to start RSVP.", "RSVP Error", JOptionPane.ERROR_MESSAGE);
            currentWords = null;
            return;
        }

        String currentPageText = currentBook.getCurrentPageContent();
        if (currentPageText.trim().isEmpty() || currentPageText.equals("No content available or end of book.")) {
            rsvpLabel.setText("Page is empty or unavailable.");
            currentWords = null;
            return;
        }

        currentWords = new ArrayList<>(Arrays.asList(currentPageText.trim().split("\\s+")));
        currentWords.removeIf(String::isEmpty);

        currentWordIndex = lastRsvpWordIndexForCurrentPage;

        if (currentWordIndex >= currentWords.size() && !currentWords.isEmpty()) {
            // Eğer kaydedilen indeks sayfanın sonundaysa, o sayfa için baştan başla.
            // Otomatik sayfa geçişi showNextRsvpWord içinde ele alınacak.
            currentWordIndex = 0;
            lastRsvpWordIndexForCurrentPage = 0; // Bu sayfa için sıfırla
        }

        if (currentWords.isEmpty()) {
            rsvpLabel.setText("Page is effectively empty.");
            return;
        }

        rsvpTimer.setDelay(rsvpDelayMilliseconds);
        rsvpTimer.start();
        // İlk kelimeyi hemen göstermek için timer'ın ilk tetiklenmesini beklemek yerine
        // direkt çağırabiliriz, ancak timer zaten hemen tetiklenecek şekilde ayarlıysa gerek yok.
        // Eğer ilk kelime hemen gösterilmiyorsa:
        if (currentWordIndex < currentWords.size()) { // Hala gösterilecek kelime varsa
            rsvpLabel.setText(currentWords.get(currentWordIndex));
            // currentWordIndex++; // Timer bir sonraki için zaten artıracak, ilk kelime için burada artırmaya gerek yok
        } else { // Kalınan yerden devam edilecek kelime kalmadıysa (örn. sayfa sonu ve index sıfırlanmadıysa)
            showNextRsvpWord(); // Sayfa sonu mantığını tetikle
        }
    }

    private void stopRsvp() {
        if (rsvpTimer != null) {
            rsvpTimer.stop();
        }
        // O an gösterilmekte olan kelimenin indeksini kaydet
        if (currentWords != null && currentWordIndex > 0 && currentWordIndex <= currentWords.size()) {
            lastRsvpWordIndexForCurrentPage = currentWordIndex -1; // Son gösterilen kelimenin indeksi
        } else if (currentWords != null && currentWordIndex == 0 && !currentWords.isEmpty()) {
            lastRsvpWordIndexForCurrentPage = 0; // Eğer hiç kelime gösterilmeden durdurulursa
        }
        // Eğer currentWordIndex zaten currentWords.size() ise, sayfanın sonu demektir,
        // bir sonraki başlangıçta startRsvp bunu ele alacaktır.
    }

    private void showNextRsvpWord() {
        if (currentWords == null || currentWordIndex >= currentWords.size()) {
            if (currentBook != null && currentBook.nextPage()) {
                updatePageInfo();
                lastRsvpWordIndexForCurrentPage = 0; // Yeni sayfa, indeksi sıfırla
                startRsvp();
            } else {
                rsvpLabel.setText("End of Book");
                stopRsvp();
                if (isRsvpActive) {
                    toggleRsvp();
                }
            }
            return;
        }

        String word = currentWords.get(currentWordIndex);
        rsvpLabel.setText(word);
        currentWordIndex++;
    }

    private void openFile() {
        if (isRsvpActive) {
            toggleRsvp();
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Book File");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(pdfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();
            currentBook = null;

            if (fileName.endsWith(".txt")) {
                currentBook = TextFileLoader.loadBook(selectedFile);
            } else if (fileName.endsWith(".pdf")) {
                currentBook = PdfFileLoader.loadBook(selectedFile);
            } else {
                if (fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                    boolean isSupportedByFilter = false;
                    for (String ext : selectedFilter.getExtensions()) {
                        if (fileName.endsWith("." + ext)) {
                            isSupportedByFilter = true;
                            break;
                        }
                    }
                    // Eğer seçilen filtre .txt veya .pdf değilse ve dosya onlardan biri değilse hata ver.
                    // Ya da "Tüm Dosyalar" seçiliyse ve dosya uzantısı .txt veya .pdf değilse.
                    if (!isSupportedByFilter && (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf"))) {
                        JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    } else if (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf")){
                        // Filtreye uyuyor ama yine de desteklenmeyen bir durum (bu olmamalı)
                        JOptionPane.showMessageDialog(this, "Selected file is not a .txt or .pdf file despite the filter.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else { // "Tüm Dosyalar" filtresi seçili
                    if (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf")){
                        JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            if (currentBook != null) {
                String windowTitle = currentBook.getTitle();
                if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                    windowTitle += " by " + currentBook.getAuthor();
                }
                setTitle(windowTitle + " - Book Reader");
                lastRsvpWordIndexForCurrentPage = 0; // Yeni kitap, RSVP indeksini sıfırla
                updateUI();
            } else {
                if (fileName.endsWith(".txt") || fileName.endsWith(".pdf")) { // Sadece desteklenen tipler için yükleme hatası göster
                    JOptionPane.showMessageDialog(this, "Could not load book from " + selectedFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updatePageInfo() {
        if (currentBook != null) {
            String pageInfo = "Page: " + (currentBook.getCurrentPageIndex() + 1) + "/" + currentBook.getTotalPages();
            if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                pageInfo += "  |  Author: " + currentBook.getAuthor();
            }
            pageInfoLabel.setText(pageInfo);
        } else {
            pageInfoLabel.setText("Page: -/-");
        }
    }

    private void updateUI() {
        if (isRsvpActive) {
            toggleRsvp();
        } else {
            if (cardLayout != null && centerPanel != null) {
                cardLayout.show(centerPanel, "NORMAL_VIEW");
            }
        }

        if (currentBook != null) {
            bookTextArea.setText(currentBook.getCurrentPageContent());
            bookTextArea.setCaretPosition(0);
            updatePageInfo();
            rsvpButton.setEnabled(true);
            speedSlider.setEnabled(true);
        } else {
            bookTextArea.setText("Open a book using File > Open Book...");
            updatePageInfo();
            rsvpButton.setEnabled(false);
            speedSlider.setEnabled(false);
        }
        updatePageNavigationButtons();
    }

    private void updatePageNavigationButtons() {
        if (currentBook != null && !isRsvpActive) {
            prevButton.setEnabled(currentBook.getCurrentPageIndex() > 0);
            nextButton.setEnabled(currentBook.getCurrentPageIndex() < currentBook.getTotalPages() - 1);
        } else {
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReadingFrame().setVisible(true));
    }
}