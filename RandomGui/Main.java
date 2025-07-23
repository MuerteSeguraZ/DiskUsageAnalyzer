package normal_stuff.java.RandomGui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Random;

@SuppressWarnings("unused")
public class Main extends JFrame {
    private JButton clickButton;
    private JButton clearButton;
    private JButton surpriseButton;
    private JButton countButton;
    private JButton wordCountButton;
    private JButton copyButton;
    private JButton clearLogButton;
    private JTextField inputField;
    private JLabel messageLabel;
    private JLabel charCountLabel;
    private JCheckBox checkBox;
    private JCheckBox reverseCheckBox;
    private JCheckBox colorCheckBox;
    private JComboBox<String> comboBox;
    private JTextArea textArea;

    public Main() {
        setTitle("Ultimate Java Swing GUI");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        clickButton = new JButton("Click Me");
        clearButton = new JButton("Clear Input");
        surpriseButton = new JButton("Surprise");
        countButton = new JButton("Count Characters");
        wordCountButton = new JButton("Count Words");
        copyButton = new JButton("Copy to Clipboard");
        clearLogButton = new JButton("Clear Log");

        inputField = new JTextField(20);
        messageLabel = new JLabel("Enter text and press the button");
        charCountLabel = new JLabel("Characters: 0");
        checkBox = new JCheckBox("Enable Uppercase");
        reverseCheckBox = new JCheckBox("Reverse Text");
        colorCheckBox = new JCheckBox("Random Text Color");
        comboBox = new JComboBox<>(new String[]{"Greeting", "Farewell", "Random"});
        textArea = new JTextArea(10, 50);
        textArea.setEditable(false);

        // Panel setup with BoxLayout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(inputField);
        inputPanel.add(charCountLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(clickButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(surpriseButton);
        buttonPanel.add(countButton);
        buttonPanel.add(wordCountButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(clearLogButton);

        JPanel optionPanel = new JPanel(new FlowLayout());
        optionPanel.add(checkBox);
        optionPanel.add(reverseCheckBox);
        optionPanel.add(colorCheckBox);
        optionPanel.add(comboBox);

        panel.add(inputPanel);
        panel.add(buttonPanel);
        panel.add(optionPanel);
        panel.add(messageLabel);

        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane);

        this.add(panel);

        // Listeners
        comboBox.addActionListener(e -> {
            String action = (String) comboBox.getSelectedItem();
            if (action != null) {
                switch (action) {
                    case "Greeting" -> clickButton.setText("Greeting");
                    case "Farewell" -> clickButton.setText("Farewell");
                    case "Random" -> clickButton.setText(new Random().nextBoolean() ? "Greeting" : "Farewell");
                }
            }
        });

        clickButton.addActionListener(e -> {
            String text = inputField.getText();
            if (text.isEmpty()) {
                messageLabel.setText("Please enter something!");
            } else {
                if (checkBox.isSelected()) {
                    text = text.toUpperCase();
                }
                if (reverseCheckBox.isSelected()) {
                    text = new StringBuilder(text).reverse().toString();
                }
                if (colorCheckBox.isSelected()) {
                    textArea.setForeground(new Color(new Random().nextInt(0xFFFFFF)));
                } else {
                    textArea.setForeground(Color.BLACK);
                }
                String action = (String) comboBox.getSelectedItem();
                if (action != null) {
                    switch (action) {
                        case "Greeting" -> {
                            messageLabel.setText("Hello, " + text + "!");
                            textArea.append("Greeted: " + text + "\n");
                        }
                        case "Farewell" -> {
                            messageLabel.setText("Goodbye, " + text + "!");
                            textArea.append("Farewelled: " + text + "\n");
                        }
                        case "Random" -> {
                            messageLabel.setText("You said: " + text);
                            textArea.append("Random: " + text + "\n");
                        }
                    }
                }
            }
        });

        clearButton.addActionListener(e -> {
            inputField.setText("");
            messageLabel.setText("Cleared input.");
            charCountLabel.setText("Characters: 0");
        });

        clearLogButton.addActionListener(e -> textArea.setText(""));

        surpriseButton.addActionListener(e -> JOptionPane.showMessageDialog(Main.this, "Surprise! You clicked the button!", "Surprise", JOptionPane.INFORMATION_MESSAGE));

        countButton.addActionListener(e -> {
            String text = inputField.getText();
            charCountLabel.setText("Characters: " + text.length());
        });

        wordCountButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
            JOptionPane.showMessageDialog(Main.this, "Word Count: " + wordCount, "Word Count", JOptionPane.INFORMATION_MESSAGE);
        });

        copyButton.addActionListener(e -> {
            String text = textArea.getText();
            if (!text.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                JOptionPane.showMessageDialog(Main.this, "Text copied to clipboard!", "Clipboard", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(Main.this, "Nothing to copy!", "Clipboard", JOptionPane.WARNING_MESSAGE);
            }
        });

        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateCount();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateCount();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateCount();
            }

            private void updateCount() {
                charCountLabel.setText("Characters: " + inputField.getText().length());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
