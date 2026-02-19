import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class newMainUI extends JPanel {
    private static final String COLUMN_ENDPOINT = "Endpoints";
    private static final String COLUMN_HIGHLIGHT_COLOR = "Highlight Color";
    private static final String KEY_PROXY_MATCH_RULES = "proxy_match_rules";
    private static final String KEY_API_MAPPED_ENDPOINTS = "api_mapped_endpoints";
    private static final String RULE_COLOR_SEPARATOR = "\u001F";
    public static JTable ProxyMatchTable;
    public static JTable APIMapperTable;
    private static final CopyOnWriteArrayList<ProxyRule> PROXY_RULES = new CopyOnWriteArrayList<>();
    private static final Set<String> MAPPED_ENDPOINTS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Object PERSISTENCE_LOCK = new Object();
    private static volatile PersistedObject extensionDataStore;
    private static volatile boolean persistenceReady = false;
    private static volatile boolean isRestoringFromPersistence = false;
    private JButton loadButton;
    private JButton addButton;
    private JButton exportButton;
    private JButton removeEndpointsButton;
    private JButton sortButton;
    private JButton uniqueSortBtnButton;
    private JButton copyListBtnButton;
    private JButton moveToHighlightButton;
    private JButton removeEndpointsButton2;
    private JButton removeEndpointsWithExtensionsButton;

    private static class ProxyRule {
        private final Pattern pattern;
        private final HighlightColor color;
        private final String rawRule;
        private final boolean exactLiteralRule;

        private ProxyRule(Pattern pattern, HighlightColor color, String rawRule, boolean exactLiteralRule) {
            this.pattern = pattern;
            this.color = color;
            this.rawRule = rawRule;
            this.exactLiteralRule = exactLiteralRule;
        }
    }


    public newMainUI() {
        initComponents();

        loadButton.addActionListener(e -> handleLoadEndpoints());
        removeEndpointsButton.addActionListener(e -> handleRemoveEndpoint(ProxyMatchTable));
        addButton.addActionListener(e -> handleAddEndpoint());
        exportButton.addActionListener(e -> txtHandleExport());
        sortButton.addActionListener(e -> handleSortEndpoint(ProxyMatchTable));

        copyListBtnButton.addActionListener(e -> handleCopyList());
        moveToHighlightButton.addActionListener(e -> handleMoveToHighlight());
        uniqueSortBtnButton.addActionListener(e -> handleSortEndpoint(APIMapperTable));
        removeEndpointsButton2.addActionListener(e -> handleRemoveEndpoint(APIMapperTable));
        removeEndpointsWithExtensionsButton.addActionListener(e -> handleRemoveEndpointsWithExtensions(APIMapperTable));
    }

    public static void initializePersistence(PersistedObject extensionData) {
        if (extensionData == null) {
            return;
        }

        synchronized (PERSISTENCE_LOCK) {
            extensionDataStore = extensionData;
            persistenceReady = true;
        }
        restoreFromPersistence();
    }

    public static String stripQueryAndFragment(String path) {
        if (path == null) {
            return "";
        }
        int questionIdx = path.indexOf('?');
        int fragmentIdx = path.indexOf('#');
        int end = path.length();
        if (questionIdx >= 0) {
            end = Math.min(end, questionIdx);
        }
        if (fragmentIdx >= 0) {
            end = Math.min(end, fragmentIdx);
        }
        return path.substring(0, end);
    }

    public static boolean matchesProxyRule(String path) {
        return findMatchingColor(path) != null;
    }

    public static boolean matchesAnyProxyRule(String path) {
        String candidate = path == null ? "" : path;

        for (ProxyRule rule : PROXY_RULES) {
            if (rule.exactLiteralRule && candidate.equals(rule.rawRule)) {
                return true;
            }
        }
        for (ProxyRule rule : PROXY_RULES) {
            if (!rule.exactLiteralRule && rule.pattern.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }

    public static HighlightColor findMatchingColor(String path) {
        String candidate = path == null ? "" : path;

        for (ProxyRule rule : PROXY_RULES) {
            if (rule.exactLiteralRule && candidate.equals(rule.rawRule)) {
                return rule.color;
            }
        }
        for (ProxyRule rule : PROXY_RULES) {
            if (!rule.exactLiteralRule && rule.pattern.matcher(candidate).matches()) {
                return rule.color;
            }
        }
        return null;
    }

    public static void addMappedEndpoint(String endpoint) {
        String normalized = normalizeCellValue(endpoint);
        if (normalized.isEmpty()) {
            return;
        }
        if (matchesAnyProxyRule(normalized)) {
            return;
        }

        synchronized (MAPPED_ENDPOINTS) {
            if (!MAPPED_ENDPOINTS.add(normalized)) {
                return;
            }
        }

        Runnable addRow = () -> {
            if (APIMapperTable != null) {
                DefaultTableModel model = (DefaultTableModel) APIMapperTable.getModel();
                model.addRow(new Object[]{normalized});
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            addRow.run();
        } else {
            SwingUtilities.invokeLater(addRow);
        }
        persistMappedEndpoints();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Left side: ProxyMatcher
        ProxyMatchTable = new JTable(new DefaultTableModel(new Object[]{COLUMN_ENDPOINT, COLUMN_HIGHLIGHT_COLOR}, 0));
        ProxyMatchTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int halfScreenWidth = Math.max(600, screenSize.width / 2);
        JScrollPane proxyMatchScrollPane = new JScrollPane(ProxyMatchTable);
        proxyMatchScrollPane.setPreferredSize(new Dimension(halfScreenWidth, 0));
        JComboBox<String> colorComboBox = new JComboBox<>();
        for (HighlightColor color : HighlightColor.values()) {
            colorComboBox.addItem(color.name());
        }
        ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorComboBox.setRenderer(colorCellRenderer);
        TableColumn endpointColumn = ProxyMatchTable.getColumnModel().getColumn(0);
        TableColumn colorColumn = ProxyMatchTable.getColumnModel().getColumn(1);
        endpointColumn.setMinWidth(260);
        endpointColumn.setPreferredWidth(500);
        colorColumn.setMinWidth(140);
        colorColumn.setPreferredWidth(180);
        colorColumn.setMaxWidth(240);
        colorColumn.setCellEditor(new DefaultCellEditor(colorComboBox));
        colorColumn.setCellRenderer(colorCellRenderer);
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

        JPanel proxyMatcherPanel = new JPanel(new BorderLayout());
        proxyMatcherPanel.add(proxyMatchScrollPane, BorderLayout.CENTER);
        proxyMatcherPanel.add(buttonPanel1, BorderLayout.SOUTH);

        // Right side: API Mapper
        APIMapperTable = new JTable(new DefaultTableModel(new Object[]{"Endpoints Mapped"}, 0));
        attachTablePersistenceListeners();
        copyListBtnButton = new JButton("Copy endpoints");
        moveToHighlightButton = new JButton("Move to highlight");
        uniqueSortBtnButton = new JButton("Sort endpoints");
        removeEndpointsButton2 = new JButton("Remove endpoints");
        removeEndpointsWithExtensionsButton = new JButton("Remove endpoints with extensions");

        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.setLayout(new BoxLayout(buttonPanel2, BoxLayout.Y_AXIS));
        JPanel buttonPanel2Row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel buttonPanel2Row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel2Row1.add(copyListBtnButton);
        buttonPanel2Row1.add(moveToHighlightButton);
        buttonPanel2Row1.add(uniqueSortBtnButton);
        buttonPanel2Row2.add(removeEndpointsButton2);
        buttonPanel2Row2.add(removeEndpointsWithExtensionsButton);
        buttonPanel2.add(buttonPanel2Row1);
        buttonPanel2.add(buttonPanel2Row2);

        JPanel apiMapperPanel = new JPanel(new BorderLayout());
        apiMapperPanel.add(new JScrollPane(APIMapperTable), BorderLayout.CENTER);
        apiMapperPanel.add(buttonPanel2, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, proxyMatcherPanel, apiMapperPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);

        add(splitPane, BorderLayout.CENTER);
        refreshProxyRulesFromTable();
        syncMappedEndpointsFromTable();
        restoreFromPersistence();
    }

    private void attachTablePersistenceListeners() {
        TableModelListener proxyListener = e -> {
            if (isRestoringFromPersistence) {
                return;
            }
            refreshProxyRulesFromTable();
        };
        TableModelListener mapperListener = e -> {
            if (isRestoringFromPersistence) {
                return;
            }
            syncMappedEndpointsFromTable();
        };

        ProxyMatchTable.getModel().addTableModelListener(proxyListener);
        APIMapperTable.getModel().addTableModelListener(mapperListener);
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
                    String normalized = normalizeCellValue(endpoint);
                    if (!normalized.isEmpty()) {
                        model1.addRow(new Object[]{normalized, HighlightColor.YELLOW.name()});
                    }
                }
                refreshProxyRulesFromTable();
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
            if (tableName == ProxyMatchTable) {
                refreshProxyRulesFromTable();
            } else if (tableName == APIMapperTable) {
                syncMappedEndpointsFromTable();
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
                for (int i = 0; i < model1.getRowCount(); i++) {
                    String endpoint = normalizeCellValue(String.valueOf(model1.getValueAt(i, 0)));
                    String colorName = normalizeColorValue(model1.getValueAt(i, 1));
                    if (endpoint.isEmpty()) {
                        continue;
                    }
                    writer.write(endpoint + "\t" + colorName);
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
        boolean firstNonEmptyLine = true;
        for (String line : lines) {
            String normalized = normalizeCellValue(line);
            if (normalized.isEmpty()) {
                continue;
            }
            if (firstNonEmptyLine && normalized.toLowerCase().startsWith("endpoints")) {
                firstNonEmptyLine = false;
                continue;
            }
            firstNonEmptyLine = false;
            ParsedRule parsedRule = parseRuleLine(normalized);
            if (!parsedRule.endpoint.isEmpty()) {
                model.addRow(new Object[]{parsedRule.endpoint, parsedRule.color.name()});
            }
        }
        refreshProxyRulesFromTable();
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
        if (tableName == ProxyMatchTable) {
            java.util.Set<String> uniqueEndpoints = new java.util.LinkedHashSet<>();
            java.util.Map<String, String> endpointColorMap = new java.util.LinkedHashMap<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String endpoint = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
                if (!endpoint.isEmpty()) {
                    uniqueEndpoints.add(endpoint);
                    endpointColorMap.putIfAbsent(endpoint, normalizeColorValue(model.getValueAt(i, 1)));
                }
            }
            model.setRowCount(0);
            for (String endpoint : uniqueEndpoints) {
                model.addRow(new Object[]{endpoint, endpointColorMap.getOrDefault(endpoint, HighlightColor.YELLOW.name())});
            }
            refreshProxyRulesFromTable();
        } else if (tableName == APIMapperTable) {
            java.util.Set<String> uniqueEndpoints = new java.util.LinkedHashSet<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String endpoint = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
                if (!endpoint.isEmpty()) {
                    uniqueEndpoints.add(endpoint);
                }
            }
            model.setRowCount(0);
            for (String endpoint : uniqueEndpoints) {
                model.addRow(new Object[]{endpoint});
            }
            syncMappedEndpointsFromTable();
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
                    String value = stripQueryAndFragment(tableName.getValueAt(i, 0).toString());
                    for (String extension : extensions) {
                        extension = extension.trim();
                        if (!extension.isEmpty() && value.endsWith(extension)) {
                            model1.removeRow(i);
                            break;
                        }
                    }
                }
                if (tableName == ProxyMatchTable) {
                    refreshProxyRulesFromTable();
                } else if (tableName == APIMapperTable) {
                    syncMappedEndpointsFromTable();
                }
            }
        }
    }

    private void handleMoveToHighlight() {
        int[] selectedRows = APIMapperTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more mapped endpoints to move.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel sourceModel = (DefaultTableModel) APIMapperTable.getModel();
        DefaultTableModel targetModel = (DefaultTableModel) ProxyMatchTable.getModel();

        java.util.Set<String> existingRules = new java.util.HashSet<>();
        for (int i = 0; i < targetModel.getRowCount(); i++) {
            String endpoint = normalizeCellValue(String.valueOf(targetModel.getValueAt(i, 0)));
            if (!endpoint.isEmpty()) {
                existingRules.add(endpoint);
            }
        }

        int movedCount = 0;
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int rowIndex = selectedRows[i];
            String endpoint = normalizeCellValue(String.valueOf(sourceModel.getValueAt(rowIndex, 0)));
            if (!endpoint.isEmpty() && existingRules.add(endpoint)) {
                targetModel.addRow(new Object[]{endpoint, HighlightColor.YELLOW.name()});
                movedCount++;
            }
            sourceModel.removeRow(rowIndex);
        }

        refreshProxyRulesFromTable();
        syncMappedEndpointsFromTable();

        JOptionPane.showMessageDialog(
                this,
                movedCount + " endpoint(s) moved to highlight table.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String normalizeCellValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void refreshProxyRulesFromTable() {
        if (ProxyMatchTable == null) {
            PROXY_RULES.clear();
            return;
        }

        DefaultTableModel model = (DefaultTableModel) ProxyMatchTable.getModel();
        CopyOnWriteArrayList<ProxyRule> rebuiltRules = new CopyOnWriteArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String rawRule = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
            HighlightColor color = parseHighlightColor(model.getValueAt(i, 1));
            if (rawRule.isEmpty()) {
                continue;
            }
            boolean exactLiteralRule = !containsRegexMeta(rawRule);
            try {
                rebuiltRules.add(new ProxyRule(Pattern.compile(rawRule), color, rawRule, exactLiteralRule));
            } catch (Exception ex) {
                // Keep behavior predictable: if regex is invalid, treat it as a literal value.
                rebuiltRules.add(new ProxyRule(Pattern.compile(Pattern.quote(rawRule)), color, rawRule, true));
            }
        }

        PROXY_RULES.clear();
        PROXY_RULES.addAll(rebuiltRules);
        persistProxyRulesFromTable();
    }

    private void syncMappedEndpointsFromTable() {
        if (APIMapperTable == null) {
            synchronized (MAPPED_ENDPOINTS) {
                MAPPED_ENDPOINTS.clear();
            }
            return;
        }

        DefaultTableModel model = (DefaultTableModel) APIMapperTable.getModel();
        LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String endpoint = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
            if (!endpoint.isEmpty()) {
                rebuilt.add(endpoint);
            }
        }

        synchronized (MAPPED_ENDPOINTS) {
            MAPPED_ENDPOINTS.clear();
            MAPPED_ENDPOINTS.addAll(rebuilt);
        }
        persistMappedEndpoints();
    }

    private static void restoreFromPersistence() {
        if (!persistenceReady || ProxyMatchTable == null || APIMapperTable == null) {
            return;
        }

        Runnable restoreAction = () -> {
            isRestoringFromPersistence = true;
            try {
                restoreProxyRulesFromPersistence();
                restoreMappedEndpointsFromPersistence();
            } finally {
                isRestoringFromPersistence = false;
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            restoreAction.run();
        } else {
            SwingUtilities.invokeLater(restoreAction);
        }
    }

    private static void restoreProxyRulesFromPersistence() {
        PersistedObject store = extensionDataStore;
        if (store == null || ProxyMatchTable == null) {
            return;
        }

        PersistedList<String> persistedRules = store.getStringList(KEY_PROXY_MATCH_RULES);
        if (persistedRules == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) ProxyMatchTable.getModel();
        model.setRowCount(0);
        for (String rule : persistedRules) {
            ParsedRule parsedRule = parsePersistedRule(rule);
            if (!parsedRule.endpoint.isEmpty()) {
                model.addRow(new Object[]{parsedRule.endpoint, parsedRule.color.name()});
            }
        }
        PROXY_RULES.clear();
        for (int i = 0; i < model.getRowCount(); i++) {
            String rawRule = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
            HighlightColor color = parseHighlightColor(model.getValueAt(i, 1));
            if (rawRule.isEmpty()) {
                continue;
            }
            boolean exactLiteralRule = !containsRegexMeta(rawRule);
            try {
                PROXY_RULES.add(new ProxyRule(Pattern.compile(rawRule), color, rawRule, exactLiteralRule));
            } catch (Exception ex) {
                PROXY_RULES.add(new ProxyRule(Pattern.compile(Pattern.quote(rawRule)), color, rawRule, true));
            }
        }
    }

    private static void restoreMappedEndpointsFromPersistence() {
        PersistedObject store = extensionDataStore;
        if (store == null || APIMapperTable == null) {
            return;
        }

        PersistedList<String> persistedEndpoints = store.getStringList(KEY_API_MAPPED_ENDPOINTS);
        if (persistedEndpoints == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) APIMapperTable.getModel();
        model.setRowCount(0);
        LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
        for (String endpoint : persistedEndpoints) {
            String normalized = normalizeCellValue(endpoint);
            if (!normalized.isEmpty() && rebuilt.add(normalized)) {
                model.addRow(new Object[]{normalized});
            }
        }

        synchronized (MAPPED_ENDPOINTS) {
            MAPPED_ENDPOINTS.clear();
            MAPPED_ENDPOINTS.addAll(rebuilt);
        }
    }

    private static void persistProxyRulesFromTable() {
        if (!persistenceReady || isRestoringFromPersistence || ProxyMatchTable == null) {
            return;
        }
        PersistedObject store = extensionDataStore;
        if (store == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) ProxyMatchTable.getModel();
        PersistedList<String> list = PersistedList.persistedStringList();
        for (int i = 0; i < model.getRowCount(); i++) {
            String rule = normalizeCellValue(String.valueOf(model.getValueAt(i, 0)));
            String color = normalizeColorValue(model.getValueAt(i, 1));
            if (!rule.isEmpty()) {
                list.add(serializeRule(rule, color));
            }
        }
        store.setStringList(KEY_PROXY_MATCH_RULES, list);
    }

    private static void persistMappedEndpoints() {
        if (!persistenceReady || isRestoringFromPersistence) {
            return;
        }
        PersistedObject store = extensionDataStore;
        if (store == null) {
            return;
        }

        PersistedList<String> list = PersistedList.persistedStringList();
        synchronized (MAPPED_ENDPOINTS) {
            for (String endpoint : MAPPED_ENDPOINTS) {
                String normalized = normalizeCellValue(endpoint);
                if (!normalized.isEmpty()) {
                    list.add(normalized);
                }
            }
        }
        store.setStringList(KEY_API_MAPPED_ENDPOINTS, list);
    }

    private static String normalizeColorValue(Object value) {
        HighlightColor parsed = parseHighlightColor(value);
        return parsed.name();
    }

    private static HighlightColor parseHighlightColor(Object rawValue) {
        String normalized = normalizeCellValue(rawValue == null ? "" : String.valueOf(rawValue)).toUpperCase();
        if (normalized.isEmpty()) {
            return HighlightColor.YELLOW;
        }
        try {
            return HighlightColor.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return HighlightColor.YELLOW;
        }
    }

    private static String serializeRule(String endpoint, String colorName) {
        return endpoint + RULE_COLOR_SEPARATOR + colorName;
    }

    private static boolean containsRegexMeta(String value) {
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '.' || ch == '^' || ch == '$' || ch == '|' || ch == '?' || ch == '*' || ch == '+'
                    || ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '{' || ch == '}') {
                return true;
            }
        }
        return false;
    }

    private static ParsedRule parsePersistedRule(String rawValue) {
        String normalized = normalizeCellValue(rawValue);
        if (normalized.isEmpty()) {
            return new ParsedRule("", HighlightColor.YELLOW);
        }
        int separatorIndex = normalized.indexOf(RULE_COLOR_SEPARATOR);
        if (separatorIndex < 0) {
            return new ParsedRule(normalized, HighlightColor.YELLOW);
        }

        String endpoint = normalizeCellValue(normalized.substring(0, separatorIndex));
        String colorName = normalizeCellValue(normalized.substring(separatorIndex + RULE_COLOR_SEPARATOR.length()));
        return new ParsedRule(endpoint, parseHighlightColor(colorName));
    }

    private static ParsedRule parseRuleLine(String line) {
        String normalized = normalizeCellValue(line);
        if (normalized.isEmpty()) {
            return new ParsedRule("", HighlightColor.YELLOW);
        }

        String[] tabSplit = normalized.split("\\t", 2);
        if (tabSplit.length == 2) {
            return new ParsedRule(normalizeCellValue(tabSplit[0]), parseHighlightColor(tabSplit[1]));
        }

        String[] commaSplit = normalized.split(",", 2);
        if (commaSplit.length == 2) {
            HighlightColor parsed = parseHighlightColor(commaSplit[1]);
            return new ParsedRule(normalizeCellValue(commaSplit[0]), parsed);
        }

        return new ParsedRule(normalized, HighlightColor.YELLOW);
    }

    private static class ParsedRule {
        private final String endpoint;
        private final HighlightColor color;

        private ParsedRule(String endpoint, HighlightColor color) {
            this.endpoint = endpoint;
            this.color = color;
        }
    }

    private static class ColorCellRenderer extends DefaultTableCellRenderer implements ListCellRenderer<Object> {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            applyColorStyle(value, isSelected, table.getSelectionBackground(), table.getSelectionForeground());
            return this;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setOpaque(true);
            applyColorStyle(value, isSelected, list.getSelectionBackground(), list.getSelectionForeground());
            return this;
        }

        private void applyColorStyle(Object value, boolean isSelected, Color selectedBg, Color selectedFg) {
            String label = normalizeCellValue(value == null ? "" : String.valueOf(value)).toUpperCase();
            setText(label.isEmpty() ? HighlightColor.YELLOW.name() : label);
            setHorizontalAlignment(SwingConstants.CENTER);

            HighlightColor highlightColor = parseHighlightColor(label);
            Color bg = mapUiColor(highlightColor);
            Color fg = textColorFor(bg);

            if (isSelected) {
                setBackground(selectedBg);
                setForeground(selectedFg);
            } else {
                setBackground(bg);
                setForeground(fg);
            }
        }
    }

    private static Color mapUiColor(HighlightColor color) {
        switch (color) {
            case RED:
                return new Color(255, 99, 99);
            case ORANGE:
                return new Color(255, 199, 82);
            case YELLOW:
                return new Color(245, 245, 103);
            case GREEN:
                return new Color(99, 239, 110);
            case CYAN:
                return new Color(92, 224, 247);
            case BLUE:
                return new Color(106, 127, 242);
            case PINK:
                return new Color(250, 171, 209);
            case MAGENTA:
                return new Color(230, 99, 225);
            case GRAY:
                return new Color(170, 170, 170);
            case NONE:
            default:
                return new Color(65, 65, 65);
        }
    }

    private static Color textColorFor(Color bg) {
        int luminance = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return luminance >= 140 ? Color.BLACK : Color.WHITE;
    }

}

