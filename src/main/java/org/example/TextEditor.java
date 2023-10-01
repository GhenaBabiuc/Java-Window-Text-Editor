package org.example;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TextEditor extends JFrame {
    private JTabbedPane tabbedPane;
    private Map<String, File> tabInfoMap;
    private JComboBox<String> fontComboBox;
    private JComboBox<Integer> fontSizeComboBox;
    private JButton applyFormattingButton;

    public TextEditor() {
        setTitle("Text Editor");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        tabInfoMap = new HashMap<>();

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem newMenuItem = new JMenuItem("New");
        newMenuItem.addActionListener(e -> newFile());
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        fileMenu.add(newMenuItem);

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(e -> openFile());
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileMenu.add(openMenuItem);

        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(e -> save());
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        fileMenu.add(saveMenuItem);

        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        saveAsMenuItem.addActionListener(e -> saveAs());
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        fileMenu.add(saveAsMenuItem);

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
        fileMenu.add(exitMenuItem);

        initFormattingPanel();

        setLocationRelativeTo(null);
        setVisible(true);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initFormattingPanel() {
        JPanel formattingPanel = new JPanel();
        fontComboBox = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontSizeComboBox = new JComboBox<>(new Integer[]{8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72});
        applyFormattingButton = new JButton("Apply");

        formattingPanel.add(new JLabel("Font: "));
        formattingPanel.add(fontComboBox);
        formattingPanel.add(new JLabel("Size: "));
        formattingPanel.add(fontSizeComboBox);
        formattingPanel.add(applyFormattingButton);

        applyFormattingButton.addActionListener(e -> applyFormatting());

        getContentPane().add(formattingPanel, BorderLayout.NORTH);
    }

    private void applyFormatting() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                int start = textArea.getSelectionStart();
                int end = textArea.getSelectionEnd();

                if (start != end) {
                    String selectedFont = (String) fontComboBox.getSelectedItem();
                    int selectedFontSize = (Integer) fontSizeComboBox.getSelectedItem();

                    Font font = new Font(selectedFont, Font.PLAIN, selectedFontSize);
                    SimpleAttributeSet attributes = new SimpleAttributeSet();
                    StyleConstants.setFontFamily(attributes, font.getFamily());
                    StyleConstants.setFontSize(attributes, font.getSize());
                    StyledDocument doc = textArea.getStyledDocument();
                    doc.setCharacterAttributes(start, end - start, attributes, false);
                }
            }
        }
    }

    private void newFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("RTF Files (*.rtf)", ".rtf"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".txt") && !selectedFile.getName().toLowerCase().endsWith(".rtf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + (fileChooser.getFileFilter().getDescription().contentEquals(".txt") ? ".txt" : ".rtf"));
            }

            try {
                if (selectedFile.createNewFile()) {
                    openFile(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(this, "The file already exists.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error when creating a file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            openFile(selectedFile);
        }
    }

    private void openFile(File selectedFile) {
        if (tabInfoMap.containsValue(selectedFile)) {
            JOptionPane.showMessageDialog(this, "The file has already been opened.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String fileContent;
            JTextPane textArea;

            if (selectedFile.getName().endsWith(".rtf")) {
                RTFEditorKit rtfKit = new RTFEditorKit();
                FileInputStream inputStream = new FileInputStream(selectedFile);
                textArea = new JTextPane();
                rtfKit.read(inputStream, textArea.getDocument(), 0);
                inputStream.close();
            } else {
                fileContent = readFileContent(selectedFile);
                textArea = createTextArea(selectedFile, fileContent);
            }

            JScrollPane scrollPane = new JScrollPane(textArea);

            JPanel tabPanel = createTabPanel(selectedFile, textArea);

            tabbedPane.addTab(null, scrollPane);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);

            tabInfoMap.put(selectedFile.getAbsolutePath(), selectedFile);
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error when opening a file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        }
        return fileContent.toString();
    }

    private JTextPane createTextArea(File file, String content) {
        JTextPane textArea = new JTextPane();
        textArea.setText(content);
        textArea.setName(file.getAbsolutePath());
        return textArea;
    }

    private JPanel createTabPanel(File file, JTextPane textArea) {
        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.setOpaque(false);

        JButton closeButton = new JButton("x");
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.addActionListener(e -> closeTab(file, textArea, tabPanel));

        tabPanel.add(new JLabel(file.getName()), BorderLayout.CENTER);
        tabPanel.setToolTipText(file.getAbsolutePath());
        tabPanel.add(closeButton, BorderLayout.EAST);

        return tabPanel;
    }

    private void closeTab(File file, JTextPane textArea, JPanel tabPanel) {
        if (file != null && textArea != null) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());

                String textInFile = new String(bytes);
                String textInTextArea = textArea.getText();

                if (!textInFile.equals(textInTextArea)) {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "Save changes to the file?",
                            "Saving",
                            JOptionPane.YES_NO_CANCEL_OPTION
                    );

                    if (result == JOptionPane.YES_OPTION) {
                        saveFile(file, textArea);
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error when reading a file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        int tabIndex = tabbedPane.indexOfTabComponent(tabPanel);
        if (tabIndex != -1) {
            tabbedPane.remove(tabIndex);
            tabInfoMap.remove(file.getAbsolutePath());
        }
    }

    private void save() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);
            File file = tabInfoMap.get(textArea.getName());

            if (file != null) {
                saveFile(file, textArea);
            }
        }
    }

    private void saveAs() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", ".txt"));
                fileChooser.setFileFilter(new FileNameExtensionFilter("RTF Files (*.rtf)", ".rtf"));

                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().toLowerCase().endsWith(".txt") && !selectedFile.getName().toLowerCase().endsWith(".rtf")) {
                        selectedFile = new File(selectedFile.getAbsolutePath() + (fileChooser.getFileFilter().getDescription().contentEquals(".txt") ? ".txt" : ".rtf"));
                    }

                    tabInfoMap.remove(textArea.getName());
                    tabbedPane.remove(selectedIndex);

                    saveFile(selectedFile, textArea);
                    openFile(selectedFile);
                }
            }
        }
    }

    private JTextPane findTextAreaInComponent(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            JViewport viewport = scrollPane.getViewport();
            if (viewport.getView() instanceof JTextPane) {
                return (JTextPane) viewport.getView();
            }
        }
        return null;
    }

    private void saveFile(File file, JTextPane textPane) {
        try {
            String fileName = file.getName();
            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

            RTFEditorKit rtfKit = new RTFEditorKit();

            if ("rtf".equals(fileExtension)) {
                StyledDocument doc = textPane.getStyledDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                rtfKit.write(out, doc, 0, doc.getLength());
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                out.writeTo(fileOutputStream);
                fileOutputStream.close();
            } else if ("txt".equals(fileExtension)) {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(textPane.getText());
                fileWriter.close();
            } else {
                JOptionPane.showMessageDialog(this, "Unsupported file extension", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new TextEditor();
    }
}
