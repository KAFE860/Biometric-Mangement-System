/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.io.*;

/**
 *
 * @author natha
 */
public class SupervisorDashboard extends JFrame {
    private final int supervisorId;
    private final String unit;
    
    public SupervisorDashboard(int supervisorId, String unit) {
        this.supervisorId = supervisorId;
        this.unit = unit;
        
        setTitle("Supervisor Dashboard - " + unit);
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Attendance", createAttendancePanel());
        tabbedPane.addTab("Roster Management", createRosterPanel());
        tabbedPane.addTab("Shift Swaps", createSwapPanel());
        
        add(tabbedPane);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Welcome, " + getStaffName(supervisorId) + " (" + unit + " Supervisor)"), BorderLayout.WEST);
        
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
        filterPanel.add(new JLabel("Date:"));
        JTextField dateField = new JTextField(10);
        dateField.setText(java.time.LocalDate.now().toString());
        filterPanel.add(dateField);
        
        filterPanel.add(new JLabel("Staff ID:"));
        JTextField staffIdField = new JTextField(5);
        filterPanel.add(staffIdField);
        
        JButton filterButton = new JButton("Filter");
        filterPanel.add(filterButton);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Staff ID", "Staff Name", "Shift", "Sign In", "Sign Out", "Status"});
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        filterButton.addActionListener(e -> {
            String date = dateField.getText();
            String staffId = staffIdField.getText().trim();
            loadAttendanceData(model, date, staffId);
        });
        
        loadAttendanceData(model, dateField.getText(), null);
        return panel;
    }
    
    private JPanel createRosterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Date:"));
        JTextField dateField = new JTextField(10);
        dateField.setText(java.time.LocalDate.now().toString());
        filterPanel.add(dateField);
        
        filterPanel.add(new JLabel("Staff ID:"));
        JTextField staffIdField = new JTextField(5);
        filterPanel.add(staffIdField);
        
        JButton filterButton = new JButton("Filter");
        filterPanel.add(filterButton);
        
        JButton generateButton = new JButton("Generate");
        filterPanel.add(generateButton);
        
        JButton uploadButton = new JButton("Upload CSV");
        filterPanel.add(uploadButton);
        
        JButton downloadButton = new JButton("Download CSV");
        filterPanel.add(downloadButton);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Staff ID", "Staff Name", "Shift", "Start Time", "End Time"});
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        filterButton.addActionListener(e -> {
            String date = dateField.getText();
            String staffId = staffIdField.getText().trim();
            loadRosterData(model, date, staffId);
        });
        
        generateButton.addActionListener(e -> {
            generateDefaultRoster(dateField.getText());
            loadRosterData(model, dateField.getText(), null);
        });
        
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                processRosterUpload(fileChooser.getSelectedFile(), dateField.getText());
                loadRosterData(model, dateField.getText(), null);
            }
        });
        
        downloadButton.addActionListener(e -> downloadRosterAsCSV(table, dateField.getText()));
        
        loadRosterData(model, dateField.getText(), null);
        return panel;
    }
    
    private JPanel createSwapPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Request ID", "Requester", "Requested Staff", "Date", "Current Shift", "Requested Shift", "Action"});
        JTable table = new JTable(model);
        
        TableColumn actionColumn = table.getColumnModel().getColumn(6);
        actionColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(new String[]{"Approve", "Reject"})));
        
        JButton processButton = new JButton("Process Selected");
        processButton.addActionListener(e -> processSwapRequests(table, model));
        
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(processButton, BorderLayout.SOUTH);
        
        loadPendingSwaps(model);
        return panel;
    }
    
    private void loadAttendanceData(DefaultTableModel model, String date, String staffId) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.staff_id, s.full_name, sh.shift_name, " +
                         "DATE_FORMAT(a.sign_in_time, '%H:%i') AS sign_in, " +
                         "DATE_FORMAT(a.sign_out_time, '%H:%i') AS sign_out, a.status " +
                         "FROM attendance_logs a " +
                         "JOIN staff s ON a.staff_id = s.staff_id " +
                         "JOIN rosters r ON a.roster_id = r.roster_id " +
                         "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                         "WHERE s.unit_id = (SELECT unit_id FROM staff WHERE staff_id = ?) " +
                         "AND DATE(a.sign_in_time) = ? " +
                         (staffId != null && !staffId.isEmpty() ? "AND s.staff_id = ? " : "") +
                         "ORDER BY a.sign_in_time";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, supervisorId);
            stmt.setString(2, date);
            if (staffId != null && !staffId.isEmpty()) stmt.setInt(3, Integer.parseInt(staffId));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("shift_name"),
                    rs.getString("sign_in"),
                    rs.getString("sign_out"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadRosterData(DefaultTableModel model, String date, String staffId) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.staff_id, s.full_name, sh.shift_name, " +
                         "DATE_FORMAT(sh.start_time, '%H:%i') AS start_time, " +
                         "DATE_FORMAT(sh.end_time, '%H:%i') AS end_time " +
                         "FROM rosters r " +
                         "JOIN staff s ON r.staff_id = s.staff_id " +
                         "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                         "WHERE s.unit_id = (SELECT unit_id FROM staff WHERE staff_id = ?) " +
                         "AND r.roster_date = ? " +
                         (staffId != null && !staffId.isEmpty() ? "AND s.staff_id = ? " : "") +
                         "ORDER BY sh.start_time";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, supervisorId);
            stmt.setString(2, date);
            if (staffId != null && !staffId.isEmpty()) stmt.setInt(3, Integer.parseInt(staffId));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("shift_name"),
                    rs.getString("start_time"),
                    rs.getString("end_time")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading roster: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadPendingSwaps(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sw.swap_id, s1.full_name AS requester, s2.full_name AS requested_staff, " +
                         "r.roster_date, sh1.shift_name AS current_shift, sh2.shift_name AS requested_shift " +
                         "FROM shift_swaps sw " +
                         "JOIN staff s1 ON sw.requester_id = s1.staff_id " +
                         "JOIN staff s2 ON sw.requested_staff_id = s2.staff_id " +
                         "JOIN rosters r ON sw.roster_id = r.roster_id " +
                         "JOIN shifts sh1 ON r.shift_id = sh1.shift_id " +
                         "JOIN rosters r2 ON sw.requested_roster_id = r2.roster_id " +
                         "JOIN shifts sh2 ON r2.shift_id = sh2.shift_id " +
                         "WHERE s1.unit_id = (SELECT unit_id FROM staff WHERE staff_id = ?) " +
                         "AND sw.status = 'Pending' " +
                         "ORDER BY r.roster_date";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, supervisorId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("swap_id"),
                    rs.getString("requester"),
                    rs.getString("requested_staff"),
                    rs.getDate("roster_date"),
                    rs.getString("current_shift"),
                    rs.getString("requested_shift"),
                    ""
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading swaps: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void generateDefaultRoster(String date) {
        try (Connection conn = DBConnection.getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL GenerateDefaultRoster(?, ?, ?)}");
            stmt.setString(1, date);
            stmt.setString(2, unit);
            stmt.setInt(3, supervisorId);
            stmt.execute();
            
            sendRosterUpdateNotifications(date);
            JOptionPane.showMessageDialog(this, "Default roster generated for " + date, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error generating roster: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void processRosterUpload(File file, String date) {
        try (Connection conn = DBConnection.getConnection()) {
            
            String deleteSql = "DELETE FROM rosters WHERE roster_date = ? AND staff_id IN " +
                             "(SELECT staff_id FROM staff WHERE unit_id = (SELECT unit_id FROM staff WHERE staff_id = ?))";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setString(1, date);
            deleteStmt.setInt(2, supervisorId);
            deleteStmt.executeUpdate();
            
            
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5 && !parts[0].equals("Staff ID")) {
                    try {
                        int staffId = Integer.parseInt(parts[0]);
                        String shiftName = parts[2];
                        
                        
                        String shiftSql = "SELECT shift_id FROM shifts WHERE shift_name = ?";
                        PreparedStatement shiftStmt = conn.prepareStatement(shiftSql);
                        shiftStmt.setString(1, shiftName);
                        ResultSet rs = shiftStmt.executeQuery();
                        
                        if (rs.next()) {
                            String insertSql = "INSERT INTO rosters (staff_id, shift_id, roster_date, created_by) VALUES (?, ?, ?, ?)";
                            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                            insertStmt.setInt(1, staffId);
                            insertStmt.setInt(2, rs.getInt("shift_id"));
                            insertStmt.setString(3, date);
                            insertStmt.setInt(4, supervisorId);
                            insertStmt.executeUpdate();
                            count++;
                        }
                    } catch (NumberFormatException e) {
                        
                    }
                }
            }
            
            sendRosterUpdateNotifications(date);
            JOptionPane.showMessageDialog(this, "Successfully processed " + count + " roster entries", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void downloadRosterAsCSV(JTable table, String date) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(unit + "_roster_" + date + ".csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                
                for (int i = 0; i < table.getColumnCount(); i++) {
                    writer.print(table.getColumnName(i));
                    if (i < table.getColumnCount() - 1) writer.print(",");
                }
                writer.println();
                
                
                for (int i = 0; i < table.getRowCount(); i++) {
                    for (int j = 0; j < table.getColumnCount(); j++) {
                        writer.print(table.getValueAt(i, j));
                        if (j < table.getColumnCount() - 1) writer.print(",");
                    }
                    writer.println();
                }
                
                JOptionPane.showMessageDialog(this, "Roster downloaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error downloading roster: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void processSwapRequests(JTable table, DefaultTableModel model) {
        try (Connection conn = DBConnection.getConnection()) {
            for (int i = 0; i < table.getRowCount(); i++) {
                String action = (String) table.getValueAt(i, 6);
                if (action != null && !action.isEmpty()) {
                    int swapId = (Integer) table.getValueAt(i, 0);
                    
                    if (action.equals("Approve")) {
                        
                        String swapSql = "SELECT roster_id, requested_roster_id FROM shift_swaps WHERE swap_id = ?";
                        PreparedStatement swapStmt = conn.prepareStatement(swapSql);
                        swapStmt.setInt(1, swapId);
                        ResultSet rs = swapStmt.executeQuery();
                        
                        if (rs.next()) {
                            int rosterId = rs.getInt("roster_id");
                            int requestedRosterId = rs.getInt("requested_roster_id");
                            
                            
                            String updateSql = "UPDATE rosters SET shift_id = (SELECT shift_id FROM rosters WHERE roster_id = ?) WHERE roster_id = ?";
                            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                            updateStmt.setInt(1, requestedRosterId);
                            updateStmt.setInt(2, rosterId);
                            updateStmt.executeUpdate();
                            
                            updateStmt.setInt(1, rosterId);
                            updateStmt.setInt(2, requestedRosterId);
                            updateStmt.executeUpdate();
                        }
                    }
                    
                    
                    String statusSql = "UPDATE shift_swaps SET status = ?, action_by = ?, action_at = NOW() WHERE swap_id = ?";
                    PreparedStatement statusStmt = conn.prepareStatement(statusSql);
                    statusStmt.setString(1, action.equals("Approve") ? "Approved" : "Rejected");
                    statusStmt.setInt(2, supervisorId);
                    statusStmt.setInt(3, swapId);
                    statusStmt.executeUpdate();
                    
                    sendSwapNotification(swapId, action.equals("Approve") ? "Approved" : "Rejected");
                }
            }
            
            loadPendingSwaps(model);
            JOptionPane.showMessageDialog(this, "Swap requests processed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing swaps: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void sendRosterUpdateNotifications(String date) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.email, s.full_name, sh.shift_name, " +
                        "DATE_FORMAT(sh.start_time, '%H:%i') AS start_time " +
                        "FROM rosters r " +
                        "JOIN staff s ON r.staff_id = s.staff_id " +
                        "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                        "WHERE s.unit_id = (SELECT unit_id FROM staff WHERE staff_id = ?) " +
                        "AND r.roster_date = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, supervisorId);
            stmt.setString(2, date);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String subject = "Shift Schedule Update";
                String body = "Dear " + rs.getString("full_name") + ",\n\n" +
                              "Your shift for " + date + " has been scheduled:\n\n" +
                              "Shift: " + rs.getString("shift_name") + "\n" +
                              "Start Time: " + rs.getString("start_time") + "\n\n" +
                              "Please contact your supervisor if you have any questions.";
                
                EmailUtils.sendEmail(rs.getString("email"), subject, body);
            }
        } catch (Exception ex) {
            System.err.println("Error sending notifications: " + ex.getMessage());
        }
    }
    
    private void sendSwapNotification(int swapId, String status) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s1.email AS requester_email, s2.email AS requested_email, " +
                         "r.roster_date, sh1.shift_name AS current_shift, sh2.shift_name AS requested_shift " +
                         "FROM shift_swaps sw " +
                         "JOIN staff s1 ON sw.requester_id = s1.staff_id " +
                         "JOIN staff s2 ON sw.requested_staff_id = s2.staff_id " +
                         "JOIN rosters r ON sw.roster_id = r.roster_id " +
                         "JOIN shifts sh1 ON r.shift_id = sh1.shift_id " +
                         "JOIN rosters r2 ON sw.requested_roster_id = r2.roster_id " +
                         "JOIN shifts sh2 ON r2.shift_id = sh2.shift_id " +
                         "WHERE sw.swap_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, swapId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String subject = "Shift Swap Request " + status;
                String body = "Your shift swap request for " + rs.getDate("roster_date") + " has been " + status + ".\n\n" +
                              "Original Shift: " + rs.getString("current_shift") + "\n" +
                              "Requested Shift: " + rs.getString("requested_shift") + "\n\n" +
                              "This is an automated notification.";
                
                EmailUtils.sendEmail(rs.getString("requester_email"), subject, body);
                
                if (status.equals("Approved")) {
                    EmailUtils.sendEmail(rs.getString("requested_email"), subject, body);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error sending swap notification: " + ex.getMessage());
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