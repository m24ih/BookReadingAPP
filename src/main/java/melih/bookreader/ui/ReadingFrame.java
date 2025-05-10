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

public class ReadingFrame extends JFrame {
    private JTextArea bookTextArea;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageInfoLabel;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem openMenuItem;

    private Book currentBook;

    public ReadingFrame() {
        setTitle("Simple Book Reader");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        addListeners();
        updateUI();
    }

    private void initComponents() {
        bookTextArea = new JTextArea();
        bookTextArea.setEditable(false);
        bookTextArea.setLineWrap(true);
        bookTextArea.setWrapStyleWord(true);
        bookTextArea.setFont(new Font("Serif", Font.PLAIN, 16));

        prevButton = new JButton("Previous");
        nextButton = new JButton("Next");
        pageInfoLabel = new JLabel("Page: -/-");

        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        openMenuItem = new JMenuItem("Open Book..."); // Genel bir isim
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(bookTextArea);
        add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(prevButton);
        bottomPanel.add(pageInfoLabel);
        bottomPanel.add(nextButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentBook != null && currentBook.nextPage()) {
                    updateUI();
                }
            }
        });

        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentBook != null && currentBook.previousPage()) {
                    updateUI();
                }
            }
        });
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Book File");

        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.addChoosableFileFilter(pdfFilter);

        fileChooser.setFileFilter(pdfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();
            currentBook = null; // Önceki kitabı temizle

            if (fileName.endsWith(".txt")) {
                currentBook = TextFileLoader.loadBook(selectedFile);
            } else if (fileName.endsWith(".pdf")) {
                currentBook = PdfFileLoader.loadBook(selectedFile);
            } else {
                JOptionPane.showMessageDialog(this, "Unsupported file type: " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (currentBook != null) {
                // Kitap başlığı ve yazarını pencere başlığına ekleyelim (eğer yazar varsa)
                String windowTitle = currentBook.getTitle();
                if (currentBook.getAuthor() != null && !currentBook.getAuthor().isEmpty() && !currentBook.getAuthor().trim().equals("Unknown Author")) {
                    windowTitle += " by " + currentBook.getAuthor();
                }
                setTitle(windowTitle + " - Book Reader");
                updateUI();
            } else {
                JOptionPane.showMessageDialog(this, "Could not load book from " + selectedFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateUI() {
        if (currentBook != null) {
            bookTextArea.setText(currentBook.getCurrentPageContent());
            bookTextArea.setCaretPosition(0);
            String pageInfo = "Page: " + (currentBook.getCurrentPageIndex() + 1) + "/" + currentBook.getTotalPages();
            pageInfoLabel.setText(pageInfo);
            prevButton.setEnabled(currentBook.getCurrentPageIndex() > 0);
            nextButton.setEnabled(currentBook.getCurrentPageIndex() < currentBook.getTotalPages() - 1);
        } else {
            bookTextArea.setText("Open a book using File > Open Book...");
            pageInfoLabel.setText("Page: -/-");
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
