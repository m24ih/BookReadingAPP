// src/main/java/melih/bookreader/ui/ReadingFrame.java
package melih.bookreader.ui;

import melih.bookreader.model.Book;
import melih.bookreader.utils.TextFileLoader;
import melih.bookreader.utils.PdfFileLoader;
import melih.bookreader.core.RsvpController; // YENİ

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
// Artık Arrays, List, ArrayList'e doğrudan RSVP için ihtiyacımız olmayabilir
// RsvpController bu detayları saklayacak.

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
    private RsvpController rsvpController; // YENİ

    // private Timer rsvpTimer; // RsvpController'a taşındı
    // private List<String> currentWords; // RsvpController'a taşındı
    // private int currentWordIndex; // RsvpController'a taşındı
    // private boolean isRsvpActive = false; // rsvpController.isActive() ile kontrol edilecek
    private int rsvpInitialDelayMs = 500; // Sadece başlangıç için
    private JSlider speedSlider;
    private JLabel speedLabel;

    private JPanel centerPanel;
    private CardLayout cardLayout;

    // private int lastRsvpWordIndexForCurrentPage = 0; // RsvpController içinde yönetilecek

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        initRsvpController(); // RsvpController'ı başlat
        addListeners();
        updateUI();
    }

    private void initComponents() {
        // ... (Değişiklik yok)
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

        speedSlider = new JSlider(JSlider.HORIZONTAL, 50, 1000, rsvpInitialDelayMs);
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
        // ... (Değişiklik yok)
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

    private void initRsvpController() {
        rsvpController = new RsvpController(
                speedSlider.getValue(), // Başlangıç gecikmesi slider'dan alınsın
                word -> rsvpLabel.setText(word),         // Kelimeyi gösterme callback'i
                () -> currentBook != null ? currentBook.getCurrentPageContent() : "", // Sayfa içeriği sağlayıcı
                this::handleRsvpNextPage,                // Sonraki sayfaya geçme eylemi
                this::handleRsvpEndOfBook,               // Kitap sonu eylemi
                this::handleRsvpPageEndedOrFailed        // Sayfa bittiğinde veya RSVP başlatılamadığında
        );
        updateRsvpSpeedLabel(); // Başlangıç etiketini ayarla
    }

    private void updateRsvpSpeedLabel() {
        int delay = speedSlider.getValue();
        double wpm = (60.0 * 1000.0) / delay;
        speedLabel.setText(String.format("%.0f wpm (%dms)", wpm, delay));
    }


    private void addListeners() {
        openMenuItem.addActionListener(e -> openFile());

        nextButton.addActionListener(e -> {
            if (rsvpController.isActive()) toggleRsvpUiState();
            if (currentBook != null && currentBook.nextPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
                rsvpController.resetWordIndexForNewPage(); // Yeni sayfa, RSVP indeksini sıfırla
            }
        });

        prevButton.addActionListener(e -> {
            if (rsvpController.isActive()) toggleRsvpUiState();
            if (currentBook != null && currentBook.previousPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
                rsvpController.resetWordIndexForNewPage(); // Yeni sayfa, RSVP indeksini sıfırla
            }
        });

        rsvpButton.addActionListener(e -> toggleRsvpUiState());

        speedSlider.addChangeListener(e -> {
            rsvpController.setDelay(speedSlider.getValue());
            updateRsvpSpeedLabel();
        });
    }

    // RSVP Controller için callback metodları
    private void handleRsvpNextPage() {
        if (currentBook != null && currentBook.nextPage()) {
            updatePageInfo();
            rsvpController.resetWordIndexForNewPage();
            rsvpController.start(); // Yeni sayfa için RSVP'yi yeniden başlat
        } else {
            handleRsvpEndOfBook(); // Sonraki sayfa yoksa kitap sonu
        }
    }

    private void handleRsvpEndOfBook() {
        rsvpLabel.setText("End of Book");
        if (rsvpController.isActive()) { // Sadece UI'ı güncelle, stop() zaten çağrılmış olabilir
            toggleRsvpUiState();
        }
    }

    private void handleRsvpPageEndedOrFailed() {
        // Bu metod, RSVP'nin bir sayfada başlayamaması (örn: sayfa boş)
        // veya normal bir şekilde sayfa sonuna gelinmesi durumunda çağrılabilir.
        // Eğer RSVP aktifse ve bu metod çağrılıyorsa, UI'ı normale döndürmek isteyebiliriz.
        if (rsvpController.isActive()) {
            // toggleRsvpUiState(); // Bu, sonsuz döngüye sokabilir eğer start hemen pageEnded çağırırsa.
            // Daha kontrollü bir şekilde UI'ı normale döndür.
            cardLayout.show(centerPanel, "NORMAL_VIEW");
            rsvpButton.setText("Start RSVP");
            updatePageNavigationButtons();
            openMenuItem.setEnabled(true);
            speedSlider.setEnabled(true);
            // rsvpController.stop(); // Zaten stop çağrılmış olabilir veya isActive false olmalı
        }
        // Eğer sayfa boş olduğu için başlatılamadıysa, rsvpLabel'da zaten mesaj yazar.
    }


    private void toggleRsvpUiState() {
        if (currentBook == null && !rsvpController.isActive()) {
            JOptionPane.showMessageDialog(this, "Please open a book first.", "RSVP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!rsvpController.isActive()) { // RSVP'yi başlat
            rsvpController.start();
            if (!rsvpController.isActive()){ // Eğer startRsvp başlatamazsa (örn: sayfa boş)
                // Hata mesajı rsvpLabel'da veya dialog ile gösterilmiş olmalı.
                // RSVP moduna geçme.
                return;
            }
            cardLayout.show(centerPanel, "RSVP_VIEW");
            rsvpButton.setText("Stop RSVP");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            openMenuItem.setEnabled(false);
            speedSlider.setEnabled(false);
        } else { // RSVP'yi durdur
            rsvpController.stop();
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

    private void openFile() {
        if (rsvpController.isActive()) {
            toggleRsvpUiState();
        }
        // ... (openFile metodunun geri kalanı büyük ölçüde aynı)
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
                if (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf")){ // "Tüm Dosyalar" seçili olabilir
                    JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                } else { // Bu durum olmamalı ama bir yedek.
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
                rsvpController.resetWordIndexForNewPage(); // Yeni kitap, RSVP indeksini sıfırla
                updateUI();
            } else {
                if (fileName.endsWith(".txt") || fileName.endsWith(".pdf")) {
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
        if (rsvpController != null && rsvpController.isActive()) {
            toggleRsvpUiState(); // UI güncellenmeden önce RSVP'yi durdur ve normale dön
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
        boolean enableNav = currentBook != null && (rsvpController == null || !rsvpController.isActive());
        prevButton.setEnabled(enableNav && currentBook.getCurrentPageIndex() > 0);
        nextButton.setEnabled(enableNav && currentBook.getCurrentPageIndex() < currentBook.getTotalPages() - 1);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReadingFrame().setVisible(true));
    }
}