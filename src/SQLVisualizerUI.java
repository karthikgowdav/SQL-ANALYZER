import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class SQLVisualizerUI extends JFrame {

    // =====================================================
    // ================= THEME COLOR PALETTE ===============
    // =====================================================
    private static final Color BG_LIGHT = new Color(244, 246, 245);      // Soft greenish-gray background
    private static final Color CARD_WHITE = new Color(255, 255, 255);    // Pure white cards
    private static final Color SIDEBAR_DARK = new Color(9, 38, 30);      // Dark forest green sidebar background
    private static final Color TEXT_DARK = new Color(28, 46, 36);        // Very dark forest gray for titles
    private static final Color TEXT_MUTED = new Color(130, 148, 138);    // Medium greenish-gray for descriptions
    private static final Color BORDER_GRAY = new Color(175, 195, 185);    // Sharp defined borders
    
    // Forest Green & Teal Accent Colors
    private static final Color ACCENT_GREEN = new Color(19, 117, 71);    // Vibrant forest green
    private static final Color ACCENT_TEAL = new Color(0, 168, 150);     // Teal from database icon
    private static final Color ACCENT_BLUE = new Color(63, 140, 255);
    private static final Color ACCENT_RED = new Color(255, 94, 94);
    private static final Color ACCENT_ORANGE = new Color(255, 170, 90);
    private static final Color LIME_GREEN = new Color(162, 215, 41);     // Lime green active accent
    private static final Color SELECTION_BG = new Color(226, 244, 236);   // Soft green selection background

    // ================= COMPONENTS ========================
    JTextArea queryArea;
    JTable resultTable;
    DefaultTableModel tableModel;
    JTable vizTable;
    DefaultTableModel vizTableModel;
    JTextArea infoArea;
    ModernButton executeButton;
    ModernButton analyzeButton;
    JTextArea chatHistoryArea;
    JTextField userInputField;
    ModernButton sendButton;

    JPanel analyzePanel;
    JTextPane analysisReportPane;
    ModernButton autoOptimizeButton;

    boolean casingNeedsOptimization = false;
    boolean wildcardNeedsOptimization = false;
    java.util.List<String> missingIndexColumns = new java.util.ArrayList<>();
    String geminiApiKey = null;

    ModernButton visualizeButton;
    ModernButton clearButton;
    VisualizationCanvas vizCanvas;
    JTabbedPane tabbedPane;
    
    // Sidebar list
    java.util.List<SidebarItem> sidebarItemsList = new java.util.ArrayList<>();

    // ================= VISUALIZATION VARIABLES ===========
    int currentAnimatedRow = -1;
    int matchedRow = -1;
    int recentlyInsertedRow = -1;
    String currentQueryType = "SELECT";
    java.util.List<Vector<String>> lastDeletedRows = new java.util.ArrayList<>();

    // ================= DATABASE ==========================
    String url = "jdbc:postgresql://localhost:5434/sql_visualizer?options=-c%20TimeZone=UTC";
    String user = "postgres";
    String password = "postgres";

    // HISTORY LOG
    DefaultTableModel historyModel;
    JTable historyTable;

    // =====================================================
    // ================= CUSTOM COMPONENTS =================
    // =====================================================
    class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(15, 15, 15, 15));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Paint card background
            g2.setColor(CARD_WHITE);
            g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 16, 16);
            
            // Defined darker border
            g2.setColor(BORDER_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 16, 16);
            
            g2.dispose();
        }
    }

    class ModernButton extends JButton {
        private Color bgColor;
        private Color hoverColor;
        private boolean hovered = false;

        public ModernButton(String text) {
            this(text, ACCENT_GREEN);
        }

        public ModernButton(String text, Color baseColor) {
            super(text);
            this.bgColor = baseColor;
            this.hoverColor = baseColor.brighter();
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(hovered ? hoverColor : bgColor);
            g2.fillRoundRect(0, 0, w, h, 20, 20); // Pill rounded shape
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // Logo Icon matching database stack design in forest green/teal
    class LogoIcon extends JComponent {
        public LogoIcon() {
            setPreferredSize(new Dimension(28, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // Database stack base color: Teal
            g2.setColor(ACCENT_TEAL);
            
            // Top layer
            g2.fillOval(0, 2, w, h / 3 - 2);
            
            // Middle layer
            g2.fillRect(0, h / 6 + 2, w, h / 3 - 2);
            g2.fillOval(0, h / 3 + 2, w, h / 3 - 2);
            
            // Bottom layer
            g2.fillRect(0, h / 2 + 2, w, h / 3 - 2);
            g2.fillOval(0, h / 2 + h / 6 + 2, w, h / 3 - 2);
            
            // Transparent separators (matches sidebar background)
            g2.setColor(SIDEBAR_DARK);
            g2.fillRect(0, h / 3 + 1, w, 2);
            g2.fillRect(0, h / 3 * 2 + 1, w, 2);
            
            // Vertical cut-out stripe on the left
            g2.fillRect(w / 4, 0, w / 8, h);
            
            g2.dispose();
        }
    }

    class SidebarItem extends JPanel {
        private String label;
        private String iconType;
        private boolean active = false;
        private int tabIndex;
        private boolean hovered = false;

        public SidebarItem(String iconType, String label, int tabIndex) {
            this.iconType = iconType;
            this.label = label;
            this.tabIndex = tabIndex;
            
            // Set to non-opaque so parent panel paints its solid green background first,
            // preventing transparent text ghosting/overlapping artifacts.
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(12, 18, 12, 18));
            
            JLabel textLabel = new JLabel(label);
            textLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            textLabel.setForeground(new Color(230, 235, 230));
            textLabel.setBorder(new EmptyBorder(0, 30, 0, 0));
            
            add(textLabel, BorderLayout.CENTER);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (tabIndex >= 0) {
                        tabbedPane.setSelectedIndex(tabIndex);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    triggerParentRepaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    triggerParentRepaint();
                }
            });
        }
        
        public void setActive(boolean active) {
            this.active = active;
            triggerParentRepaint();
        }
        
        private void triggerParentRepaint() {
            if (getParent() != null) {
                getParent().repaint(); // Force sidebar container to clear screen
            } else {
                repaint();
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Paint hover / active selection background states
            if (active) {
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            
            // Active indicator lime-green bar on the left edge
            if (active) {
                g2.setColor(LIME_GREEN);
                g2.fillRoundRect(0, 6, 4, getHeight() - 12, 2, 2);
            }
            
            // Draw clean Vector Icons
            int ix = 22;
            int iy = (getHeight() - 18) / 2;
            g2.setColor(active ? LIME_GREEN : new Color(200, 205, 200));
            g2.setStroke(new BasicStroke(2));
            
            if (iconType.equals("results")) {
                // Overview / Grid icon
                g2.fillRoundRect(ix, iy, 7, 7, 2, 2);
                g2.fillRoundRect(ix + 9, iy, 7, 7, 2, 2);
                g2.fillRoundRect(ix, iy + 9, 7, 7, 2, 2);
                g2.fillRoundRect(ix + 9, iy + 9, 7, 7, 2, 2);
            } else if (iconType.equals("info")) {
                // Info Circle icon
                g2.drawOval(ix, iy, 16, 16);
                g2.fillRect(ix + 7, iy + 4, 2, 2);
                g2.fillRect(ix + 7, iy + 7, 2, 5);
            } else if (iconType.equals("viz")) {
                // Eye/Visualization icon
                g2.drawArc(ix, iy + 1, 16, 12, 0, 180);
                g2.drawArc(ix, iy - 3, 16, 12, 180, 180);
                g2.fillOval(ix + 5, iy + 5, 6, 6);
            } else if (iconType.equals("history")) {
                // Clock icon
                g2.drawOval(ix, iy, 16, 16);
                g2.drawLine(ix + 8, iy + 8, ix + 8, iy + 4);
                g2.drawLine(ix + 8, iy + 8, ix + 12, iy + 8);
            } else if (iconType.equals("analyze")) {
                // Thunder icon
                int[] px = {ix + 10, ix + 3, ix + 9, ix + 6, ix + 13, ix + 7};
                int[] py = {iy, iy + 9, iy + 9, iy + 16, iy + 7, iy + 7};
                g2.fillPolygon(px, py, 6);
            } else if (iconType.equals("settings")) {
                // Gear icon
                g2.drawOval(ix + 3, iy + 3, 10, 10);
                for (int a = 0; a < 360; a += 60) {
                    double rad = Math.toRadians(a);
                    int x1 = (int) (ix + 8 + 5 * Math.cos(rad));
                    int y1 = (int) (iy + 8 + 5 * Math.sin(rad));
                    int x2 = (int) (ix + 8 + 8 * Math.cos(rad));
                    int y2 = (int) (iy + 8 + 8 * Math.sin(rad));
                    g2.drawLine(x1, y1, x2, y2);
                }
            } else if (iconType.equals("security")) {
                // Shield icon
                int[] sx = {ix + 8, ix + 15, ix + 15, ix + 8, ix + 1, ix + 1};
                int[] sy = {iy, iy + 3, iy + 9, iy + 16, iy + 9, iy + 3};
                g2.fillPolygon(sx, sy, 6);
            }
            
            g2.dispose();
        }
    }

    private void styleTable(JTable table) {
        table.setBackground(CARD_WHITE);
        table.setForeground(TEXT_DARK);
        table.setGridColor(BORDER_GRAY);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(TEXT_DARK);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_LIGHT);
        header.setForeground(TEXT_DARK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_GRAY));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                comp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (!s) {
                    if (r % 2 == 0) {
                        comp.setBackground(CARD_WHITE);
                    } else {
                        comp.setBackground(new Color(242, 245, 243));
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return comp;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane pane) {
        pane.getViewport().setBackground(CARD_WHITE);
        pane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(175, 195, 185));
                g2.fillRoundRect(r.x + 4, r.y + 2, r.width - 8, r.height - 4, 6, 6);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(BG_LIGHT);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        pane.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1));
    }

    private void updateSidebarHighlight(int tabIndex) {
        for (int i = 0; i < sidebarItemsList.size(); i++) {
            sidebarItemsList.get(i).setActive(sidebarItemsList.get(i).tabIndex == tabIndex);
        }
    }

    // =====================================================
    // ================= CONSTRUCTOR =======================
    // =====================================================
    public SQLVisualizerUI() {
        setTitle("TraceQL Dashboard Overview");
        setSize(1480, 880);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_LIGHT);
        setLayout(new BorderLayout());

        // =====================================================
        // ================= SIDEBAR PANEL =====================
        // =====================================================
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SIDEBAR_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(30, 0, 30, 0));

        // Sidebar Logo/Branding matching the Siohioma image layout
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        logoPanel.setOpaque(false);
        
        LogoIcon dbLogo = new LogoIcon();
        
        JLabel appTitleLabel = new JLabel("TraceQL");
        appTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appTitleLabel.setForeground(Color.WHITE);
        
        logoPanel.add(dbLogo);
        logoPanel.add(appTitleLabel);
        sidebar.add(logoPanel);
        
        sidebar.add(Box.createRigidArea(new Dimension(0, 35)));

        // MENU Title Label
        JPanel menuTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        menuTitlePanel.setOpaque(false);
        JLabel menuTitleLabel = new JLabel("MENU");
        menuTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuTitleLabel.setForeground(new Color(255, 255, 255, 100));
        menuTitlePanel.add(menuTitleLabel);
        sidebar.add(menuTitlePanel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        // Sidebar Items (Synchronized with Tab indices using Vector icons)
        SidebarItem itemResults = new SidebarItem("results", "Results", 0);
        SidebarItem itemInfo = new SidebarItem("info", "Information", 1);
        SidebarItem itemViz = new SidebarItem("viz", "Visualization", 2);
        SidebarItem itemHistory = new SidebarItem("history", "History", 3);
        SidebarItem itemAnalyze = new SidebarItem("analyze", "Analyze", 4);
        
        sidebarItemsList.add(itemResults);
        sidebarItemsList.add(itemInfo);
        sidebarItemsList.add(itemViz);
        sidebarItemsList.add(itemHistory);
        sidebarItemsList.add(itemAnalyze);
        
        for (SidebarItem item : sidebarItemsList) {
            sidebar.add(item);
        }
        
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // GENERAL Section Label
        JPanel generalTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        generalTitlePanel.setOpaque(false);
        JLabel generalTitleLabel = new JLabel("GENERAL");
        generalTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        generalTitleLabel.setForeground(new Color(255, 255, 255, 100));
        generalTitlePanel.add(generalTitleLabel);
        sidebar.add(generalTitlePanel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        SidebarItem itemSettings = new SidebarItem("settings", "Settings", -1);
        SidebarItem itemSecurity = new SidebarItem("security", "Security", -1);
        sidebar.add(itemSettings);
        sidebar.add(itemSecurity);
        
        add(sidebar, BorderLayout.WEST);

        // Default highlight Result tab
        updateSidebarHighlight(0);

        // =====================================================
        // ================= MAIN CONTAINER ====================
        // =====================================================
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(BG_LIGHT);
        mainContent.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(mainContent, BorderLayout.CENTER);

        // Header Title Section
        JPanel headerSection = new JPanel(new BorderLayout());
        headerSection.setOpaque(false);
        JLabel titleLabel = new JLabel("TraceQL Overview");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_DARK);
        
        JLabel subtitleLabel = new JLabel("Visual TraceQL query performance and storage analysis engine");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_MUTED);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        headerSection.add(titlePanel, BorderLayout.WEST);

        // Date Display
        JLabel dateLabel = new JLabel("Live Database Connected");
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dateLabel.setForeground(ACCENT_GREEN);
        dateLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN, 2, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        headerSection.add(dateLabel, BorderLayout.EAST);
        mainContent.add(headerSection, BorderLayout.NORTH);

        // SPLIT PANEL: Left (Editor), Right (Tabs / Visualizer)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setBorder(null);
        mainSplit.setDividerLocation(520);
        mainSplit.setResizeWeight(0.35);
        mainSplit.setBackground(BG_LIGHT);
        mainContent.add(mainSplit, BorderLayout.CENTER);

        // =====================================================
        // ================= LEFT EDITOR CARD ==================
        // =====================================================
        CardPanel leftCard = new CardPanel();
        leftCard.setLayout(new BorderLayout(10, 10));

        JLabel editorTitle = new JLabel("SQL QUERY EDITOR");
        editorTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        editorTitle.setForeground(TEXT_DARK);
        leftCard.add(editorTitle, BorderLayout.NORTH);

        queryArea = new JTextArea();
        queryArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        queryArea.setText("SELECT * FROM students WHERE id = 3;");
        queryArea.setBackground(new Color(250, 250, 252));
        queryArea.setForeground(TEXT_DARK);
        queryArea.setCaretColor(ACCENT_GREEN);
        queryArea.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane queryScroll = new JScrollPane(queryArea);
        styleScrollPane(queryScroll);
        queryScroll.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1));
        leftCard.add(queryScroll, BorderLayout.CENTER);

        JPanel executePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        executePanel.setOpaque(false);
        executeButton = new ModernButton("EXECUTE QUERY", ACCENT_GREEN);
        executeButton.setPreferredSize(new Dimension(200, 42));
        executePanel.add(executeButton);
        leftCard.add(executePanel, BorderLayout.SOUTH);

        mainSplit.setLeftComponent(leftCard);

        // =====================================================
        // ================= RIGHT TABS CARD ===================
        // =====================================================
        CardPanel rightCard = new CardPanel();
        rightCard.setLayout(new BorderLayout(10, 10));

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(CARD_WHITE);
        tabbedPane.setForeground(TEXT_DARK);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Sync sidebar highlight on tab switch
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateSidebarHighlight(tabbedPane.getSelectedIndex());
            }
        });

        // RESULTS
        JScrollPane tableScroll = new JScrollPane();
        styleScrollPane(tableScroll);
        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        styleTable(resultTable);
        tableScroll.setViewportView(resultTable);

        // INFORMATION
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(250, 250, 252));
        infoArea.setForeground(TEXT_DARK);
        infoArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        infoArea.setMargin(new Insets(12, 12, 12, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        styleScrollPane(infoScroll);

        tabbedPane.addTab("RESULTS", tableScroll);
        tabbedPane.addTab("INFORMATION", infoScroll);

        // VISUALIZATION
        JPanel vizPanel = new JPanel(new BorderLayout(10, 10));
        vizPanel.setBackground(CARD_WHITE);

        vizTableModel = new DefaultTableModel();
        vizTable = new JTable(vizTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    if (row == currentAnimatedRow) {
                        c.setBackground(ACCENT_ORANGE);
                        c.setForeground(Color.WHITE);
                    } else if (row == matchedRow) {
                        if (currentQueryType.equals("SELECT")) {
                            c.setBackground(ACCENT_BLUE);
                        } else if (currentQueryType.equals("UPDATE")) {
                            c.setBackground(ACCENT_GREEN);
                        } else if (currentQueryType.equals("DELETE")) {
                            c.setBackground(ACCENT_RED);
                        } else {
                            c.setBackground(SIDEBAR_DARK);
                        }
                        c.setForeground(Color.WHITE);
                    } else if (row == recentlyInsertedRow) {
                        c.setBackground(ACCENT_GREEN);
                        c.setForeground(Color.WHITE);
                    } else {
                        if (row % 2 == 0) {
                            c.setBackground(CARD_WHITE);
                        } else {
                            c.setBackground(new Color(248, 249, 253));
                        }
                        c.setForeground(TEXT_DARK);
                    }
                }
                return c;
            }
        };
        styleTable(vizTable);

        JScrollPane vizScroll = new JScrollPane(vizTable);
        styleScrollPane(vizScroll);
        vizScroll.setPreferredSize(new Dimension(800, 200));
        vizPanel.add(vizScroll, BorderLayout.CENTER);

        vizCanvas = new VisualizationCanvas();
        vizCanvas.setPreferredSize(new Dimension(800, 230));
        vizPanel.add(vizCanvas, BorderLayout.SOUTH);

        tabbedPane.addTab("VISUALIZATION", vizPanel);

        // HISTORY
        historyModel = new DefaultTableModel(new String[]{"ID", "SQL COMMAND", "STATUS", "TIME"}, 0);
        historyTable = new JTable(historyModel);
        styleTable(historyTable);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        styleScrollPane(historyScroll);
        tabbedPane.addTab("HISTORY", historyScroll);

        // ANALYZE
        analyzePanel = new JPanel(new BorderLayout(10, 10));
        analyzePanel.setBackground(CARD_WHITE);

        JLabel analyzeHeader = new JLabel("SQL QUERY PERFORMANCE ANALYZER", JLabel.CENTER);
        analyzeHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        analyzeHeader.setOpaque(true);
        analyzeHeader.setBackground(BG_LIGHT);
        analyzeHeader.setForeground(TEXT_DARK);
        analyzeHeader.setPreferredSize(new Dimension(800, 40));
        analyzePanel.add(analyzeHeader, BorderLayout.NORTH);

        analysisReportPane = new JTextPane();
        analysisReportPane.setContentType("text/html");
        analysisReportPane.setEditable(false);
        analysisReportPane.setBackground(CARD_WHITE);
        analysisReportPane.setText("<html><body style='font-family:sans-serif; padding:15px; color:#2D3142; background-color:#FFFFFF;'>"
                + "<h2>No query analyzed yet.</h2>"
                + "<p>Enter an SQL query and click the <b>ANALYZE</b> button to review optimization status.</p>"
                + "</body></html>");
        JScrollPane analyzeScroll = new JScrollPane(analysisReportPane);
        styleScrollPane(analyzeScroll);

        // Chat panel
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(CARD_WHITE);
        chatPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1),
                "SQL ASSISTANT CHAT",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 11),
                TEXT_MUTED
        ));
        chatPanel.setVisible(false);

        chatHistoryArea = new JTextArea();
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setBackground(new Color(250, 250, 252));
        chatHistoryArea.setForeground(TEXT_DARK);
        chatHistoryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatHistoryArea.setLineWrap(true);
        chatHistoryArea.setWrapStyleWord(true);
        chatHistoryArea.setMargin(new Insets(8, 8, 8, 8));
        chatHistoryArea.append("System: Hello! Ask me questions about SQL optimization, indexes, or scans.\n\n");
        JScrollPane chatHistoryScroll = new JScrollPane(chatHistoryArea);
        styleScrollPane(chatHistoryScroll);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(CARD_WHITE);
        userInputField = new JTextField();
        userInputField.setBackground(new Color(250, 250, 252));
        userInputField.setForeground(TEXT_DARK);
        userInputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        sendButton = new ModernButton("SEND", ACCENT_GREEN);
        sendButton.setPreferredSize(new Dimension(80, 30));

        inputPanel.add(userInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(chatHistoryScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, analyzeScroll, chatPanel);
        splitPane.setDividerLocation(380);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);
        splitPane.setBackground(CARD_WHITE);
        analyzePanel.add(splitPane, BorderLayout.CENTER);

        JPanel analyzeControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        analyzeControlPanel.setBackground(CARD_WHITE);
        autoOptimizeButton = new ModernButton("AUTO-OPTIMIZE QUERY", ACCENT_BLUE);
        autoOptimizeButton.setPreferredSize(new Dimension(200, 40));
        autoOptimizeButton.setEnabled(false);
        analyzeControlPanel.add(autoOptimizeButton);

        JButton chatToggleButton = new ModernButton("CHAT", ACCENT_GREEN);
        chatToggleButton.setPreferredSize(new Dimension(120, 40));
        chatToggleButton.addActionListener(e -> {
            chatPanel.setVisible(true);
            splitPane.setDividerLocation(380);
            userInputField.requestFocusInWindow();
        });
        analyzeControlPanel.add(chatToggleButton);
        analyzePanel.add(analyzeControlPanel, BorderLayout.SOUTH);

        autoOptimizeButton.addActionListener(e -> autoOptimizeQuery());

        ActionListener sendMessageAction = e -> {
            String message = userInputField.getText().trim();
            if (message.isEmpty()) return;

            chatHistoryArea.append("You: " + message + "\n");
            userInputField.setText("");

            if (geminiApiKey == null || geminiApiKey.isEmpty()) {
                geminiApiKey = System.getenv("GEMINI_API_KEY");
            }

            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                chatHistoryArea.append("Assistant: Thinking...\n");
                chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());

                new Thread(() -> {
                    try {
                        String reply = callGeminiAPI(message, "", "");
                        SwingUtilities.invokeLater(() -> {
                            String currentText = chatHistoryArea.getText();
                            int thinkingIdx = currentText.lastIndexOf("Assistant: Thinking...");
                            if (thinkingIdx != -1) {
                                chatHistoryArea.setText(currentText.substring(0, thinkingIdx));
                            }
                            chatHistoryArea.append("Assistant: " + reply + "\n\n");
                            chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            chatHistoryArea.append("System Error: " + ex.getMessage() + "\n\n");
                        });
                    }
                }).start();
            } else {
                String reply = getAssistantReply(message);
                chatHistoryArea.append("Assistant (Local Mode): " + reply + "\n\n");
                chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());
            }
        };

        sendButton.addActionListener(sendMessageAction);
        userInputField.addActionListener(sendMessageAction);

        tabbedPane.addTab("ANALYZE", analyzePanel);
        rightCard.add(tabbedPane, BorderLayout.CENTER);

        // Control Panel
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomButtonPanel.setBackground(CARD_WHITE);
        bottomButtonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GRAY));

        analyzeButton = new ModernButton("ANALYZE PERFORMANCE", ACCENT_BLUE);
        visualizeButton = new ModernButton("VISUALIZE QUERY", ACCENT_GREEN);
        clearButton = new ModernButton("CLEAR ALL", ACCENT_ORANGE);

        analyzeButton.setPreferredSize(new Dimension(200, 42));
        visualizeButton.setPreferredSize(new Dimension(200, 42));
        clearButton.setPreferredSize(new Dimension(140, 42));

        bottomButtonPanel.add(analyzeButton);
        bottomButtonPanel.add(visualizeButton);
        bottomButtonPanel.add(clearButton);

        rightCard.add(bottomButtonPanel, BorderLayout.SOUTH);
        mainSplit.setRightComponent(rightCard);

        // Actions
        analyzeButton.addActionListener(e -> {
            runQueryAnalysis();
            userInputField.requestFocusInWindow();
        });

        executeButton.addActionListener(e -> executeSQLQuery());
        visualizeButton.addActionListener(e -> startVisualization());

        clearButton.addActionListener(e -> {
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
        });

        setVisible(true);
    }

    private String getAnalysisReportText() {
        String text = analysisReportPane.getText();
        return text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (url.contains(":5434/")) {
                String fallbackUrl = url.replace(":5434/", ":5432/");
                try {
                    Connection conn = DriverManager.getConnection(fallbackUrl, user, password);
                    url = fallbackUrl;
                    return conn;
                } catch (SQLException ex) {
                    throw e;
                }
            }
            throw e;
        }
    }

    // =====================================================
    // ================= EXECUTE SQL METHOD ================
    // =====================================================
    private void executeSQLQuery() {
        String query = queryArea.getText().trim();
        infoArea.setText("");
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        lastDeletedRows.clear();

        try {
            if (query.endsWith(";")) {
                query = query.substring(0, query.length() - 1);
            }

            long startTime = System.currentTimeMillis();
            Connection conn = getConnection();
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
                if (lowerQuery.startsWith("delete")) {
                    try {
                        String selectQuery = query.replaceAll("(?i)^\\s*delete\\b", "SELECT *");
                        try (Statement selectStmt = conn.createStatement()) {
                            ResultSet rs = selectStmt.executeQuery(selectQuery);
                            ResultSetMetaData md = rs.getMetaData();
                            int colCount = md.getColumnCount();
                            while (rs.next()) {
                                Vector<String> row = new Vector<>();
                                for (int i = 1; i <= colCount; i++) {
                                    row.add(rs.getString(i));
                                }
                                lastDeletedRows.add(row);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
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
    // ================= SQL QUERY ANALYSIS ================
    // =====================================================
    private void runQueryAnalysis() {
        String query = queryArea.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SQL query first.");
            return;
        }

        tabbedPane.setSelectedComponent(analyzePanel);

        casingNeedsOptimization = false;
        wildcardNeedsOptimization = false;
        missingIndexColumns.clear();

        String[] keywords = {"select", "from", "where", "join", "on", "group by", "order by", "and", "or", "insert into", "values", "update", "set", "delete from"};
        String lowerQuery = query.toLowerCase();
        for (String kw : keywords) {
            String regex = "\\b" + kw.replace(" ", "\\s+") + "\\b";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher m = p.matcher(lowerQuery);
            if (m.find()) {
                int start = m.start();
                int end = m.end();
                String originalMatch = query.substring(start, end);
                if (!originalMatch.equals(originalMatch.toUpperCase())) {
                    casingNeedsOptimization = true;
                    break;
                }
            }
        }

        if (lowerQuery.matches("(?i).*select\\s+\\*\\s+from.*")) {
            wildcardNeedsOptimization = true;
        }

        StringBuilder explainPlan = new StringBuilder();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("EXPLAIN " + query);
            while (rs.next()) {
                explainPlan.append(rs.getString(1)).append("\n");
            }
        } catch (Exception ex) {
            explainPlan.append("Error fetching explain plan: ").append(ex.getMessage());
        }
        String planText = explainPlan.toString();

        if (lowerQuery.contains("students")) {
            String[] checkCols = {"name", "marks", "department", "cgpa"};
            for (String col : checkCols) {
                if (lowerQuery.matches("(?i).*\\b" + col + "\\b.*")) {
                    boolean hasIndex = false;
                    try (Connection conn = getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT indexname FROM pg_indexes WHERE tablename = 'students'")) {
                        while (rs.next()) {
                            String indexName = rs.getString(1);
                            if (indexName.contains(col)) {
                                hasIndex = true;
                                break;
                            }
                        }
                    } catch (Exception ex) {}

                    if (!hasIndex && planText.contains("Seq Scan")) {
                        missingIndexColumns.add(col);
                    }
                }
            }
        }

        // Build premium styled Light HTML Report
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; padding:15px; color:#2D3142; background-color:#FFFFFF;'>");
        html.append("<h2 style='color:#137547; border-bottom: 2px solid #E8E9F3; padding-bottom:5px;'>SQL QUERY ANALYSIS REPORT</h2>");

        html.append("<div style='background-color:#F8F9FD; color:#2D3142; padding:12px; border-radius:8px; font-family:monospace; margin-bottom:15px; border:1px solid #E8E9F3;'>");
        html.append("<strong>Analyzing Query:</strong><br/><code>").append(query.replace("<", "&lt;").replace(">", "&gt;")).append("</code>");
        html.append("</div>");

        html.append("<table width='100%' cellpadding='8' cellspacing='0' style='border:1px solid #E8E9F3; background-color:#FFFFFF; margin-bottom:15px; border-radius:8px; overflow:hidden;'>");

        // Casing
        html.append("<tr style='border-bottom:1px solid #E8E9F3;'>");
        if (casingNeedsOptimization) {
            html.append("<td width='180' valign='top'><span style='background-color:#FFF3E0; color:#E65100; padding:4px 8px; font-weight:bold; border-radius:4px;'>NEEDS OPTIMIZATION</span></td>");
            html.append("<td valign='top'><b>Keyword Casing:</b> Lowercase SQL keywords detected. Convert them to UPPERCASE for standard styling.</td>");
        } else {
            html.append("<td width='180' valign='top'><span style='background-color:#E8F5E9; color:#2E7D32; padding:4px 8px; font-weight:bold; border-radius:4px;'>OPTIMAL</span></td>");
            html.append("<td valign='top'><b>Keyword Casing:</b> Keywords are standard UPPERCASE.</td>");
        }
        html.append("</tr>");

        // Wildcard
        html.append("<tr style='border-bottom:1px solid #E8E9F3;'>");
        if (wildcardNeedsOptimization) {
            html.append("<td valign='top'><span style='background-color:#FFF3E0; color:#E65100; padding:4px 8px; font-weight:bold; border-radius:4px;'>NEEDS OPTIMIZATION</span></td>");
            html.append("<td valign='top'><b>Wildcard '*' Usage:</b> <code>SELECT *</code> detected. Explicit columns improve performance and reduce overhead.</td>");
        } else {
            html.append("<td valign='top'><span style='background-color:#E8F5E9; color:#2E7D32; padding:4px 8px; font-weight:bold; border-radius:4px;'>OPTIMAL</span></td>");
            html.append("<td valign='top'><b>Wildcard '*' Usage:</b> Explicit column names specified.</td>");
        }
        html.append("</tr>");

        // Indexing
        html.append("<tr>");
        if (!missingIndexColumns.isEmpty()) {
            html.append("<td valign='top'><span style='background-color:#FFEBEE; color:#C62828; padding:4px 8px; font-weight:bold; border-radius:4px;'>NEEDS OPTIMIZATION</span></td>");
            html.append("<td valign='top'><b>Sequential Scan:</b> Missing index detected for: <code>" + String.join(", ", missingIndexColumns) + "</code>. Create a B-Tree index to perform index lookups instead.</td>");
        } else {
            html.append("<td valign='top'><span style='background-color:#E8F5E9; color:#2E7D32; padding:4px 8px; font-weight:bold; border-radius:4px;'>OPTIMAL</span></td>");
            if (planText.contains("Index Scan") || planText.contains("Index Only Scan")) {
                html.append("<td valign='top'><b>Table Scanning:</b> Index scan utilized. Good performance.</td>");
            } else {
                html.append("<td valign='top'><b>Table Scanning:</b> No missing indexes or sequential scan filters detected.</td>");
            }
        }
        html.append("</tr>");
        html.append("</table>");

        html.append("<h3 style='color:#137547;'>POSTGRESQL EXPLAIN PLAN</h3>");
        html.append("<div style='background-color:#F8F9FD; color:#8C90A6; padding:12px; border-radius:8px; font-family:monospace; font-size:12px; white-space:pre; border:1px solid #E8E9F3;'>");
        html.append(planText.replace("<", "&lt;").replace(">", "&gt;"));
        html.append("</div>");

        boolean hasIssues = casingNeedsOptimization || wildcardNeedsOptimization || !missingIndexColumns.isEmpty();
        if (hasIssues) {
            html.append("<div style='background-color:#FFF8E1; border-left:4px solid #FFAA5A; padding:10px; margin-top:15px; color:#E65100; border-radius:4px;'>");
            html.append("<strong>Suggestions Available:</strong> Click the <b>AUTO-OPTIMIZE QUERY</b> button below to automatically apply optimizations.");
            html.append("</div>");
        } else {
            html.append("<div style='background-color:#E8F5E9; border-left:4px solid #2ED47A; padding:10px; margin-top:15px; color:#2E7D32; border-radius:4px;'>");
            html.append("<strong>Fully Optimized:</strong> Your query is running under optimal conditions.");
            html.append("</div>");
        }

        html.append("</body></html>");
        analysisReportPane.setText(html.toString());
        analysisReportPane.setCaretPosition(0);

        autoOptimizeButton.setEnabled(hasIssues);
    }

    private void autoOptimizeQuery() {
        String query = queryArea.getText().trim();
        if (query.isEmpty()) return;

        java.util.List<String> actionsApplied = new java.util.ArrayList<>();

        if (casingNeedsOptimization) {
            query = uppercaseKeywords(query);
            actionsApplied.add("Uppercased SQL keywords");
        }

        if (wildcardNeedsOptimization) {
            query = replaceWildcard(query);
            actionsApplied.add("Replaced '*' wildcard with explicit column names");
        }

        if (!missingIndexColumns.isEmpty()) {
            for (String col : missingIndexColumns) {
                String indexName = "idx_students_" + col;
                String ddl = "CREATE INDEX IF NOT EXISTS " + indexName + " ON students(" + col + ");";
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(ddl);
                    actionsApplied.add("Created B-Tree database index: " + indexName);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to create index: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (!actionsApplied.isEmpty()) {
            queryArea.setText(query);
            runQueryAnalysis();

            StringBuilder msg = new StringBuilder("The query has been optimized:\n\n");
            for (String action : actionsApplied) {
                msg.append("✔ ").append(action).append("\n");
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Optimization Applied", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String uppercaseKeywords(String query) {
        String[] keywords = {"select", "from", "where", "join", "on", "group by", "order by", "and", "or", "insert into", "values", "update", "set", "delete from"};
        String[] parts = query.split("'", -1);
        for (int i = 0; i < parts.length; i += 2) {
            for (String kw : keywords) {
                String regex = "(?i)\\b" + kw.replace(" ", "\\s+") + "\\b";
                parts[i] = parts[i].replaceAll(regex, kw.toUpperCase());
            }
        }
        return String.join("'", parts);
    }

    private String replaceWildcard(String query) {
        String[] parts = query.split("'", -1);
        for (int i = 0; i < parts.length; i += 2) {
            parts[i] = parts[i].replaceAll("(?i)\\bselect\\s+\\*\\s+from\\b", "SELECT id, name, marks, department, cgpa FROM");
        }
        return String.join("'", parts);
    }

    // =====================================================
    // ================= VISUALIZATION =====================
    // =====================================================
    private void startVisualization() {
        try {
            tabbedPane.setSelectedIndex(2);
            String planText = infoArea.getText();
            String query = queryArea.getText().trim();
            String lowerQuery = query.toLowerCase();

            resetVisualizationState();
            vizCanvas.setMetadata(planText);

            if (lowerQuery.startsWith("insert")) {
                runInsertAnimation(query);
            } else if (lowerQuery.startsWith("update") || lowerQuery.startsWith("delete") || lowerQuery.startsWith("select")) {
                runUniversalScanAnimation(query, planText);
            } else if (planText.toLowerCase().contains("join")) {
                runJoinAnimation(query, planText);
            } else if (planText.toLowerCase().contains("sort")) {
                runSortAnimation(query, planText);
            } else {
                runUniversalScanAnimation(query, planText);
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
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.startsWith("insert")) {
            int intoIndex = lowerQuery.indexOf("into");
            if (intoIndex != -1) {
                String afterInto = query.substring(intoIndex + 4).trim();
                String firstToken = afterInto.split("[\\s\\(]+")[0];
                return firstToken.replace(";", "").replace("`", "").replace("\"", "");
            }
        }
        if (lowerQuery.startsWith("update")) {
            String afterUpdate = query.substring(6).trim();
            String firstToken = afterUpdate.split("[\\s\\(]+")[0];
            return firstToken.replace(";", "").replace("`", "").replace("\"", "");
        }
        if (lowerQuery.startsWith("delete")) {
            int fromIndex = lowerQuery.indexOf("from");
            if (fromIndex != -1) {
                String afterFrom = query.substring(fromIndex + 4).trim();
                String firstToken = afterFrom.split("[\\s\\(]+")[0];
                return firstToken.replace(";", "").replace("`", "").replace("\"", "");
            }
        }
        int fromIndex = lowerQuery.indexOf("from");
        if (fromIndex != -1) {
            String afterFrom = query.substring(fromIndex + 4).trim();
            String firstToken = afterFrom.split("[\\s\\(]+")[0];
            return firstToken.replace(";", "").replace("`", "").replace("\"", "");
        }
        return null;
    }

    private void fetchFullTable(String tableName) {
        try (Connection conn = getConnection()) {
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
            String setClause;
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
        if (filter == null || filter.trim().isEmpty()) return false;

        try {
            String rawFilter = filter.trim();
            rawFilter = rawFilter.replaceAll("(?i)[a-zA-Z0-9_]+\\.", "");
            rawFilter = rawFilter.replaceAll("(?i)::[a-z]+", "");

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

    // =====================================================
    // ================= RUN INSERT ANIMATION =============
    // =====================================================
    private void runInsertAnimation(String query) {
        String tableName = extractTableName(query);
        if (tableName != null) fetchFullTable(tableName);

        try {
            String lowerQuery = query.toLowerCase();
            int intoIdx = lowerQuery.indexOf("into");
            int valuesIdx = lowerQuery.indexOf("values");

            String intoPart = query.substring(intoIdx + 4, valuesIdx).trim();
            java.util.List<String> insertCols = new java.util.ArrayList<>();
            if (intoPart.contains("(") && intoPart.contains(")")) {
                String colListStr = intoPart.substring(intoPart.indexOf("(") + 1, intoPart.lastIndexOf(")")).trim();
                for (String c : colListStr.split(",")) {
                    insertCols.add(c.trim().toLowerCase());
                }
            }

            String valuesPart = query.substring(valuesIdx + 6).trim();
            if (valuesPart.endsWith(";")) valuesPart = valuesPart.substring(0, valuesPart.length() - 1).trim();
            if (valuesPart.startsWith("(")) valuesPart = valuesPart.substring(1).trim();
            if (valuesPart.endsWith(")")) valuesPart = valuesPart.substring(0, valuesPart.length() - 1).trim();

            String[] parts = valuesPart.split(",");
            final String[] rowData = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                rowData[i] = parts[i].trim().replace("'", "").replace("\"", "");
            }

            int matchRowIdx = -1;
            for (int r = vizTableModel.getRowCount() - 1; r >= 0; r--) {
                boolean match = true;
                if (!insertCols.isEmpty()) {
                    for (int i = 0; i < insertCols.size() && i < rowData.length; i++) {
                        String colName = insertCols.get(i);
                        int tableColIdx = -1;
                        for (int c = 0; c < vizTableModel.getColumnCount(); c++) {
                            if (vizTableModel.getColumnName(c).toLowerCase().equals(colName)) {
                                tableColIdx = c;
                                break;
                            }
                        }
                        if (tableColIdx != -1) {
                            String cellVal = String.valueOf(vizTableModel.getValueAt(r, tableColIdx)).trim();
                            if (!cellVal.equalsIgnoreCase(rowData[i])) {
                                match = false;
                                break;
                            }
                        }
                    }
                } else {
                    int startCol = 0;
                    if (rowData.length < vizTableModel.getColumnCount()) {
                        startCol = vizTableModel.getColumnCount() - rowData.length;
                    }
                    for (int c = 0; c < rowData.length && (startCol + c) < vizTableModel.getColumnCount(); c++) {
                        String cellVal = String.valueOf(vizTableModel.getValueAt(r, startCol + c)).trim();
                        if (!cellVal.equalsIgnoreCase(rowData[c])) {
                            match = false;
                            break;
                        }
                    }
                }
                if (match) {
                    matchRowIdx = r;
                    break;
                }
            }

            final Vector<Object> fullRowDataVector;
            if (matchRowIdx != -1) {
                Vector<?> rawRow = (Vector<?>) vizTableModel.getDataVector().elementAt(matchRowIdx);
                fullRowDataVector = new Vector<>(rawRow);
                vizTableModel.removeRow(matchRowIdx);
            } else {
                if (vizTableModel.getRowCount() > 0) {
                    Vector<?> rawRow = (Vector<?>) vizTableModel.getDataVector().elementAt(vizTableModel.getRowCount() - 1);
                    fullRowDataVector = new Vector<>(rawRow);
                    vizTableModel.removeRow(vizTableModel.getRowCount() - 1);
                } else {
                    fullRowDataVector = new Vector<>();
                    for (String val : rowData) fullRowDataVector.add(val);
                }
            }

            String[] printableFloatingRow = new String[fullRowDataVector.size()];
            for (int i = 0; i < fullRowDataVector.size(); i++) {
                printableFloatingRow[i] = String.valueOf(fullRowDataVector.get(i));
            }

            vizCanvas.setFloatingRowData(printableFloatingRow);
            vizCanvas.setStatus("INSERT: LOCATING STORAGE POSITION...");

            final int[] stage = {0};
            final int[] x = {50};
            final int[] y = {180};

            Timer insertTimer = new Timer(50, null);
            insertTimer.addActionListener(e -> {
                if (stage[0] == 0) {
                    vizCanvas.setStatus("INSERT: ALLOCATING DISK BLOCKS...");
                    if (++stage[0] > 10) stage[0] = 1;
                } else if (stage[0] == 1) {
                    y[0] -= 5;
                    vizCanvas.setFloatingRowPos(x[0], y[0]);
                    if (y[0] <= 40) stage[0] = 2;
                } else if (stage[0] == 2) {
                    insertTimer.stop();
                    vizCanvas.setFloatingRowData(null);
                    vizTableModel.addRow(fullRowDataVector);
                    recentlyInsertedRow = vizTableModel.getRowCount() - 1;
                    vizTable.repaint();
                    vizCanvas.setStatus("INSERT: WRITING TRANSACTION LOG...");

                    Timer flashTimer = new Timer(1500, ev -> {
                        recentlyInsertedRow = -1;
                        vizTable.repaint();
                        vizCanvas.setStatus("INSERT COMPLETE");
                        JOptionPane.showMessageDialog(null, "INSERT SUCCESSFUL\n\nDBMS has safely committed the record.");
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
            ex.printStackTrace();
        }
    }

    private void runUniversalScanAnimation(String query, String planText) {
        String tableName = extractTableName(query);
        if (tableName != null) fetchFullTable(tableName);

        currentQueryType = "SELECT";
        if (query.toLowerCase().startsWith("update")) currentQueryType = "UPDATE";
        if (query.toLowerCase().startsWith("delete")) currentQueryType = "DELETE";

        String filterPart = extractFilterFromPlan(planText);
        if (filterPart.isEmpty()) {
            filterPart = extractFilterFromQuery(query);
        }
        final String finalFilter = filterPart;

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
                if (!lastDeletedRows.isEmpty()) {
                    for (Vector<String> deletedRow : lastDeletedRows) {
                        int insertPos = 0;
                        int idCol = -1;
                        for (int i = 0; i < vizTable.getColumnCount(); i++) {
                            if (vizTable.getColumnName(i).toLowerCase().contains("id")) {
                                idCol = i;
                                break;
                            }
                        }

                        if (idCol != -1 && idCol < deletedRow.size()) {
                            try {
                                double targetId = Double.parseDouble(deletedRow.get(idCol));
                                for (int r = 0; r < vizTableModel.getRowCount(); r++) {
                                    try {
                                        double currentId = Double.parseDouble(vizTableModel.getValueAt(r, idCol).toString());
                                        if (currentId < targetId) {
                                            insertPos = r + 1;
                                        } else {
                                            break;
                                        }
                                    } catch (Exception nfe) {}
                                }
                            } catch (Exception ex) {
                                insertPos = 0;
                            }
                        }

                        vizTableModel.insertRow(insertPos, deletedRow);
                        vizTable.scrollRectToVisible(vizTable.getCellRect(insertPos, 0, true));
                        infoArea.append("\n[SIMULATION] Restored target row from cache at sorted position (Row " + (insertPos + 1) + ").");
                    }
                } else {
                    int colCount = vizTable.getColumnCount();
                    Vector<String> placeholder = new Vector<>();
                    for (int i = 0; i < colCount; i++) {
                        placeholder.add("DELETED");
                    }

                    String targetCol = "";
                    String targetVal = "";
                    String op = "=";

                    if (finalFilter != null && !finalFilter.trim().isEmpty()) {
                        String cleanFilter = finalFilter.replaceAll("(?i)[a-zA-Z0-9_]+\\.", "")
                                                        .replaceAll("(?i)::[a-z]+", "")
                                                        .replace("(", "").replace(")", "")
                                                        .replace("'", "").replace("\"", "")
                                                        .trim();

                        String[] parts = null;
                        String lowerFilter = cleanFilter.toLowerCase();
                        if (lowerFilter.contains(">=")) { op = ">="; parts = cleanFilter.split("(?i)>="); }
                        else if (lowerFilter.contains("<=")) { op = "<="; parts = cleanFilter.split("(?i)<="); }
                        else if (lowerFilter.contains("=")) { op = "="; parts = cleanFilter.split("="); }
                        else if (lowerFilter.contains(">")) { op = ">"; parts = cleanFilter.split(">"); }
                        else if (lowerFilter.contains("<")) { op = "<"; parts = cleanFilter.split("<"); }

                        if (parts != null && parts.length >= 2) {
                            targetCol = parts[0].trim().toLowerCase();
                            targetVal = parts[1].trim();
                        }
                    }

                    int targetColIdx = -1;
                    if (!targetCol.isEmpty()) {
                        for (int i = 0; i < colCount; i++) {
                            String actualColName = vizTable.getColumnName(i).toLowerCase();
                            if (actualColName.equals(targetCol) || targetCol.contains(actualColName)) {
                                targetColIdx = i;
                                break;
                            }
                        }
                    }

                    String idVal = "0";
                    int idCol = -1;
                    for (int i = 0; i < colCount; i++) {
                        String colName = vizTable.getColumnName(i).toLowerCase();
                        if (colName.contains("id")) {
                            idCol = i;
                        }
                    }

                    if (targetColIdx != -1) {
                        placeholder.set(targetColIdx, targetVal);
                        if (idCol != -1 && targetColIdx == idCol) {
                            idVal = targetVal;
                        }
                    }

                    for (int i = 0; i < colCount; i++) {
                        if (i == targetColIdx) continue;
                        String colName = vizTable.getColumnName(i).toLowerCase();
                        if (colName.contains("id")) {
                            placeholder.set(i, idVal);
                        } else if (colName.contains("name")) {
                            placeholder.set(i, "TARGET_FOR_DELETE");
                        } else if (colName.contains("marks") || colName.contains("cgpa")) {
                            placeholder.set(i, "0");
                        } else {
                            placeholder.set(i, "DELETED_ROW");
                        }
                    }

                    int insertPos = 0;
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
                                } catch (Exception nfe) {}
                            }
                        } catch (Exception ex) {
                            insertPos = 0;
                        }
                    }

                    vizTableModel.insertRow(insertPos, placeholder);
                    vizTable.scrollRectToVisible(vizTable.getCellRect(insertPos, 0, true));
                    infoArea.append("\n[SIMULATION] Restored target row at sorted position (Row " + (insertPos + 1) + ").");
                }
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
                        JOptionPane.showMessageDialog(null, "FULL SCAN COMPLETE\n\nNo filter applied, entire table was traversed.");
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
            vizCanvas.setStatus("UPDATE: COMMITTING TRANSACTION...");

            Timer flashTimer = new Timer(1000, ev -> {
                matchedRow = -1;
                vizTable.repaint();
                vizCanvas.setStatus("UPDATE COMPLETE");
                JOptionPane.showMessageDialog(null, "UPDATE SUCCESSFUL\n\nRecord safely modified and committed.");
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
        vizCanvas.setStatus("DELETE: MARKING ROW FOR DELETION...");
        Timer deleteTimer = new Timer(1000, e -> {
            vizCanvas.setStatus("DELETE: REORGANIZING TABLE...");

            Timer removalTimer = new Timer(1000, ev -> {
                if (matchedRow != -1) {
                    vizTableModel.removeRow(matchedRow);
                    matchedRow = -1;
                    vizTable.repaint();
                    vizCanvas.setStatus("DELETE COMPLETE (ROWS SHIFTED)");
                    JOptionPane.showMessageDialog(null, "DELETE SUCCESSFUL\n\nRecord removed and storage blocks updated.");
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

        String[] floatingRowData = null;
        int floatX = 50, floatY = 200;

        public void setStatus(String s) { this.status = s; }
        public void setJoinStep(int s) { this.joinStep = s; }
        public void setBTreeStep(int s) {}
        public void setMetadata(String m) { this.metadata = m; }
        public void setFloatingRowData(String[] d) { this.floatingRowData = d; }
        public void setFloatingRowPos(int x, int y) { this.floatX = x; this.floatY = y; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Light dashboard canvas styling
            g2.setColor(CARD_WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(ACCENT_GREEN);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("DBMS ENGINE STATUS: " + status, 20, 25);

            g2.setColor(TEXT_DARK);
            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            if (!metadata.isEmpty()) {
                String[] lines = metadata.split("\n");
                int y = 55;
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    g2.drawString(">> " + line.trim(), 20, y);
                    y += 18;
                    if (y > getHeight() - 15) break;
                }
            }

            if (floatingRowData != null) {
                int rectW = 320;
                int rectH = 42;
                g2.setColor(ACCENT_GREEN);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(floatX, floatY, rectW, rectH, 12, 12);

                g2.setColor(new Color(19, 117, 71, 30));
                g2.fillRoundRect(floatX, floatY, rectW, rectH, 12, 12);

                g2.setColor(TEXT_DARK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String content = String.join(" | ", floatingRowData);
                g2.drawString("NEW ROW: [ " + content + " ]", floatX + 15, floatY + 26);
            }

            if (joinStep >= 0) {
                int xOffset = 400;
                g2.setColor(ACCENT_BLUE);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(xOffset + 50, 60, 110, 80, 10, 10);
                g2.drawString("TABLE A", xOffset + 75, 50);

                g2.setColor(ACCENT_RED);
                g2.drawRoundRect(xOffset + 250, 60, 110, 80, 10, 10);
                g2.drawString("TABLE B", xOffset + 275, 50);

                if (joinStep > 0) {
                    g2.setColor(ACCENT_ORANGE);
                    g2.drawLine(xOffset + 160, 100, xOffset + 250, 100);
                    g2.drawString("MATCHING...", xOffset + 175, 90);
                }
            }
        }
    }

    private String getAssistantReply(String userMessage) {
        String msg = userMessage.toLowerCase().trim();

        if (msg.contains("index") || msg.contains("indices")) {
            return "A Database Index is a data structure (typically a B-Tree) that improves data retrieval times.\n"
                 + "• Cost: Lookups are O(log N) rather than O(N) sequential scans.\n"
                 + "• Trade-off: Speeds up SELECT but writes (INSERT/UPDATE/DELETE) slightly slow down due to index maintenance.";
        }
        if (msg.contains("seq scan") || msg.contains("sequential scan") || msg.contains("table scan")) {
            return "A Sequential Scan checks every single row from top to bottom.\n"
                 + "• When it happens: Filtering on unindexed columns or on very small tables where a scan is cheaper than tree index traversal.\n"
                 + "• Fix: Add a B-Tree index on the search filter column.";
        }
        if (msg.contains("select *") || msg.contains("wildcard")) {
            return "Using 'SELECT *' is generally discouraged because:\n"
                 + "1. Higher I/O: Loads unused columns, increasing network payloads.\n"
                 + "2. Restricts index-only scans: Forces loading main storage heap pages.";
        }
        if (msg.contains("casing")) {
            return "SQL keywords are technically case-insensitive, but writing them in UPPERCASE (e.g. SELECT, FROM, WHERE):\n"
                 + "• Enhances code clarity.\n"
                 + "• Aids parsing and plan caching consistency.";
        }
        if (msg.contains("explain")) {
            return "The 'EXPLAIN' command shows execution plans created by the PostgreSQL optimizer.\n"
                 + "• Shows relative estimation startup and total costs.\n"
                 + "• Shows step execution paths (Seq Scan, Index Scan, etc.).";
        }
        if (msg.contains("join")) {
            return "Common join algorithms:\n"
                 + "1. Nested Loop: Loop through outer table comparing inner table row-by-row.\n"
                 + "2. Hash Join: Build hash table of inner table, probe outer table keys.\n"
                 + "3. Merge Join: Sort both tables and perform sorted linear merge.";
        }
        if (msg.contains("hello") || msg.contains("hi")) {
            return "Hello! I am your performance optimization assistant. Ask me anything about indexes, scans, wildcards, casing, or query plans.";
        }

        return "I can help with query tuning. Try asking:\n"
             + "- 'Why is SELECT * bad?'\n"
             + "- 'What is a Sequential Scan?'\n"
             + "- 'How does a B-Tree index work?'";
    }

    private String callGeminiAPI(String prompt, String queryContext, String reportContext) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        StringBuilder fullBuilder = new StringBuilder();
        fullBuilder.append(prompt);
        if (queryContext != null && !queryContext.isEmpty()) {
            fullBuilder.append("\nContext Query: ").append(queryContext);
        }
        if (reportContext != null && !reportContext.isEmpty()) {
            fullBuilder.append("\nAnalysis Report: ").append(reportContext);
        }
        String fullPrompt = fullBuilder.toString();

        String escapedPrompt = fullPrompt.replace("\\", "\\\\")
                                         .replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "\\r");

        String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + escapedPrompt + "\"}]}]}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "API Error (Code " + response.statusCode() + "): " + response.body();
        }

        return parseGeminiResponse(response.body());
    }

    private String parseGeminiResponse(String json) {
        try {
            int textIndex = json.indexOf("\"text\":");
            if (textIndex == -1) return "Could not parse text response.";
            int startQuote = json.indexOf("\"", textIndex + 7);
            if (startQuote == -1) return "Failed parsing response quote.";
            int endQuote = -1;
            for (int i = startQuote + 1; i < json.length(); i++) {
                if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                     endQuote = i;
                     break;
                }
            }
            if (endQuote == -1) return "Failed matching response quote.";
            String rawText = json.substring(startQuote + 1, endQuote);
            return rawText.replace("\\n", "\n")
                          .replace("\\\"", "\"")
                          .replace("\\\\", "\\");
        } catch (Exception e) {
            return "Parsing error: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        try {
            UIManager.put("TabbedPane.background", CARD_WHITE);
            UIManager.put("TabbedPane.foreground", TEXT_DARK);
            UIManager.put("TabbedPane.selected", SELECTION_BG);
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        } catch (Exception e) {}
        new SQLVisualizerUI();
    }
}