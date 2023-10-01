package org.example.textEditor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void newFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
            }

            try {
                if (selectedFile.createNewFile()) {
                    openFile(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(this, "Файл уже существует.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ошибка при создании файла", "Ошибка", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Файл уже открыт.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String fileContent = readFileContent(selectedFile);

            JTextArea textArea = createTextArea(selectedFile, fileContent);
            JScrollPane scrollPane = new JScrollPane(textArea);

            JPanel tabPanel = createTabPanel(selectedFile, textArea);

            tabbedPane.addTab(null, scrollPane);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);

            tabInfoMap.put(selectedFile.getAbsolutePath(), selectedFile);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при открытии файла", "Ошибка", JOptionPane.ERROR_MESSAGE);
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

    private JTextArea createTextArea(File file, String content) {
        JTextArea textArea = new JTextArea();
        textArea.setText(content);
        textArea.setName(file.getAbsolutePath());
        return textArea;
    }

    private JPanel createTabPanel(File file, JTextArea textArea) {
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

    private void closeTab(File file, JTextArea textArea, JPanel tabPanel) {
        if (file != null && textArea != null) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());

                String textInFile = new String(bytes);
                String textInTextArea = textArea.getText();

                if (!textInFile.equals(textInTextArea)) {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "Сохранить изменения в файле?",
                            "Сохранение",
                            JOptionPane.YES_NO_CANCEL_OPTION
                    );

                    if (result == JOptionPane.YES_OPTION) {
                        saveFile(file, textInTextArea);
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ошибка при чтении файла", "Ошибка", JOptionPane.ERROR_MESSAGE);
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
            JTextArea textArea = findTextAreaInComponent(selectedComponent);
            File file = tabInfoMap.get(textArea.getName());

            if (file != null) {
                saveFile(file, textArea.getText());
            }
        }
    }

    private void saveAs() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            JTextArea textArea = findTextAreaInComponent(selectedComponent);

            if (textArea != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
                    }

                    tabInfoMap.remove(textArea.getName());
                    tabbedPane.remove(selectedIndex);

                    saveFile(selectedFile, textArea.getText());
                    openFile(selectedFile);
                }
            }
        }
    }

    private JTextArea findTextAreaInComponent(Component component) {
        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            JViewport viewport = scrollPane.getViewport();
            if (viewport.getView() instanceof JTextArea) {
                return (JTextArea) viewport.getView();
            }
        }
        return null;
    }

    private void saveFile(File file, String text) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new TextEditor();
    }
}
