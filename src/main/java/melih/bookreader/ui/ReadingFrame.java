// src/main/java/melih/bookreader/ui/ReadingFrame.java
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
    private int rsvpDelayMilliseconds = 500; // Varsayılan 0.5 saniye
    private JSlider speedSlider;
    private JLabel speedLabel;

    private JPanel centerPanel;
    private CardLayout cardLayout;

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 650); // TopPanel için biraz yükseklik
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        initRsvpTimer(); // Timer'ı oluştur
        addListeners();  // Listener'ları timer oluşturulduktan sonra ekle

        updateUI(); // Başlangıç UI durumu
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

        speedLabel = new JLabel(); // updateRsvpSpeed ile doldurulacak

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
        updateRsvpSpeed(); // Slider'ın başlangıç değerine göre hızı ve etiketi ayarla

        rsvpTimer = new Timer(rsvpDelayMilliseconds, new ActionListener() {
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
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
            }
        });

        prevButton.addActionListener(e -> {
            if (isRsvpActive) toggleRsvp();
            if (currentBook != null && currentBook.previousPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
            }
        });

        rsvpButton.addActionListener(e -> toggleRsvp());

        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateRsvpSpeed();
            }
        });
    }

    private void toggleRsvp() {
        if (currentBook == null && !isRsvpActive) {
            JOptionPane.showMessageDialog(this, "Please open a book first.", "RSVP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isRsvpActive = !isRsvpActive;

        if (isRsvpActive) {
            startRsvp(); // Kelimeleri yükle
            if (currentWords == null || currentWords.isEmpty()) { // startRsvp kelime yükleyemezse (örn: sayfa boş)
                isRsvpActive = false; // RSVP'yi başlatma
                // startRsvp içinde zaten mesaj gösterilmiş olabilir veya burada gösterilebilir.
                // JOptionPane.showMessageDialog(this, "Cannot start RSVP on an empty page.", "RSVP Info", JOptionPane.INFORMATION_MESSAGE);
                return; // RSVP moduna geçme
            }
            cardLayout.show(centerPanel, "RSVP_VIEW");
            rsvpButton.setText("Stop RSVP");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            openMenuItem.setEnabled(false);
            speedSlider.setEnabled(false);
        } else {
            stopRsvp();
            cardLayout.show(centerPanel, "NORMAL_VIEW");
            rsvpButton.setText("Start RSVP");
            updatePageNavigationButtons();
            openMenuItem.setEnabled(true);
            speedSlider.setEnabled(true);
            // Normal görünüme döndüğümüzde metin alanını güncelle
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
            rsvpLabel.setText("Page is empty or unavailable."); // RSVP label'ında göster
            currentWords = null; // Kelime listesini null yap
            // Otomatik sonraki sayfaya geçmeyi burada deneyebiliriz, ancak toggleRsvp'den çağrıldığı için karmaşıklaşabilir.
            // Şimdilik kullanıcıya bırakalım.
            return;
        }

        // Kelimeleri boşluklara göre böl ve boş stringleri temizle
        currentWords = new ArrayList<>(Arrays.asList(currentPageText.trim().split("\\s+")));
        currentWords.removeIf(String::isEmpty); // lambda ile boş stringleri kaldır

        currentWordIndex = 0;

        if (currentWords.isEmpty()) {
            rsvpLabel.setText("Page is effectively empty."); // RSVP label'ında göster
            // stopRsvp(); // Timer zaten çalışmıyor olabilir
            return;
        }

        rsvpTimer.setDelay(rsvpDelayMilliseconds); // Mevcut hız ayarını kullan
        rsvpTimer.start();
        showNextRsvpWord(); // İlk kelimeyi hemen göster
    }

    private void stopRsvp() {
        if (rsvpTimer != null) {
            rsvpTimer.stop();
        }
    }

    private void showNextRsvpWord() {
        if (currentWords == null || currentWordIndex >= currentWords.size()) {
            // Sayfanın sonuna gelindi
            if (currentBook != null && currentBook.nextPage()) {
                // Bir sonraki sayfaya başarıyla geçildi
                updatePageInfo();
                startRsvp(); // Yeni sayfa için RSVP'yi yeniden başlat
                // startRsvp zaten ilk kelimeyi gösterecek showNextRsvpWord'ü çağıracak.
            } else {
                // Kitabın sonu veya sonraki sayfaya geçilemedi
                rsvpLabel.setText("End of Book");
                stopRsvp();
                if (isRsvpActive) { // Eğer hala RSVP modundaysak normale dön
                    toggleRsvp();
                }
            }
            return;
        }

        String word = currentWords.get(currentWordIndex);
        // Boş stringleri atlama mantığına artık gerek yok çünkü startRsvp'de temizliyoruz.
        // Yine de bir güvenlik önlemi olarak kalabilir.
        // while (word.trim().isEmpty() && currentWordIndex < currentWords.size() - 1) {
        //    currentWordIndex++;
        //    word = currentWords.get(currentWordIndex);
        // }
        // if(word.trim().isEmpty() && currentWordIndex >= currentWords.size() -1){
        //     // Bu durum yukarıdaki ilk if bloğu tarafından yakalanmalı
        //     showNextRsvpWord(); // Rekürsif çağrı ile bir sonraki kelimeye (veya sayfa sonuna) git
        //     return;
        // }

        rsvpLabel.setText(word);
        currentWordIndex++;
    }


    private void openFile() {
        if (isRsvpActive) {
            toggleRsvp(); // Yeni dosya açmadan önce RSVP'yi durdur ve normale dön
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Book File");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(pdfFilter); // Varsayılan olarak PDF filtresi
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
                // Eğer dosya uzantısı bilinenlerden biri değilse ve "Tüm Dosyalar" seçiliyse
                if (fileChooser.getFileFilter() instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                    boolean knownType = false;
                    for (String ext : selectedFilter.getExtensions()) {
                        if (fileName.endsWith("." + ext)) {
                            knownType = true;
                            break;
                        }
                    }
                    if (!knownType) { // Eğer seçilen filtreye uymayan bir dosya ise
                        JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else { // "Tüm Dosyalar" seçili ve dosya .txt veya .pdf değilse
                    JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Bu noktaya gelinirse, dosya uzantısı desteklenmiyor demektir.
                // Yukarıdaki kontrollerden biri zaten yakalamış olmalı.
            }

            if (currentBook != null) {
                String windowTitle = currentBook.getTitle();
                if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                    windowTitle += " by " + currentBook.getAuthor();
                }
                setTitle(windowTitle + " - Book Reader");
                updateUI(); // Bu, RSVP durumunu da sıfırlar (normal görünüme döner)
            } else {
                // Sadece desteklenen dosya tipleri için yükleme başarısız olduğunda gösterilmeli.
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
        if (isRsvpActive) {
            toggleRsvp(); // UI güncellenmeden önce RSVP'yi durdur ve normale dön
        } else {
            // Zaten normal moddaysak, CardLayout'un doğru görünümde olduğundan emin ol
            if (cardLayout != null && centerPanel != null) { // Bileşenler oluşturulduysa
                cardLayout.show(centerPanel, "NORMAL_VIEW");
            }
        }

        if (currentBook != null) {
            bookTextArea.setText(currentBook.getCurrentPageContent());
            bookTextArea.setCaretPosition(0);
            updatePageInfo();
            rsvpButton.setEnabled(true);
            speedSlider.setEnabled(true); // Kitap varsa hız ayarı aktif olsun
        } else {
            bookTextArea.setText("Open a book using File > Open Book...");
            updatePageInfo();
            rsvpButton.setEnabled(false);
            speedSlider.setEnabled(false); // Kitap yoksa hız ayarı pasif
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
        SwingUtilities.invokeLater(() -> {
            new ReadingFrame().setVisible(true);
        });
    }
}