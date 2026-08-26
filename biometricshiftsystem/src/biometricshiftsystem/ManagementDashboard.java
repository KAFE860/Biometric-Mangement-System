/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.io.*;
import java.time.LocalDate;
import java.time.Month;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author natha
 */
public class ManagementDashboard extends JFrame {
    private final int managerId;
    
    public ManagementDashboard(int managerId) {
        this.managerId = managerId;
        
        setTitle("Management Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Attendance Logs", createAttendancePanel());
        tabbedPane.addTab("Staff Rosters", createRosterPanel());
        tabbedPane.addTab("Reports", createReportsPanel());
        add(tabbedPane);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Welcome, " + getStaffName(managerId)), BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
    }
    
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Unit:"));
        JComboBox<String> unitCombo = new JComboBox<>();
        loadUnits(unitCombo);
        filterPanel.add(unitCombo);
        
        filterPanel.add(new JLabel("Date:"));
        JTextField dateField = new JTextField(10);
        dateField.setText(java.time.LocalDate.now().toString());
        filterPanel.add(dateField);
        
        filterPanel.add(new JLabel("Staff ID:"));
        JTextField staffIdField = new JTextField(5);
        filterPanel.add(staffIdField);
        
        JButton filterButton = new JButton("Filter");
        filterPanel.add(filterButton);
        
        JButton exportButton = new JButton("Export CSV");
        filterPanel.add(exportButton);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Staff ID", "Staff Name", "Unit", "Sign In", "Sign Out", "Status"});
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        filterButton.addActionListener(e -> {
            String unit = unitCombo.getSelectedItem().toString();
            if ("All Units".equals(unit)) unit = null;
            String date = dateField.getText();
            String staffId = staffIdField.getText().trim();
            loadAttendanceData(model, unit, date, staffId);
        });
        
        exportButton.addActionListener(e -> exportToCSV(table, "attendance_" + dateField.getText() + ".csv"));
        
        loadAttendanceData(model, null, dateField.getText(), null);
        return panel;
    }
    
     private JPanel createReportsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    
    
    JPanel reportPanel = new JPanel();
    reportPanel.add(new JLabel("Report Type:"));
    JComboBox<String> reportCombo = new JComboBox<>(new String[] {
        "Monthly Attendance Summary",
        "Late Arrivals Report", 
        "Early Departures Report",
        "Absenteeism Report"
    });
    reportPanel.add(reportCombo);
    
   
    reportPanel.add(new JLabel("Month:"));
    JComboBox<String> monthCombo = new JComboBox<>(new String[] {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    });
    monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
    reportPanel.add(monthCombo);
    
    reportPanel.add(new JLabel("Year:"));
    JComboBox<Integer> yearCombo = new JComboBox<>();
    int currentYear = LocalDate.now().getYear();
    for (int i = currentYear - 1; i <= currentYear + 1; i++) {
        yearCombo.addItem(i);
    }
    yearCombo.setSelectedItem(currentYear);
    reportPanel.add(yearCombo);
    
    
    reportPanel.add(new JLabel("Unit:"));
    JComboBox<String> unitCombo = new JComboBox<>();
    loadUnits(unitCombo);
    reportPanel.add(unitCombo);
    
    
    JButton generateButton = new JButton("Generate");
    reportPanel.add(generateButton);
    
    JButton exportButton = new JButton("Export CSV");
    reportPanel.add(exportButton);
    
    panel.add(reportPanel, BorderLayout.NORTH);
    
    
    DefaultTableModel model = new DefaultTableModel();
    JTable reportTable = new JTable(model);
    panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
    
    
    generateButton.addActionListener(e -> {
        String reportType = (String) reportCombo.getSelectedItem();
        String month = (String) monthCombo.getSelectedItem();
        int year = (int) yearCombo.getSelectedItem();
        String unit = unitCombo.getSelectedItem().toString();
        if ("All Units".equals(unit)) unit = null;
        
        generateReport(model, reportType, month, year, unit);
    });
    
    
    exportButton.addActionListener(e -> {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No report data to export. Please generate a report first.",
                "Export Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report to CSV");
        
        
        String reportType = (String) reportCombo.getSelectedItem();
        String month = (String) monthCombo.getSelectedItem();
        int year = (int) yearCombo.getSelectedItem();
        String defaultName = String.format("%s_%s_%d.csv", 
            reportType.replaceAll("\\s+", "_"), month, year);
        fileChooser.setSelectedFile(new File(defaultName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(file)) {
                
                for (int i = 0; i < model.getColumnCount(); i++) {
                    pw.print(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) pw.print(",");
                }
                pw.println();
                
                
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object value = model.getValueAt(row, col);
                        pw.print(value != null ? value.toString() : "");
                        if (col < model.getColumnCount() - 1) pw.print(",");
                    }
                    pw.println();
                }
                
                JOptionPane.showMessageDialog(this,
                    "Report exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting report: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    });
    
    return panel;
}

private void generateReport(DefaultTableModel model, String reportType, 
                          String month, int year, String unit) {
    model.setRowCount(0);
    model.setColumnCount(0);
    
    try (Connection conn = DBConnection.getConnection()) {
        String sql = "";
        String[] columns = {};
        
        switch (reportType) {
            case "Monthly Attendance Summary":
                columns = new String[]{"Staff ID", "Staff Name", "Unit", "Days Worked", "Total Hours"};
                sql = "SELECT s.staff_id, s.full_name, u.unit_name, " +
                      "COUNT(DISTINCT DATE(a.sign_in_time)) AS days_worked, " +
                      "SUM(TIMESTAMPDIFF(HOUR, a.sign_in_time, a.sign_out_time)) AS total_hours " +
                      "FROM attendance_logs a " +
                      "JOIN staff s ON a.staff_id = s.staff_id " +
                      "JOIN units u ON s.unit_id = u.unit_id " +
                      "WHERE MONTH(a.sign_in_time) = ? AND YEAR(a.sign_in_time) = ? " +
                      (unit != null ? "AND u.unit_name = ? " : "") +
                      "GROUP BY s.staff_id, s.full_name, u.unit_name";
                break;
                
            case "Late Arrivals Report":
                columns = new String[]{"Staff ID", "Staff Name", "Unit", "Date", "Scheduled Time", "Actual Time", "Minutes Late"};
                sql = "SELECT s.staff_id, s.full_name, u.unit_name, " +
                      "DATE(a.sign_in_time) AS date, " +
                      "sh.start_time AS scheduled_time, " +
                      "TIME(a.sign_in_time) AS actual_time, " +
                      "TIMESTAMPDIFF(MINUTE, CONCAT(DATE(a.sign_in_time), ' ', sh.start_time), a.sign_in_time) AS minutes_late " +
                      "FROM attendance_logs a " +
                      "JOIN rosters r ON a.roster_id = r.roster_id " +
                      "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                      "JOIN staff s ON a.staff_id = s.staff_id " +
                      "JOIN units u ON s.unit_id = u.unit_id " +
                      "WHERE MONTH(a.sign_in_time) = ? AND YEAR(a.sign_in_time) = ? " +
                      "AND TIMESTAMPDIFF(MINUTE, CONCAT(DATE(a.sign_in_time), ' ', sh.start_time), a.sign_in_time) > 5 " +
                      (unit != null ? "AND u.unit_name = ? " : "") +
                      "ORDER BY minutes_late DESC";
                break;
        }
        
        model.setColumnIdentifiers(columns);
        
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Month.valueOf(month.toUpperCase()).getValue());
        stmt.setInt(2, year);
        if (unit != null) stmt.setString(3, unit);
        
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                row[i] = rs.getObject(i + 1);
            }
            model.addRow(row);
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Error generating report: " + ex.getMessage(),
            "Report Error",
            JOptionPane.ERROR_MESSAGE);
    }
}

private void exportToCSV(JTable table, String defaultFileName) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Report to CSV");
    fileChooser.setSelectedFile(new File(defaultFileName));
    
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        
        
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }
        
        try (PrintWriter writer = new PrintWriter(file)) {
            
            for (int i = 0; i < table.getColumnCount(); i++) {
                writer.write(table.getColumnName(i));
                if (i < table.getColumnCount() - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");
            
            
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Object value = table.getValueAt(row, col);
                    writer.write(value != null ? value.toString() : "");
                    if (col < table.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
            
            JOptionPane.showMessageDialog(this,
                "Report exported successfully to:\n" + file.getAbsolutePath(),
                "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error exporting report: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
    
    
    private JPanel createRosterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Unit:"));
        JComboBox<String> unitCombo = new JComboBox<>();
        loadUnits(unitCombo);
        filterPanel.add(unitCombo);
        
        filterPanel.add(new JLabel("Date:"));
        JTextField dateField = new JTextField(10);
        dateField.setText(java.time.LocalDate.now().toString());
        filterPanel.add(dateField);
        
        filterPanel.add(new JLabel("Staff ID:"));
        JTextField staffIdField = new JTextField(5);
        filterPanel.add(staffIdField);
        
        JButton filterButton = new JButton("Filter");
        filterPanel.add(filterButton);
        
        JButton exportButton = new JButton("Export CSV");
        filterPanel.add(exportButton);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Staff ID", "Staff Name", "Unit", "Shift", "Start Time", "End Time"});
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        filterButton.addActionListener(e -> {
            String unit = unitCombo.getSelectedItem().toString();
            if ("All Units".equals(unit)) unit = null;
            String date = dateField.getText();
            String staffId = staffIdField.getText().trim();
            loadRosterData(model, unit, date, staffId);
        });
        
        exportButton.addActionListener(e -> exportToCSV(table, "roster_" + dateField.getText() + ".csv"));
        
        loadRosterData(model, null, dateField.getText(), null);
        return panel;
    }
    
    private void loadUnits(JComboBox<String> comboBox) {
        comboBox.addItem("All Units");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT unit_name FROM units";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                comboBox.addItem(rs.getString("unit_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading units: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAttendanceData(DefaultTableModel model, String unit, String date, String staffId) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.staff_id, s.full_name, u.unit_name, " +
                         "DATE_FORMAT(a.sign_in_time, '%H:%i') AS sign_in, " +
                         "DATE_FORMAT(a.sign_out_time, '%H:%i') AS sign_out, a.status " +
                         "FROM attendance_logs a " +
                         "JOIN staff s ON a.staff_id = s.staff_id " +
                         "JOIN units u ON s.unit_id = u.unit_id " +
                         "WHERE DATE(a.sign_in_time) = ? " +
                         (unit != null ? "AND u.unit_name = ? " : "") +
                         (staffId != null && !staffId.isEmpty() ? "AND s.staff_id = ? " : "") +
                         "ORDER BY u.unit_name, a.sign_in_time";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            int paramIndex = 1;
            stmt.setString(paramIndex++, date);
            if (unit != null) stmt.setString(paramIndex++, unit);
            if (staffId != null && !staffId.isEmpty()) stmt.setInt(paramIndex++, Integer.parseInt(staffId));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("unit_name"),
                    rs.getString("sign_in"),
                    rs.getString("sign_out"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadRosterData(DefaultTableModel model, String unit, String date, String staffId) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.staff_id, s.full_name, u.unit_name, sh.shift_name, " +
                         "DATE_FORMAT(sh.start_time, '%H:%i') AS start_time, " +
                         "DATE_FORMAT(sh.end_time, '%H:%i') AS end_time " +
                         "FROM rosters r " +
                         "JOIN staff s ON r.staff_id = s.staff_id " +
                         "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                         "JOIN units u ON s.unit_id = u.unit_id " +
                         "WHERE r.roster_date = ? " +
                         (unit != null ? "AND u.unit_name = ? " : "") +
                         (staffId != null && !staffId.isEmpty() ? "AND s.staff_id = ? " : "") +
                         "ORDER BY u.unit_name, sh.start_time";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            int paramIndex = 1;
            stmt.setString(paramIndex++, date);
            if (unit != null) stmt.setString(paramIndex++, unit);
            if (staffId != null && !staffId.isEmpty()) stmt.setInt(paramIndex++, Integer.parseInt(staffId));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("unit_name"),
                    rs.getString("shift_name"),
                    rs.getString("start_time"),
                    rs.getString("end_time")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading roster: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
 
    
    private String getStaffName(int staffId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT full_name FROM staff WHERE staff_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }
}