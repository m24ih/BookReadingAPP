// src/main/java/melih/bookreader/ui/ReadingFrame.java
package melih.bookreader.ui;

import melih.bookreader.model.Book;
import melih.bookreader.utils.TextFileLoader;
import melih.bookreader.utils.PdfFileLoader;
import melih.bookreader.core.RsvpController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;


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
    private RsvpController rsvpController;

    private int rsvpInitialDelayMs = 500;
    private JSlider speedSlider;
    private JLabel speedLabel;

    private JPanel centerPanel;
    private CardLayout cardLayout;

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        initRsvpController();
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
                speedSlider.getValue(),
                word -> rsvpLabel.setText(word),
                () -> currentBook != null ? currentBook.getCurrentPageContent() : "",
                this::handleRsvpNextPage,
                this::handleRsvpEndOfBook,
                this::handleRsvpPageEndedOrFailed
        );
        updateRsvpSpeedLabel();
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
                rsvpController.resetWordIndexForNewPage();
            }
        });

        prevButton.addActionListener(e -> {
            if (rsvpController.isActive()) toggleRsvpUiState();
            if (currentBook != null && currentBook.previousPage()) {
                bookTextArea.setText(currentBook.getCurrentPageContent());
                bookTextArea.setCaretPosition(0);
                updatePageInfo();
                updatePageNavigationButtons();
                rsvpController.resetWordIndexForNewPage();
            }
        });

        rsvpButton.addActionListener(e -> toggleRsvpUiState());

        speedSlider.addChangeListener(e -> {
            rsvpController.setDelay(speedSlider.getValue());
            updateRsvpSpeedLabel();
        });
    }

    private void handleRsvpNextPage() {
        if (currentBook != null && currentBook.nextPage()) {
            updatePageInfo();
            rsvpController.resetWordIndexForNewPage();
            rsvpController.start();
        } else {
            handleRsvpEndOfBook();
        }
    }

    private void handleRsvpEndOfBook() {
        rsvpLabel.setText("End of Book");
        if (rsvpController.isActive()) {
            toggleRsvpUiState();
        }
    }

    private void handleRsvpPageEndedOrFailed() {
        if (rsvpController.isActive()) {
            cardLayout.show(centerPanel, "NORMAL_VIEW");
            rsvpButton.setText("Start RSVP");
            updatePageNavigationButtons();
            openMenuItem.setEnabled(true);
            speedSlider.setEnabled(true);
        }
    }


    private void toggleRsvpUiState() {
        if (currentBook == null && !rsvpController.isActive()) {
            JOptionPane.showMessageDialog(this, "Please open a book first.", "RSVP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!rsvpController.isActive()) {
            rsvpController.start();
            if (!rsvpController.isActive()){
                return;
            }
            cardLayout.show(centerPanel, "RSVP_VIEW");
            rsvpButton.setText("Stop RSVP");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            openMenuItem.setEnabled(false);
            speedSlider.setEnabled(false);
        } else {
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
                if (!fileName.endsWith(".txt") && !fileName.endsWith(".pdf")){
                    JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName + "\nPlease select a .txt or .pdf file.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
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
                rsvpController.resetWordIndexForNewPage();
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
            toggleRsvpUiState();
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