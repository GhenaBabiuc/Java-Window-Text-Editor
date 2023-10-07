package org.example;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class TextEditor extends JFrame {
    private JTabbedPane tabbedPane;
    private Map<String, File> tabInfoMap;
    private JComboBox<String> fontComboBox;
    private JComboBox<Integer> fontSizeComboBox;
    private JButton applyFormattingButton;
    private JComboBox<String> colorComboBox;
    private JComboBox<String> backgroundColorComboBox;
    private JComboBox<String> fontStyleComboBox;
    private JTextField searchField;
    private JButton findButton;
    private JButton prevResultButton;
    private JButton nextResultButton;
    private List<Integer> searchResults;
    private int currentResultIndex;
    private JLabel searchResultCountLabel;
    private JLabel currentPositionLabel;
    private JRadioButton searchDownRadioButton;
    private JRadioButton searchUpRadioButton;
    private JRadioButton searchAllRadioButton;
    private ButtonGroup searchDirectionGroup;
    private JTextField replaceField;
    private JButton replaceButton;
    private JButton replaceAllButton;


    public TextEditor() {
        setTitle("Text Editor");
        setSize(950, 600);
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
        initSearchPanel();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        findButton = new JButton("Find");
        prevResultButton = new JButton("Previous Result");
        nextResultButton = new JButton("Next Result");
        searchResults = new ArrayList<>();
        currentResultIndex = -1;

        findButton.addActionListener(e -> findText());
        prevResultButton.addActionListener(e -> showPreviousResult());
        nextResultButton.addActionListener(e -> showNextResult());

        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);

        searchDownRadioButton = new JRadioButton("Down");
        searchUpRadioButton = new JRadioButton("Up");
        searchAllRadioButton = new JRadioButton("All");
        searchDirectionGroup = new ButtonGroup();

        searchDirectionGroup.add(searchDownRadioButton);
        searchDirectionGroup.add(searchUpRadioButton);
        searchDirectionGroup.add(searchAllRadioButton);

        searchDownRadioButton.setSelected(true);

        searchPanel.add(searchDownRadioButton);
        searchPanel.add(searchUpRadioButton);
        searchPanel.add(searchAllRadioButton);

        searchPanel.add(findButton);
        searchPanel.add(prevResultButton);
        searchPanel.add(nextResultButton);

        searchResultCountLabel = new JLabel("Results: 0");
        currentPositionLabel = new JLabel("Position: -");

        searchPanel.add(searchResultCountLabel);
        searchPanel.add(currentPositionLabel);

        JPanel replacePanel = new JPanel();
        replaceField = new JTextField(20);
        replaceButton = new JButton("Replace");
        replaceAllButton = new JButton("Replace All");

        replaceButton.addActionListener(e -> replaceText());
        replaceAllButton.addActionListener(e -> replaceAllText());

        replacePanel.add(new JLabel("Replace with: "));
        replacePanel.add(replaceField);
        replacePanel.add(replaceButton);
        replacePanel.add(replaceAllButton);

        JPanel dockedPanel = new JPanel(new GridLayout(2, 1));
        dockedPanel.add(searchPanel);
        dockedPanel.add(replacePanel);

        getContentPane().add(dockedPanel, BorderLayout.SOUTH);
    }

    private void replaceText() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                String searchText = searchField.getText();
                String replaceText = replaceField.getText();

                int start = textArea.getSelectionStart();
                int end = textArea.getSelectionEnd();
                String text = textArea.getText();
                String newText = text.substring(0, start) + text.substring(start, end).replaceFirst(searchText, replaceText) + text.substring(end);

                textArea.setText(newText);
            }
        }
    }

    private void replaceAllText() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                String searchText = searchField.getText();
                String replaceText = replaceField.getText();
                String text = textArea.getText();

                text = text.replaceAll(searchText, replaceText);

                textArea.setText(text);
            }
        }
    }

    private void findText() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                String searchText = searchField.getText();
                String text = textArea.getText();
                searchResults.clear();

                int startIndex = 0;
                int endIndex = text.length();

                if (searchDownRadioButton.isSelected()) {
                    startIndex = textArea.getCaretPosition();
                } else if (searchUpRadioButton.isSelected()) {
                    endIndex = textArea.getCaretPosition();
                }

                if (searchDownRadioButton.isSelected() || searchAllRadioButton.isSelected()) {
                    while (startIndex < endIndex) {
                        startIndex = text.indexOf(searchText, startIndex);
                        if (startIndex == -1) {
                            break;
                        }
                        searchResults.add(startIndex);
                        startIndex += searchText.length();
                    }
                } else if (searchUpRadioButton.isSelected()) {
                    while (endIndex > startIndex) {
                        endIndex = text.lastIndexOf(searchText, endIndex - 1);
                        if (endIndex == -1) {
                            break;
                        }
                        searchResults.add(endIndex);
                    }
                }

                if (searchResults.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No results found.", "Search", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    currentResultIndex = 0;
                    highlightSearchResult(textArea, searchResults.get(currentResultIndex), searchText.length());
                }
            }
        }
        if (!searchResults.isEmpty()) {
            searchResultCountLabel.setText("Results: " + searchResults.size());
            currentResultIndex = 0;
            showCurrentResult();
        }
    }

    private void showPreviousResult() {
        if (!searchResults.isEmpty()) {
            currentResultIndex--;
            if (currentResultIndex < 0) {
                currentResultIndex = searchResults.size() - 1;
            }
            showCurrentResult();
        }
    }

    private void showNextResult() {
        if (!searchResults.isEmpty()) {
            currentResultIndex++;
            if (currentResultIndex >= searchResults.size()) {
                currentResultIndex = 0;
            }
            showCurrentResult();
        }
    }

    private void showCurrentResult() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextPane textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                int resultIndex = searchResults.get(currentResultIndex);
                String searchText = searchField.getText();
                highlightSearchResult(textArea, resultIndex, searchText.length());
            }
        }
        currentPositionLabel.setText("Position: " + (searchResults.isEmpty() ? "-" : (currentResultIndex + 1)));
    }

    private void highlightSearchResult(JTextPane textArea, int startIndex, int length) {
        textArea.requestFocusInWindow();
        textArea.select(startIndex, startIndex + length);
    }

    private void initFormattingPanel() {
        JPanel formattingPanel = new JPanel();
        fontComboBox = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontSizeComboBox = new JComboBox<>(new Integer[]{8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72});
        colorComboBox = new JComboBox<>(new String[]{"Black", "Red", "Blue", "Green", "Orange"});
        backgroundColorComboBox = new JComboBox<>(new String[]{"None", "Yellow", "Gray", "Cyan"});
        fontStyleComboBox = new JComboBox<>(new String[]{"Plain", "Bold", "Italic", "Bold Italic"});

        applyFormattingButton = new JButton("Apply");

        formattingPanel.add(new JLabel("Font: "));
        formattingPanel.add(fontComboBox);
        formattingPanel.add(new JLabel("Size: "));
        fontSizeComboBox.setSelectedIndex(5);
        formattingPanel.add(fontSizeComboBox);
        formattingPanel.add(new JLabel("Color: "));
        formattingPanel.add(colorComboBox);
        formattingPanel.add(new JLabel("Background: "));
        formattingPanel.add(backgroundColorComboBox);
        formattingPanel.add(new JLabel("Style: "));
        formattingPanel.add(fontStyleComboBox);
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
                    Color selectedColor = getColorFromString((String) colorComboBox.getSelectedItem());
                    Color selectedBackgroundColor = getBackgroundColorFromString((String) backgroundColorComboBox.getSelectedItem());

                    Font font = new Font(selectedFont, Font.PLAIN, selectedFontSize);
                    SimpleAttributeSet attributes = new SimpleAttributeSet();
                    StyleConstants.setFontFamily(attributes, font.getFamily());
                    StyleConstants.setFontSize(attributes, font.getSize());
                    StyleConstants.setForeground(attributes, selectedColor);
                    StyleConstants.setBackground(attributes, selectedBackgroundColor);

                    Object o = Objects.requireNonNull(fontStyleComboBox.getSelectedItem());
                    if (o.equals("Bold")) {
                        StyleConstants.setBold(attributes, true);
                    } else if (o.equals("Italic")) {
                        StyleConstants.setItalic(attributes, true);
                    } else if (o.equals("Bold Italic")) {
                        StyleConstants.setBold(attributes, true);
                        StyleConstants.setItalic(attributes, true);
                    } else {
                        StyleConstants.setBold(attributes, false);
                        StyleConstants.setItalic(attributes, false);
                    }

                    StyledDocument doc = textArea.getStyledDocument();
                    doc.setCharacterAttributes(start, end - start, attributes, false);
                }
            }
        }
    }

    private Color getColorFromString(String colorString) {
        return switch (colorString) {
            case "Red" -> Color.RED;
            case "Blue" -> Color.BLUE;
            case "Green" -> Color.GREEN;
            case "Orange" -> Color.ORANGE;
            default -> Color.BLACK;
        };
    }

    private Color getBackgroundColorFromString(String backgroundColorString) {
        return switch (backgroundColorString) {
            case "Yellow" -> Color.YELLOW;
            case "Gray" -> Color.GRAY;
            case "Cyan" -> Color.CYAN;
            default -> Color.WHITE;
        };
    }

    private void newFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", ".txt"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("RTF Files (*.rtf)", ".rtf"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".txt") && !selectedFile.getName().toLowerCase().endsWith(".rtf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + (fileChooser.getFileFilter().getDescription().contains(".txt") ? ".txt" : ".rtf"));
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

            textArea.setName(selectedFile.getAbsolutePath());
            tabInfoMap.put(selectedFile.getAbsolutePath(), selectedFile);

            textArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char typedChar = e.getKeyChar();
                    if (isValidCharacter(typedChar)) {
                        applyStyleAndInsertCharacter(typedChar, textArea);
                    }
                }
            });
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error when opening a file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == ',' || c == '.' || c == ' ';
    }

    private void applyStyleAndInsertCharacter(char typedChar, JTextPane textArea) {
        String selectedFont = (String) fontComboBox.getSelectedItem();
        int selectedFontSize = (Integer) fontSizeComboBox.getSelectedItem();
        Color selectedColor = getColorFromString((String) colorComboBox.getSelectedItem());
        Color selectedBackgroundColor = getBackgroundColorFromString((String) backgroundColorComboBox.getSelectedItem());

        Font font = new Font(selectedFont, Font.PLAIN, selectedFontSize);
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, font.getFamily());
        StyleConstants.setFontSize(attributes, font.getSize());
        StyleConstants.setForeground(attributes, selectedColor);
        StyleConstants.setBackground(attributes, selectedBackgroundColor);

        Object o = Objects.requireNonNull(fontStyleComboBox.getSelectedItem());
        if (o.equals("Bold")) {
            StyleConstants.setBold(attributes, true);
        } else if (o.equals("Italic")) {
            StyleConstants.setItalic(attributes, true);
        } else if (o.equals("Bold Italic")) {
            StyleConstants.setBold(attributes, true);
            StyleConstants.setItalic(attributes, true);
        } else {
            StyleConstants.setBold(attributes, false);
            StyleConstants.setItalic(attributes, false);
        }

        Document doc = textArea.getDocument();
        int caretPosition = textArea.getCaretPosition();

        try {
            doc.insertString(caretPosition, String.valueOf(typedChar), attributes);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String readFileContent(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes);
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
                if (file.getName().endsWith(".txt")) {
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
                } else if (file.getName().endsWith(".rtf")) {
                    saveFile(file, textArea);
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
                        selectedFile = new File(selectedFile.getAbsolutePath() + (fileChooser.getFileFilter().getDescription().contains(".txt") ? ".txt" : ".rtf"));
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
