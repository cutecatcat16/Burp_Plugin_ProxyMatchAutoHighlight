import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;

public class newMainUI extends JPanel {
    public static JTable ProxyMatchTable;
    public static JTable APIMapperTable;
    private JButton loadButton;
    private JButton addButton;
    private JButton exportButton;
    private JButton removeEndpointsButton;
    private JButton sortButton;
    private JButton uniqueSortBtnButton;
    private JButton copyListBtnButton;
    private JButton removeEndpointsButton2;
    private JButton removeEndpointsWithExtensionsButton;


    public newMainUI() {
        initComponents();

        loadButton.addActionListener(e -> handleLoadEndpoints());
        removeEndpointsButton.addActionListener(e -> handleRemoveEndpoint(ProxyMatchTable));
        addButton.addActionListener(e -> handleAddEndpoint());
        exportButton.addActionListener(e -> txtHandleExport());
        sortButton.addActionListener(e -> handleSortEndpoint(ProxyMatchTable));

        copyListBtnButton.addActionListener(e -> handleCopyList());
        uniqueSortBtnButton.addActionListener(e -> handleSortEndpoint(APIMapperTable));
        removeEndpointsButton2.addActionListener(e -> handleRemoveEndpoint(APIMapperTable));
        removeEndpointsWithExtensionsButton.addActionListener(e -> handleRemoveEndpointsWithExtensions(APIMapperTable));
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        /*
        * First tab ProxyMatcher
        * */
        JTabbedPane tabbedPane1 = new JTabbedPane();
        JPanel panel1 = new JPanel(new BorderLayout());
        ProxyMatchTable = new JTable(new DefaultTableModel(new Object[]{"Endpoints"}, 0));
        loadButton = new JButton("Load endpoints");
        addButton = new JButton("Add endpoints");
        exportButton = new JButton("Export");
        removeEndpointsButton = new JButton("Remove endpoints");
        sortButton = new JButton("Sort endpoints");

        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel1.add(loadButton);
        buttonPanel1.add(addButton);
        buttonPanel1.add(removeEndpointsButton);
        buttonPanel1.add(exportButton);
        buttonPanel1.add(sortButton);

        panel1.add(new JScrollPane(ProxyMatchTable), BorderLayout.CENTER);
        panel1.add(buttonPanel1, BorderLayout.SOUTH);

        tabbedPane1.addTab("ProxyMatcher", panel1);
        add(tabbedPane1, BorderLayout.CENTER);

        /*
        * Second tab API Mapper
        * */
//        JTabbedPane tabbedPane2 = new JTabbedPane();
        JPanel panel2 = new JPanel(new BorderLayout());
        APIMapperTable = new JTable(new DefaultTableModel(new Object[]{"Endpoints Mapped"}, 0));
        copyListBtnButton = new JButton("Copy endpoints");
        uniqueSortBtnButton = new JButton("Sort endpoints");
        removeEndpointsButton2 = new JButton("Remove endpoints");
        removeEndpointsWithExtensionsButton = new JButton("Remove endpoints with extensions");

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel2.add(copyListBtnButton);
        buttonPanel2.add(uniqueSortBtnButton);
        buttonPanel2.add(removeEndpointsButton2);
        buttonPanel2.add(removeEndpointsWithExtensionsButton);

        panel2.add(new JScrollPane(APIMapperTable), BorderLayout.CENTER);
        panel2.add(buttonPanel2, BorderLayout.SOUTH);

        tabbedPane1.addTab("API Mapper", panel2);
        add(tabbedPane1, BorderLayout.CENTER);
    }

    private void handleAddEndpoint() {

        JTextArea textArea = new JTextArea(10, 30);
        JScrollPane scrollPane = new JScrollPane(textArea);
        int option = JOptionPane.showConfirmDialog(this, scrollPane,
                "Enter Endpoint (one per line)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String input = textArea.getText().trim();
            if (!input.isEmpty()) {
                DefaultTableModel model1 = (DefaultTableModel) ProxyMatchTable.getModel();
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

    private void handleRemoveEndpoint(JTable tableName) {
        DefaultTableModel model1 = (DefaultTableModel) tableName.getModel();

        int[] selectedRows = tableName.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                model1.removeRow(selectedRows[i]);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No rows selected to remove.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void txtHandleExport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("txt files", "txt"));
        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (!selectedFile.getAbsolutePath().endsWith(".txt")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                DefaultTableModel model1 = (DefaultTableModel) ProxyMatchTable.getModel();

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

    private void updateHighlighterTableWithContent(String content) {
        DefaultTableModel model = (DefaultTableModel) ProxyMatchTable.getModel();
        model.setRowCount(0);

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            model.addRow(new Object[]{line});
        }
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

    private void handleSortEndpoint(JTable tableName) {
        DefaultTableModel model = (DefaultTableModel) tableName.getModel();
        java.util.Set<String> uniqueEndpoints = new java.util.LinkedHashSet<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String endpoint = (String) model.getValueAt(i, 0);
            uniqueEndpoints.add(endpoint);
        }
        model.setRowCount(0);
        for (String endpoint : uniqueEndpoints) {

            model.addRow(new Object[]{endpoint});
        }
    }

    private void handleCopyList() {
        DefaultTableModel model = (DefaultTableModel) APIMapperTable.getModel();
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < model.getRowCount(); i++) {
            content.append(model.getValueAt(i, 0)).append("\n");
        }

        java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(content.toString()), null);

        JOptionPane.showMessageDialog(this, "All endpoints copied to clipboard.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRemoveEndpointsWithExtensions(JTable tableName) {

        JTextArea textArea = new JTextArea(10, 30);
        JScrollPane scrollPane = new JScrollPane(textArea);
        int option = JOptionPane.showConfirmDialog(this, scrollPane,
                "Enter Extension to remove (one per line)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String input = textArea.getText().trim();
            if (!input.isEmpty()) {
                DefaultTableModel model1 = (DefaultTableModel) tableName.getModel();
                String[] extensions = input.split("\\r?\\n");
                for (int i = model1.getRowCount() - 1; i >= 0; i--) {
                    String value = tableName.getValueAt(i, 0).toString();
                    for (String extension : extensions) {
                        extension = extension.trim();
                        if (!extension.isEmpty() && value.endsWith(extension)) {
                            model1.removeRow(i);
                            break;
                        }
                    }
                }
            }
        }
    }

}

