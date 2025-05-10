package melih.bookreader.ui;

import melih.bookreader.model.Book;
import melih.bookreader.utils.TextFileLoader;
import melih.bookreader.utils.PdfFileLoader;

import javax.swing.*;
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
    private final int RSVP_DELAY = 500;

    // CardLayout ve onu kullanan panel için referanslar
    private JPanel centerPanel;
    private CardLayout cardLayout;

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        addListeners();
        initRsvpTimer();

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
        // rsvpLabel.setPreferredSize(new Dimension(600, 100)); // Gerekirse CardLayout'ta otomatik boyutlanır
        // rsvpLabel.setVisible(false); // CardLayout yönetecek

        prevButton = new JButton("Previous Page");
        nextButton = new JButton("Next Page");
        pageInfoLabel = new JLabel("Page: -/-");
        rsvpButton = new JButton("Start RSVP");

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

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(rsvpButton);
        add(topPanel, BorderLayout.NORTH);
    }

    private void initRsvpTimer() {
        rsvpTimer = new Timer(RSVP_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextRsvpWord();
            }
        });
        rsvpTimer.setRepeats(true);
    }

    private void addListeners() {
        openMenuItem.addActionListener(e -> openFile());

        nextButton.addActionListener(e -> {
            if (isRsvpActive) toggleRsvp(); // RSVP aktifse normale dön ve durdur
            if (currentBook != null && currentBook.nextPage()) {
                updateUI(); // Normal UI'ı güncelle, bu da RSVP durumunu sıfırlar
            }
        });

        prevButton.addActionListener(e -> {
            if (isRsvpActive) toggleRsvp(); // RSVP aktifse normale dön ve durdur
            if (currentBook != null && currentBook.previousPage()) {
                updateUI(); // Normal UI'ı güncelle
            }
        });

        rsvpButton.addActionListener(e -> toggleRsvp());
    }

    private void toggleRsvp() {
        if (currentBook == null && !isRsvpActive) { // Sadece RSVP başlatılacaksa kitap kontrolü
            JOptionPane.showMessageDialog(this, "Please open a book first.", "RSVP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isRsvpActive = !isRsvpActive;

        if (isRsvpActive) {
            startRsvp();
            cardLayout.show(centerPanel, "RSVP_VIEW");
            rsvpButton.setText("Stop RSVP");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            openMenuItem.setEnabled(false);
        } else {
            stopRsvp();
            cardLayout.show(centerPanel, "NORMAL_VIEW");
            rsvpButton.setText("Start RSVP");
            updatePageNavigationButtons();
            openMenuItem.setEnabled(true);
        }
    }


    private void startRsvp() {
        if (currentBook == null || currentBook.getCurrentPageContent() == null) {
            isRsvpActive = false; // Hata durumunda RSVP'yi kapat
            toggleRsvp();
            return;
        }

        String currentPageText = currentBook.getCurrentPageContent();
        if (currentPageText.trim().isEmpty() || currentPageText.equals("No content available or end of book.")) {
            rsvpLabel.setText("No content on this page.");
            isRsvpActive = false; // Hata durumunda RSVP'yi kapat
            toggleRsvp(); // UI'ı normale çevir
            return;
        }

        currentWords = new ArrayList<>(Arrays.asList(currentPageText.trim().split("\\s+")));
        currentWordIndex = 0;

        if (currentWords.isEmpty() || currentWords.get(0).trim().isEmpty()) {
            rsvpLabel.setText("End of Page (or empty)");
            stopRsvp();
            isRsvpActive = false; // Otomatik durdur
            toggleRsvp(); // UI'ı normale çevir
            return;
        }
        rsvpTimer.start();
        showNextRsvpWord();
    }

    private void stopRsvp() {
        if (rsvpTimer != null) {
            rsvpTimer.stop();
        }
        // rsvpLabel.setText(""); // İsteğe bağlı
    }

    private void showNextRsvpWord() {
        if (currentWords == null || currentWordIndex >= currentWords.size()) {
            rsvpLabel.setText("End of Page");
            stopRsvp();
            // İsteğe bağlı: Otomatik olarak bir sonraki sayfaya geçebilir veya RSVP modunu kapatabilir
            // isRsvpActive = false; // RSVP'yi kapat
            // toggleRsvp(); // UI'ı normale çevir
            return;
        }

        String word = currentWords.get(currentWordIndex);
        // Boş stringleri atla (split'ten gelebilir)
        while (word.trim().isEmpty() && currentWordIndex < currentWords.size() - 1) {
            currentWordIndex++;
            word = currentWords.get(currentWordIndex);
        }

        if(word.trim().isEmpty() && currentWordIndex >= currentWords.size() -1){ // Son kelime de boşsa
            rsvpLabel.setText("End of Page");
            stopRsvp();
            return;
        }

        rsvpLabel.setText(word);
        currentWordIndex++;
    }


    private void openFile() {
        if (isRsvpActive) { // Eğer RSVP aktifse, önce durdur ve normale dön
            toggleRsvp();
        }

        JFileChooser fileChooser = new JFileChooser();
        // ... (fileChooser ayarları aynı)
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(pdfFilter);

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
                if (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf")) {
                    JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Bu durum genellikle JFileChooser filtresi nedeniyle oluşmaz
                    // ama yine de bir yedek mesaj.
                    JOptionPane.showMessageDialog(this, "Please select a valid .txt or .pdf file.", "File Type Error", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }

            if (currentBook != null) {
                String windowTitle = currentBook.getTitle();
                if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                    windowTitle += " by " + currentBook.getAuthor();
                }
                setTitle(windowTitle + " - Book Reader");
                updateUI();
            } else {
                if (fileName.endsWith(".txt") || fileName.endsWith(".pdf")) {
                    JOptionPane.showMessageDialog(this, "Could not load book from " + selectedFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updateUI() {
        if (isRsvpActive) { // Eğer UI güncellenirken RSVP aktifse, onu kapat
            toggleRsvp();
        } else { // Sadece normal moddaysa CardLayout'u normale çevir
            cardLayout.show(centerPanel, "NORMAL_VIEW");
        }

        if (currentBook != null) {
            bookTextArea.setText(currentBook.getCurrentPageContent());
            bookTextArea.setCaretPosition(0);
            String pageInfo = "Page: " + (currentBook.getCurrentPageIndex() + 1) + "/" + currentBook.getTotalPages();
            if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                pageInfo += "  |  Author: " + currentBook.getAuthor();
            }
            pageInfoLabel.setText(pageInfo);
            rsvpButton.setEnabled(true);
        } else {
            bookTextArea.setText("Open a book using File > Open Book...");
            pageInfoLabel.setText("Page: -/-");
            rsvpButton.setEnabled(false);
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