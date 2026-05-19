import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class SQLVisualizerUI extends JFrame {

    // =====================================================
    // ================= COMPONENTS ========================
    // =====================================================

    JTextArea queryArea;

    JTable resultTable;

    DefaultTableModel tableModel;

    JTable vizTable;

    DefaultTableModel vizTableModel;

    JTextArea infoArea;

    JButton executeButton;

    JButton optimizeButton;

    JButton visualizeButton;

    JButton clearButton;

    VisualizationCanvas vizCanvas;

    JTabbedPane tabbedPane;

    // =====================================================
    // ================= VISUALIZATION VARIABLES ===========
    // =====================================================

    int currentAnimatedRow = -1;
    int matchedRow = -1;
    int recentlyInsertedRow = -1;
    String currentQueryType = "SELECT";

    // =====================================================
    // ================= DATABASE ==========================
    // =====================================================

   String url ="jdbc:postgresql://localhost:5434/sql_visualizer?options=-c%20TimeZone=UTC";

    String user = "postgres";

    String password = "postgres";

    // HISTORY LOG
    DefaultTableModel historyModel;
    JTable historyTable;

    // =====================================================
    // ================= CONSTRUCTOR =======================
    // =====================================================

    public SQLVisualizerUI() {

        // =====================================================
        // ================= FRAME =============================
        // =====================================================

        setTitle("SQL Query Execution Engine");

        setSize(1400, 800);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        setLayout(new GridLayout(1, 2));

        // =====================================================
        // ================= LEFT PANEL ========================
        // =====================================================

        JPanel leftPanel = new JPanel();

        leftPanel.setLayout(new BorderLayout());

        leftPanel.setBorder(
                new LineBorder(Color.BLACK, 5)
        );

        // ================= QUERY AREA =================

        queryArea = new JTextArea();

        queryArea.setFont(
                new Font("Consolas", Font.PLAIN, 18)
        );

        queryArea.setText("SELECT * FROM students WHERE id = 3;");

        JScrollPane queryScroll = new JScrollPane(queryArea);

        queryScroll.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 3),
                        "SQL QUERY EDITOR"
                )
        );

        leftPanel.add(queryScroll, BorderLayout.CENTER);

        // ================= EXECUTE BUTTON =================

        JPanel executePanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        executeButton = new JButton("EXECUTE QUERY");

        executeButton.setPreferredSize(
                new Dimension(220, 50)
        );

        executeButton.setFont(
                new Font("Arial", Font.BOLD, 16)
        );

        executePanel.add(executeButton);

        leftPanel.add(executePanel, BorderLayout.SOUTH);

        // =====================================================
        // ================= RIGHT PANEL =======================
        // =====================================================

        JPanel rightPanel = new JPanel();

        rightPanel.setLayout(new BorderLayout());

        rightPanel.setBorder(
                new LineBorder(Color.BLACK, 5)
        );

        // ================= TABS =================

        tabbedPane = new JTabbedPane();

        // =====================================================
        // ================= RESULT TABLE ======================
        // =====================================================

        JScrollPane tableScroll = new JScrollPane();

        tableScroll.setBorder(
                BorderFactory.createLineBorder(Color.BLACK, 3)
        );

        // =====================================================
        // ================= RESULT TABLE RENDERER =============
        // =====================================================

        tableModel = new DefaultTableModel();

        resultTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
                return c;
            }
        };

        resultTable.setRowHeight(35);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 15));
        tableScroll.setViewportView(resultTable);

        // =====================================================
        // ================= INFORMATION AREA ==================
        // =====================================================

        infoArea = new JTextArea();

        infoArea.setEditable(false);

        infoArea.setFont(
                new Font("Monospaced", Font.PLAIN, 15)
        );

        JScrollPane infoScroll = new JScrollPane(infoArea);

        infoScroll.setBorder(
                BorderFactory.createLineBorder(Color.BLACK, 3)
        );

        // ================= ADD TABS =================

        tabbedPane.addTab("RESULTS", tableScroll);

        tabbedPane.addTab("INFORMATION", infoScroll);

        // =====================================================
        // ================= VISUALIZATION TAB =================
        // =====================================================

        JPanel vizPanel = new JPanel(new BorderLayout());

        vizTableModel = new DefaultTableModel();

        vizTable = new JTable(vizTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                
                // COLOR CONVENTIONS
                if (row == currentAnimatedRow) {
                    c.setBackground(Color.YELLOW); // SCANNING
                    c.setForeground(Color.BLACK);
                } else if (row == matchedRow) {
                    if (currentQueryType.equals("SELECT")) {
                        c.setBackground(Color.CYAN); // MATCHED
                    } else if (currentQueryType.equals("UPDATE")) {
                        c.setBackground(new Color(100, 149, 237)); // BLUE (Cornflower Blue)
                    } else if (currentQueryType.equals("DELETE")) {
                        c.setBackground(new Color(255, 69, 0)); // RED (Orange Red)
                    } else {
                        c.setBackground(Color.GREEN);
                    }
                    c.setForeground(Color.WHITE);
                } else if (row == recentlyInsertedRow) {
                    c.setBackground(Color.GREEN); // JUST INSERTED
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                
                return c;
            }
        };

        vizTable.setRowHeight(35);
        vizTable.setFont(new Font("Arial", Font.PLAIN, 15));

        JScrollPane vizScroll = new JScrollPane(vizTable);
        vizScroll.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        vizScroll.setPreferredSize(new Dimension(800, 200));
        vizPanel.add(vizScroll, BorderLayout.CENTER);

        vizCanvas = new VisualizationCanvas();
        vizCanvas.setPreferredSize(new Dimension(800, 250));
        vizPanel.add(vizCanvas, BorderLayout.SOUTH);

        tabbedPane.addTab("VISUALIZATION", vizPanel);

        // ================= HISTORY TAB =================
        historyModel = new DefaultTableModel(new String[]{"ID", "SQL COMMAND", "STATUS", "TIME"}, 0);
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(30);
        tabbedPane.addTab("HISTORY", new JScrollPane(historyTable));

        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        // =====================================================
        // ================= BOTTOM BUTTON PANEL ===============
        // =====================================================

        JPanel bottomButtonPanel = new JPanel();

        bottomButtonPanel.setPreferredSize(
                new Dimension(100, 120)
        );

        bottomButtonPanel.setBorder(
                BorderFactory.createLineBorder(Color.BLACK, 4)
        );

        optimizeButton = new JButton("OPTIMIZE");

        visualizeButton = new JButton("VISUALIZE");

        clearButton = new JButton("CLEAR");

        optimizeButton.setPreferredSize(
                new Dimension(180, 45)
        );

        visualizeButton.setPreferredSize(
                new Dimension(180, 45)
        );

        clearButton.setPreferredSize(
                new Dimension(180, 45)
        );

        optimizeButton.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        visualizeButton.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        clearButton.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        bottomButtonPanel.add(optimizeButton);

        bottomButtonPanel.add(visualizeButton);

        bottomButtonPanel.add(clearButton);

        rightPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

        // =====================================================
        // ================= OPTIMIZE ACTION ===================
        // =====================================================

        optimizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                provideOptimizationSuggestions();
            }
        });

        // =====================================================
        // ================= ADD PANELS ========================
        // =====================================================

        add(leftPanel);

        add(rightPanel);

        // =====================================================
        // ================= BUTTON ACTIONS ====================
        // =====================================================

        executeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                executeSQLQuery();
            }
        });

        visualizeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                startVisualization();
            }
        });

        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                queryArea.setText("");

                infoArea.setText("");

                tableModel.setRowCount(0);

                tableModel.setColumnCount(0);

                vizTableModel.setRowCount(0);

                vizTableModel.setColumnCount(0);

                currentAnimatedRow = -1;

                matchedRow = -1;

                vizTable.repaint();

                vizCanvas.setStatus("IDLE");
                vizCanvas.setJoinStep(-1);
                vizCanvas.setBTreeStep(-1);
                vizCanvas.repaint();
            }
        });

        setVisible(true);
    }

    // =====================================================
    // ================= EXECUTE SQL METHOD ================
    // =====================================================

    private void executeSQLQuery() {

        String query = queryArea.getText().trim();

        infoArea.setText("");

        tableModel.setRowCount(0);

        tableModel.setColumnCount(0);

        try {

            // REMOVE EXTRA SEMICOLON

            if (query.endsWith(";")) {

                query = query.substring(0, query.length() - 1);
            }

            long startTime = System.currentTimeMillis();

            Connection conn = DriverManager.getConnection(
                    url,
                    user,
                    password
            );

            Statement stmt = conn.createStatement();
            String lowerQuery = query.toLowerCase().trim();

            if (lowerQuery.startsWith("select")) {
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                Vector<String> columns = new Vector<>();
                for (int i = 1; i <= columnCount; i++) columns.add(metaData.getColumnName(i));
                tableModel.setColumnIdentifiers(columns);

                int rowsFound = 0;
                while (rs.next()) {
                    rowsFound++;
                    Vector<String> row = new Vector<>();
                    for (int i = 1; i <= columnCount; i++) row.add(rs.getString(i));
                    tableModel.addRow(row);
                }

                if (rowsFound == 0) {
                    tableModel.setColumnIdentifiers(new String[]{"STATUS"});
                    tableModel.addRow(new String[]{"NO RESULTS FOUND"});
                    infoArea.append(">> SELECT EXECUTED SUCCESSFULLY\n>> STATUS: EMPTY SET\n");
                } else {
                    infoArea.append(">> SELECT EXECUTED SUCCESSFULLY\n>> ROWS RETURNED: " + rowsFound + "\n");
                }
            } else {
                // FOR DML (INSERT/UPDATE/DELETE)
                int rowsAffected = stmt.executeUpdate(query);
                long endTime = System.currentTimeMillis();
                long execTime = endTime - startTime;

                Vector<String> columns = new Vector<>();
                columns.add("OPERATION");
                columns.add("ROWS AFFECTED");
                columns.add("EXECUTION TIME");
                columns.add("STATUS");
                tableModel.setColumnIdentifiers(columns);

                Vector<String> resultRow = new Vector<>();
                String opType = lowerQuery.split("\\s+")[0].toUpperCase();
                resultRow.add(opType);
                resultRow.add(String.valueOf(rowsAffected));
                resultRow.add(execTime + " ms");
                resultRow.add("SUCCESSFUL");
                tableModel.addRow(resultRow);

                infoArea.append(">> " + opType + " EXECUTED SUCCESSFULLY\n");
                infoArea.append(">> ROWS AFFECTED : " + rowsAffected + "\n");
                infoArea.append(">> EXECUTION TIME : " + execTime + " ms\n");
                
                historyModel.addRow(new Object[]{historyModel.getRowCount() + 1, query, "SUCCESS", execTime + " ms"});
                
                fetchExecutionPlan(query, conn);
                tabbedPane.setSelectedIndex(0);
                conn.close();
                return; 
            }

            long endTime = System.currentTimeMillis();
            long execTime = endTime - startTime;
            infoArea.append(">> EXECUTION TIME : " + execTime + " ms\n");
            
            historyModel.addRow(new Object[]{historyModel.getRowCount() + 1, query, "SUCCESS", execTime + " ms"});
            
            fetchExecutionPlan(query, conn);
            tabbedPane.setSelectedIndex(0); 
            conn.close();

        } catch (Exception ex) {
            historyModel.addRow(new Object[]{historyModel.getRowCount() + 1, query, "FAILED", "N/A"});
            
            tableModel.setColumnIdentifiers(new String[]{"ERROR MESSAGE"});
            tableModel.addRow(new String[]{ex.getMessage()});
            
            infoArea.setText("ERROR DETECTED:\n" + ex.getMessage());
            tabbedPane.setSelectedIndex(0); 
        }
    }

    // =====================================================
    // ================= FETCH EXECUTION PLAN ==============
    // =====================================================

    private void fetchExecutionPlan(String query, Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("EXPLAIN (ANALYZE, VERBOSE, COSTS, BUFFERS) " + query);

            infoArea.append("=========================================\n");
            infoArea.append("         POSTGRESQL EXECUTION PLAN       \n");
            infoArea.append("=========================================\n\n");

            while (rs.next()) {
                infoArea.append(rs.getString(1) + "\n");
            }

        } catch (Exception ex) {
            infoArea.append("\nCould not fetch execution plan: " + ex.getMessage());
        }
    }

    // =====================================================
    // ================= OPTIMIZATION SUGGESTIONS ==========
    // =====================================================

    private void provideOptimizationSuggestions() {
        String planText = infoArea.getText();
        if (planText.isEmpty() || !planText.contains("EXECUTION PLAN")) {
            JOptionPane.showMessageDialog(this, "Please execute a SELECT query first to analyze the plan.");
            return;
        }

        StringBuilder suggestions = new StringBuilder();
        suggestions.append("=========================================\n");
        suggestions.append("       OPTIMIZATION RECOMMENDATIONS      \n");
        suggestions.append("=========================================\n\n");

        boolean hasSuggestions = false;

        if (planText.contains("Seq Scan")) {
            suggestions.append("• DETECTED: Sequential Scan (Full Table Scan)\n");
            suggestions.append("  SUGGESTION: Add an index on the filtered columns to speed up lookup.\n\n");
            hasSuggestions = true;
        }

        if (planText.contains("Sort Method: external merge")) {
            suggestions.append("• DETECTED: Disk-based Sorting\n");
            suggestions.append("  SUGGESTION: Increase 'work_mem' or add an index to avoid expensive sorting.\n\n");
            hasSuggestions = true;
        }

        if (planText.contains("Filter:")) {
            suggestions.append("• DETECTED: Post-scan filtering\n");
            suggestions.append("  SUGGESTION: Ensure the filter column is part of an index (e.g., B-Tree index).\n\n");
            hasSuggestions = true;
        }

        if (!hasSuggestions) {
            suggestions.append("• The current query plan looks optimal!\n");
        }

        infoArea.setText(suggestions.toString() + "\n\n--- ORIGINAL PLAN ---\n\n" + planText);
    }

    // =====================================================
    // ================= VISUALIZATION =====================
    // =====================================================

    private void startVisualization() {
        try {
            tabbedPane.setSelectedIndex(2); // Switch to VISUALIZATION tab
            
            String planText = infoArea.getText(); // PRESERVE CASE
            String query = queryArea.getText().trim(); // PRESERVE CASE
            String lowerQuery = query.toLowerCase();

            // RESET STATE
            resetVisualizationState();
            vizCanvas.setMetadata(planText); // Pass original case metadata
            
            // =====================================================
            // ================= UNIVERSAL DISPATCHER ==============
            // =====================================================

            if (lowerQuery.startsWith("insert")) {
                runInsertAnimation(query);
            } else if (lowerQuery.startsWith("update") || lowerQuery.startsWith("delete") || lowerQuery.startsWith("select")) {
                // All row-based operations will now use the Universal Scanning Engine
                runUniversalScanAnimation(query, planText);
            } else if (planText.toLowerCase().contains("join")) {
                runJoinAnimation(query, planText);
            } else if (planText.toLowerCase().contains("sort")) {
                runSortAnimation(query, planText);
            } else {
                runUniversalScanAnimation(query, planText); // Fallback to scanning
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Visualization Error: " + ex.getMessage());
        }
    }

    private void resetVisualizationState() {
        currentAnimatedRow = -1;
        matchedRow = -1;
        recentlyInsertedRow = -1;
        currentQueryType = "SELECT";
        vizTableModel.setRowCount(0);
        vizTableModel.setColumnCount(0);
        vizTable.repaint();
        vizCanvas.setStatus("IDLE");
        vizCanvas.setJoinStep(-1);
        vizCanvas.setBTreeStep(-1);
        vizCanvas.repaint();
    }

    private String extractTableName(String query) {
        String lowerQuery = query.toLowerCase();
        int fromIndex = lowerQuery.indexOf("from");
        if (fromIndex == -1) {
            // Check for UPDATE
            int updateIndex = lowerQuery.indexOf("update");
            if (updateIndex != -1) {
                String afterUpdate = query.substring(updateIndex + 6).trim();
                return afterUpdate.split("\\s+")[0].replace(";", "");
            }
            // Check for DELETE
            int deleteIndex = lowerQuery.indexOf("delete from");
            if (deleteIndex != -1) {
                String afterDelete = query.substring(deleteIndex + 11).trim();
                return afterDelete.split("\\s+")[0].replace(";", "");
            }
            // Check for INSERT
            int insertIndex = lowerQuery.indexOf("insert into");
            if (insertIndex != -1) {
                String afterInsert = query.substring(insertIndex + 11).trim();
                return afterInsert.split("\\s+")[0].replace(";", "");
            }
            return null;
        }
        
        String afterFrom = query.substring(fromIndex + 5).trim();
        String[] parts = afterFrom.split("\\s+");
        return parts[0].replace(";", "");
    }

    private void fetchFullTable(String tableName) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Vector<String> columns = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            vizTableModel.setColumnIdentifiers(columns);

            while (rs.next()) {
                Vector<String> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                vizTableModel.addRow(row);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String extractFilterFromPlan(String planText) {
        String filter = "";
        String lowerPlan = planText.toLowerCase();
        int filterIndex = lowerPlan.indexOf("filter:");
        if (filterIndex == -1) filterIndex = lowerPlan.indexOf("index cond:");
        
        if (filterIndex != -1) {
            int lineEnd = planText.indexOf("\n", filterIndex);
            filter = planText.substring(planText.indexOf(":", filterIndex) + 1, lineEnd == -1 ? planText.length() : lineEnd).trim();
        }
        return filter;
    }

    private String extractFilterFromQuery(String query) {
        String lower = query.toLowerCase();
        int whereIndex = lower.indexOf("where");
        if (whereIndex != -1) {
            String filter = query.substring(whereIndex + 5).trim();
            // Remove trailing semicolon
            if (filter.endsWith(";")) filter = filter.substring(0, filter.length() - 1);
            return filter;
        }
        return "";
    }

    private String extractUpdateValue(String query) {
        String lower = query.toLowerCase();
        int setIndex = lower.indexOf("set");
        int whereIndex = lower.indexOf("where");
        if (setIndex != -1) {
            String setClause = "";
            if (whereIndex != -1) {
                setClause = query.substring(setIndex + 3, whereIndex).trim();
            } else {
                setClause = query.substring(setIndex + 3).trim();
            }
            if (setClause.contains("=")) {
                String val = setClause.split("=")[1].trim();
                if (val.endsWith(";")) val = val.substring(0, val.length() - 1);
                return val.replace("'", "").replace("\"", "");
            }
        }
        return "UPDATED";
    }

    private boolean evaluateFilter(int rowIdx, String filter) {
        if (filter == null || filter.trim().isEmpty()) return false; // Don't match if no filter

        try {
            String rawFilter = filter.trim();
            // Remove common PostgreSQL noise
            rawFilter = rawFilter.replaceAll("(?i)[a-zA-Z0-9_]+\\.", ""); // table prefixes
            rawFilter = rawFilter.replaceAll("(?i)::[a-z]+", "");        // casts
            
            String cleanFilter = rawFilter.replace("(", "").replace(")", "")
                                         .replace("'", "").replace("\"", "")
                                         .trim();
            
            String op = "=";
            String[] parts = null;
            String lowerFilter = cleanFilter.toLowerCase();
            if (lowerFilter.contains(">=")) { op = ">="; parts = cleanFilter.split("(?i)>="); }
            else if (lowerFilter.contains("<=")) { op = "<="; parts = cleanFilter.split("(?i)<="); }
            else if (lowerFilter.contains("=")) { op = "="; parts = cleanFilter.split("="); }
            else if (lowerFilter.contains(">")) { op = ">"; parts = cleanFilter.split(">"); }
            else if (lowerFilter.contains("<")) { op = "<"; parts = cleanFilter.split("<"); }
            
            if (parts != null && parts.length >= 2) {
                String colName = parts[0].trim().toLowerCase();
                String targetVal = parts[1].trim();

                int colIdx = -1;
                for (int i = 0; i < vizTable.getColumnCount(); i++) {
                    String actualColName = vizTable.getColumnName(i).toLowerCase();
                    if (actualColName.equals(colName) || colName.contains(actualColName)) {
                        colIdx = i;
                        break;
                    }
                }

                if (colIdx != -1) {
                    Object valObj = vizTable.getValueAt(rowIdx, colIdx);
                    if (valObj == null) return false;
                    String actualVal = valObj.toString().trim();

                    if (op.equals("=")) {
                        try {
                            return Double.parseDouble(actualVal) == Double.parseDouble(targetVal);
                        } catch (Exception nfe) {
                            return actualVal.equalsIgnoreCase(targetVal);
                        }
                    }
                    try {
                        double a = Double.parseDouble(actualVal);
                        double b = Double.parseDouble(targetVal);
                        if (op.equals(">")) return a > b;
                        if (op.equals("<")) return a < b;
                        if (op.equals(">=")) return a >= b;
                        if (op.equals("<=")) return a <= b;
                    } catch (Exception nfe) {}
                }
            }
        } catch (Exception ex) {}
        return false;
    }

    private void runInsertAnimation(String query) {
        String tableName = extractTableName(query);
        if (tableName != null) fetchFullTable(tableName);

        // Parse INSERT: INSERT INTO students VALUES (id, 'name', marks)
        try {
            String valuesPart = query.substring(query.toLowerCase().indexOf("values") + 6).trim();
            valuesPart = valuesPart.replace("(", "").replace(")", "").replace(";", "");
            String[] parts = valuesPart.split(",");
            final String[] rowData = new String[parts.length];
            for (int i = 0; i < parts.length; i++) rowData[i] = parts[i].trim().replace("'", "");

            // REMOVE THE DUPLICATE (Since it's already in the DB after Execute)
            for (int r = vizTableModel.getRowCount() - 1; r >= 0; r--) {
                boolean match = true;
                for (int c = 0; c < Math.min(vizTableModel.getColumnCount(), rowData.length); c++) {
                    if (!vizTableModel.getValueAt(r, c).toString().equals(rowData[c])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    vizTableModel.removeRow(r);
                    break; // Only remove one instance
                }
            }

            vizCanvas.setFloatingRowData(rowData);
            vizCanvas.setStatus("INSERT: LOCATING POSITION...");
            
            // Animation Stages
            final int[] stage = {0}; // 0: Floating, 1: Moving, 2: Inserting, 3: Flashing
            final int[] x = {50};
            final int[] y = {200};
            
            Timer insertTimer = new Timer(50, null);
            insertTimer.addActionListener(e -> {
                if (stage[0] == 0) { // FLOATING
                    vizCanvas.setStatus("INSERT: ALLOCATING SPACE...");
                    if (++stage[0] > 10) stage[0] = 1; 
                } else if (stage[0] == 1) { // MOVING
                    y[0] -= 5;
                    vizCanvas.setFloatingRowPos(x[0], y[0]);
                    if (y[0] <= 50) stage[0] = 2;
                } else if (stage[0] == 2) { // INSERTING
                    insertTimer.stop();
                    vizCanvas.setFloatingRowData(null);
                    vizTableModel.addRow(rowData);
                    recentlyInsertedRow = vizTableModel.getRowCount() - 1;
                    vizTable.repaint();
                    vizCanvas.setStatus("INSERT: COMMITTING ROW...");
                    
                    // Flash Green
                    Timer flashTimer = new Timer(1500, ev -> {
                        recentlyInsertedRow = -1;
                        vizTable.repaint();
                        vizCanvas.setStatus("INSERT COMPLETE");
                        JOptionPane.showMessageDialog(null, "INSERT SUCCESSFUL\n\nDBMS has allocated space and inserted the row.");
                        ((Timer)ev.getSource()).stop();
                    });
                    flashTimer.setRepeats(false);
                    flashTimer.start();
                }
                vizCanvas.repaint();
            });
            insertTimer.start();
            
        } catch (Exception ex) {
            vizCanvas.setStatus("INSERT ERROR: PARSING FAILED");
        }
    }

    private void runUniversalScanAnimation(String query, String planText) {
        String tableName = extractTableName(query);
        if (tableName != null) fetchFullTable(tableName);

        currentQueryType = "SELECT";
        if (query.toLowerCase().startsWith("update")) currentQueryType = "UPDATE";
        if (query.toLowerCase().startsWith("delete")) currentQueryType = "DELETE";

        // Try plan first, then fallback to direct query parsing
        String filterPart = extractFilterFromPlan(planText);
        if (filterPart.isEmpty()) {
            filterPart = extractFilterFromQuery(query);
        }
        final String finalFilter = filterPart;

        // STATE RESTORATION FOR SIMULATION
        if (currentQueryType.equals("UPDATE") || currentQueryType.equals("DELETE")) {
            boolean found = false;
            for (int r = 0; r < vizTableModel.getRowCount(); r++) {
                if (evaluateFilter(r, finalFilter)) {
                    found = true;
                    if (currentQueryType.equals("UPDATE")) {
                        vizTableModel.setValueAt("[MODIFYING...]", r, 1);
                        infoArea.append("\n[SIMULATION] Target row located. Preparing to update to new value.");
                    }
                    break;
                }
            }
            if (!found && currentQueryType.equals("DELETE")) {
                // Try to extract ID from filter for the placeholder
                Vector<String> placeholder = new Vector<>();
                String idVal = "0";
                if (finalFilter.contains("=")) {
                    String[] fParts = finalFilter.split("=");
                    idVal = fParts[fParts.length - 1].trim().replace("(", "").replace(")", "").replace("'", "").replace(";", "");
                }
                placeholder.add(idVal);
                placeholder.add("TARGET_FOR_DELETE");
                placeholder.add("0");

                // Find correct sorted position (Assuming column "id" exists)
                int insertPos = 0;
                int idCol = -1;
                for (int i = 0; i < vizTable.getColumnCount(); i++) {
                    if (vizTable.getColumnName(i).toLowerCase().contains("id")) {
                        idCol = i;
                        break;
                    }
                }

                if (idCol != -1) {
                    try {
                        double targetId = Double.parseDouble(idVal);
                        for (int r = 0; r < vizTableModel.getRowCount(); r++) {
                            try {
                                double currentId = Double.parseDouble(vizTableModel.getValueAt(r, idCol).toString());
                                if (currentId < targetId) {
                                    insertPos = r + 1;
                                } else {
                                    break;
                                }
                            } catch (Exception nfe) {
                                // Skip non-numeric IDs during sort check
                            }
                        }
                    } catch (Exception ex) {
                        insertPos = 0;
                    }
                }

                vizTableModel.insertRow(insertPos, placeholder);
                vizTable.scrollRectToVisible(vizTable.getCellRect(insertPos, 0, true));
                infoArea.append("\n[SIMULATION] Restored target row at its sorted position (Row " + (insertPos + 1) + ").");
            }
            vizTable.repaint();
        }

        Timer timer = new Timer(600, null);
        final int[] currentRow = {0};
        final int[] matchCount = {0};

        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentRow[0] < vizTable.getRowCount()) {
                    currentAnimatedRow = currentRow[0];
                    vizTable.repaint();
                    vizCanvas.setStatus("SCANNING: ROW " + (currentRow[0] + 1) + " | Condition: " + finalFilter);

                    if (evaluateFilter(currentRow[0], finalFilter)) {
                        timer.stop();
                        matchCount[0]++;
                        matchedRow = currentRow[0];
                        currentAnimatedRow = -1;
                        vizTable.repaint();
                        
                        if (currentQueryType.equals("SELECT")) {
                            vizCanvas.setStatus("MATCH FOUND: ROW " + (matchedRow + 1));
                            JOptionPane.showMessageDialog(null, "SELECT MATCH FOUND!\n\nTarget row located in storage.");
                        } else if (currentQueryType.equals("UPDATE")) {
                            String newVal = extractUpdateValue(query);
                            runUpdateSequence(newVal);
                        } else if (currentQueryType.equals("DELETE")) {
                            runDeleteSequence();
                        }
                    } else {
                        currentRow[0]++;
                    }
                } else {
                    timer.stop();
                    currentAnimatedRow = -1;
                    vizTable.repaint();
                    
                    if (currentQueryType.equals("SELECT") && (finalFilter == null || finalFilter.isEmpty())) {
                        vizCanvas.setStatus("FULL TABLE SCAN COMPLETE");
                        JOptionPane.showMessageDialog(null, "FULL SCAN COMPLETE\n\nNo specific filter was applied, so the entire table was traversed.");
                    } else if (matchCount[0] == 0) {
                        vizCanvas.setStatus("SCAN COMPLETE: NO MATCHES");
                        JOptionPane.showMessageDialog(null, "SCAN COMPLETE\n\nChecked " + currentRow[0] + " rows but found no match for: " + finalFilter);
                    }
                }
            }
        });
        timer.start();
    }

    private void runUpdateSequence(String newVal) {
        vizCanvas.setStatus("UPDATE: MODIFYING TO '" + newVal + "'...");
        Timer updateTimer = new Timer(1000, e -> {
            vizTable.setValueAt(newVal, matchedRow, 1);
            vizTable.repaint();
            vizCanvas.setStatus("UPDATE: COMMITTING '" + newVal + "'...");
            
            Timer flashTimer = new Timer(1000, ev -> {
                matchedRow = -1;
                vizTable.repaint();
                vizCanvas.setStatus("UPDATE COMPLETE");
                JOptionPane.showMessageDialog(null, "UPDATE SUCCESSFUL\n\nValue changed to '" + newVal + "' and committed.");
                ((Timer)ev.getSource()).stop();
            });
            flashTimer.setRepeats(false);
            flashTimer.start();
            
            ((Timer)e.getSource()).stop();
        });
        updateTimer.setRepeats(false);
        updateTimer.start();
    }

    private void runDeleteSequence() {
        vizCanvas.setStatus("DELETE: MARKING FOR REMOVAL...");
        Timer deleteTimer = new Timer(1000, e -> {
            vizCanvas.setStatus("DELETE: REORGANIZING STORAGE...");
            
            // FADE/REMOVAL EFFECT
            Timer removalTimer = new Timer(1000, ev -> {
                if (matchedRow != -1) {
                    vizTableModel.removeRow(matchedRow);
                    matchedRow = -1;
                    vizTable.repaint();
                    vizCanvas.setStatus("DELETE COMPLETE (ROWS SHIFTED)");
                    JOptionPane.showMessageDialog(null, "DELETE SUCCESSFUL\n\nRow removed and storage reorganized (Rows shifted up).");
                }
                ((Timer)ev.getSource()).stop();
            });
            removalTimer.setRepeats(false);
            removalTimer.start();
            
            ((Timer)e.getSource()).stop();
        });
        deleteTimer.setRepeats(false);
        deleteTimer.start();
    }

    private void runJoinAnimation(String query, String planText) {
        vizCanvas.setStatus("JOIN SIMULATION (NESTED LOOP)...");
        vizCanvas.repaint();
        
        Timer timer = new Timer(800, null);
        final int[] step = {0};

        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (step[0] < 5) {
                    vizCanvas.setJoinStep(step[0]);
                    vizCanvas.repaint();
                    step[0]++;
                } else {
                    timer.stop();
                    vizCanvas.setStatus("JOIN COMPLETE");
                }
            }
        });
        timer.start();
    }

    private void runSortAnimation(String query, String planText) {
        vizCanvas.setStatus("SORTING SIMULATION...");
        vizCanvas.repaint();
        
        Timer timer = new Timer(400, null);
        final int[] step = {0};
        final int rowCount = vizTable.getRowCount();

        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (step[0] < rowCount) {
                    currentAnimatedRow = step[0];
                    vizTable.repaint();
                    step[0]++;
                } else {
                    timer.stop();
                    currentAnimatedRow = -1;
                    vizTable.repaint();
                    vizCanvas.setStatus("SORT COMPLETE");
                }
            }
        });
        timer.start();
    }

    // =====================================================
    // ================= CANVAS CLASS ======================
    // =====================================================

    class VisualizationCanvas extends JPanel {
        String status = "IDLE";
        int joinStep = -1;
        String metadata = "";
        
        // INSERT ANIMATION PROPS
        String[] floatingRowData = null;
        int floatX = 50, floatY = 200;

        public void setStatus(String s) { this.status = s; }
        public void setJoinStep(int s) { this.joinStep = s; }
        public void setBTreeStep(int s) { /* Disabled */ }
        public void setMetadata(String m) { this.metadata = m; }
        public void setFloatingRowData(String[] d) { this.floatingRowData = d; }
        public void setFloatingRowPos(int x, int y) { this.floatX = x; this.floatY = y; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(Color.CYAN);
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString("DBMS ENGINE STATUS: " + status, 20, 30);

            // DRAW METADATA
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            if (!metadata.isEmpty()) {
                String[] lines = metadata.split("\n");
                int y = 60;
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    g2.drawString(">> " + line.trim(), 20, y);
                    y += 18;
                    if (y > getHeight() - 20) break;
                }
            }

            // DRAW FLOATING ROW (INSERT)
            if (floatingRowData != null) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(floatX, floatY, 300, 40);
                g2.setColor(new Color(255, 255, 255, 150));
                g2.fillRect(floatX, floatY, 300, 40);
                
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                String content = String.join(" | ", floatingRowData);
                g2.drawString("NEW ROW: [ " + content + " ]", floatX + 10, floatY + 25);
            }

            // JOIN VISUALIZATION
            if (joinStep >= 0) {
                int xOffset = 400;
                g2.setColor(Color.CYAN);
                g2.drawRect(xOffset + 50, 60, 100, 80);
                g2.drawString("TABLE A", xOffset + 70, 55);

                g2.setColor(Color.ORANGE);
                g2.drawRect(xOffset + 250, 60, 100, 80);
                g2.drawString("TABLE B", xOffset + 270, 55);

                if (joinStep > 0) {
                    g2.setColor(Color.YELLOW);
                    g2.drawLine(xOffset + 150, 100, xOffset + 250, 100);
                    g2.drawString("COMPARING...", xOffset + 170, 90);
                }
            }
        }
    }

    
    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        new SQLVisualizerUI();
    }
}