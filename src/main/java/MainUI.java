import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;

public class MainUI extends JPanel {

    public static JTable HighlighterTable1;

    public MainUI() {

        this.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel highlighterPanel = new JPanel(new BorderLayout());
        JPanel highlighter1 = new JPanel(new BorderLayout());

        highlighterPanel.add(highlighter1, BorderLayout.CENTER);
        tabbedPane.addTab("Highlighter", highlighterPanel);

        // Create buttons variables.
        JPanel highlightButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadEndpointBtn = new JButton("Load Endpoints");
        JButton removeEndpointBtn = new JButton("Remove Endpoints");
        JButton addEndpointBtn = new JButton("Add Endpoints");
        JButton exportEndpointBtn = new JButton("Export");

        HighlighterTable1 = new JTable(new DefaultTableModel(new Object[]{"Endpoints"}, 0));
        HighlighterTable1.setDragEnabled(true);
        HighlighterTable1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        highlighter1.add(new JScrollPane(HighlighterTable1), BorderLayout.CENTER);
        highlightButtonPanel.add(loadEndpointBtn);
        highlightButtonPanel.add(addEndpointBtn);
        highlightButtonPanel.add(removeEndpointBtn);
        highlightButtonPanel.add(exportEndpointBtn);
        highlighter1.add(highlightButtonPanel, BorderLayout.SOUTH);

        this.add(tabbedPane, BorderLayout.CENTER);  // DO NOT REMOVE.

        loadEndpointBtn.addActionListener(e -> handleLoadEndpoints());

        removeEndpointBtn.addActionListener(e -> handleRemoveEndpoint());

        addEndpointBtn.addActionListener(e -> handleAddEndpoint());

        exportEndpointBtn.addActionListener(e -> txthandleExport());
    }

    private void handleLoadEndpoints() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
                updateHighlighterTableWithContent(content.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateHighlighterTableWithContent(String content) {
        DefaultTableModel model = (DefaultTableModel) HighlighterTable1.getModel();
        model.setRowCount(0);

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            model.addRow(new Object[]{line});
        }
    }

    private void txthandleExport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("txt files", "txt"));
        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (!selectedFile.getAbsolutePath().endsWith(".txt")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                DefaultTableModel model1 = (DefaultTableModel) HighlighterTable1.getModel();

                for (int i = 0; i < model1.getColumnCount(); i++) {
                    writer.write(model1.getColumnName(i) + (i < model1.getColumnCount() - 1 ? "," : ""));
                }
                writer.newLine();

                for (int i = 0; i < model1.getRowCount(); i++) {
                    for (int j = 0; j < model1.getColumnCount(); j++) {
                        writer.write(model1.getValueAt(i, j).toString() + (j < model1.getColumnCount() - 1 ? "," : ""));
                    }
                    writer.newLine();
                }

                writer.flush();
                JOptionPane.showMessageDialog(this, "Data exported successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error exporting data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAddEndpoint() {

        JTextArea textArea = new JTextArea(10, 30);
        JScrollPane scrollPane = new JScrollPane(textArea);
        int option = JOptionPane.showConfirmDialog(this, scrollPane,
                "Enter Endpoint (one per line)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String input = textArea.getText().trim();
            if (!input.isEmpty()) {
                DefaultTableModel model1 = (DefaultTableModel) HighlighterTable1.getModel();
                String[] endpoints = input.split("\\r?\\n");
                for (String endpoint : endpoints) {
                    endpoint = endpoint.trim();
                    if (!endpoint.isEmpty()) {
                        model1.addRow(new Object[]{endpoint});
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Endpoint cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleRemoveEndpoint() {
        DefaultTableModel model1 = (DefaultTableModel) HighlighterTable1.getModel();

        int[] selectedRows = HighlighterTable1.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                model1.removeRow(selectedRows[i]);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No rows selected to remove.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}