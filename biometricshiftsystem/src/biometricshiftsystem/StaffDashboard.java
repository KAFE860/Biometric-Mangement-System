/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;



/**
 *
 * @author natha
 */
public class StaffDashboard extends JFrame {
    private final int staffId;
    private FingerprintUtils fingerprintUtils;
    
    public StaffDashboard(int staffId) {
        this.staffId = staffId;
        this.fingerprintUtils = new FingerprintUtils();
        
        setTitle("Staff Dashboard");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("My Shifts", createShiftsPanel());
        tabbedPane.addTab("Shift Swaps", createSwapPanel());
        tabbedPane.addTab("My Attendance", createAttendancePanel());
        
        add(tabbedPane);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + getStaffName(staffId));
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            recordSignOut();
            fingerprintUtils.cleanup();
            new Login().setVisible(true);
            dispose();
        });
        topPanel.add(logoutButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
    }
        private void recordSignOut() {
        try (Connection conn = DBConnection.getConnection()) {
            
            String checkSql = "SELECT log_id FROM attendance_logs " +
                            "WHERE staff_id = ? AND DATE(sign_in_time) = CURDATE() " +
                            "AND sign_out_time IS NULL";
            
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, staffId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                
                String updateSql = "UPDATE attendance_logs SET " +
                                 "sign_out_time = NOW(), " +
                                 "status = 'Completed' " +
                                 "WHERE log_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, rs.getInt("log_id"));
                updateStmt.executeUpdate();
                
                
                sendAttendanceNotification(staffId, "out");
            }
        } catch (SQLException ex) {
            System.err.println("Error recording sign-out: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

        
    private JPanel createShiftsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Date", "Shift", "Start Time", "End Time"});
        JTable shiftsTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(shiftsTable);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        loadShiftsData(model);
        
        return panel;
    }
    
    private JPanel createSwapPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultTableModel pendingModel = new DefaultTableModel();
        pendingModel.setColumnIdentifiers(new String[]{"Request ID", "Requested Staff", "Date", "Current Shift", "Requested Shift", "Status"});
        JTable pendingTable = new JTable(pendingModel);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        
        DefaultTableModel historyModel = new DefaultTableModel();
        historyModel.setColumnIdentifiers(new String[]{"Request ID", "Requested Staff", "Date", "Status"});
        JTable historyTable = new JTable(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        
        JTabbedPane swapTabs = new JTabbedPane();
        swapTabs.addTab("My Pending Requests", pendingScroll);
        swapTabs.addTab("Request History", historyScroll);
        
        panel.add(swapTabs, BorderLayout.CENTER);
        
        JButton newSwapButton = new JButton("Request New Shift Swap");
        newSwapButton.addActionListener(e -> new NewSwapDialog(this, staffId).setVisible(true));
        panel.add(newSwapButton, BorderLayout.SOUTH);
        
        loadPendingSwaps(pendingModel);
        loadSwapHistory(historyModel);
        
        return panel;
    }
    
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        
        JPanel fingerprintPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel statusLabel = new JLabel("Place finger on scanner", JLabel.CENTER);
        JLabel fingerprintImage = new JLabel("", JLabel.CENTER);
        fingerprintImage.setPreferredSize(new Dimension(200, 200));
        
        fingerprintPanel.add(statusLabel);
        fingerprintPanel.add(fingerprintImage);
        
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton startScanButton = new JButton("Start Fingerprint Scan");
        JButton stopScanButton = new JButton("Stop Fingerprint Scan");
        
        startScanButton.addActionListener(e -> fingerprintUtils.startCapture(statusLabel, fingerprintImage));
        stopScanButton.addActionListener(e -> {
            fingerprintUtils.stopCapture();
            statusLabel.setText("Scan stopped");
        });
        
        buttonPanel.add(startScanButton);
        buttonPanel.add(stopScanButton);

        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Date", "Shift", "Sign In", "Sign Out", "Status"});
        JTable attendanceTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(attendanceTable);
        scrollPane.setPreferredSize(new Dimension(800, 200));
        
        
        panel.add(fingerprintPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        loadAttendanceData(model);
        fingerprintUtils.setAttendanceCallback(() -> {
            recordAttendance();
            loadAttendanceData(model);
        });
        
        return panel;
    }
    
    private void recordAttendance() {
        try (Connection conn = DBConnection.getConnection()) {
            String checkSql = "SELECT a.log_id, r.shift_id, sh.start_time, sh.end_time " +
                             "FROM attendance_logs a " +
                             "JOIN rosters r ON a.roster_id = r.roster_id " +
                             "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                             "WHERE a.staff_id = ? AND DATE(a.sign_in_time) = CURDATE() " +
                             "AND a.sign_out_time IS NULL";
            
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, staffId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String updateSql = "UPDATE attendance_logs SET sign_out_time = NOW(), " +
                                 "status = 'Completed' WHERE log_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, rs.getInt("log_id"));
                updateStmt.executeUpdate();
                sendAttendanceNotification(staffId, "out");
            } else {
                String shiftSql = "SELECT r.roster_id FROM rosters r " +
                                "WHERE r.staff_id = ? AND r.roster_date = CURDATE()";
                PreparedStatement shiftStmt = conn.prepareStatement(shiftSql);
                shiftStmt.setInt(1, staffId);
                ResultSet shiftRs = shiftStmt.executeQuery();
                
                if (shiftRs.next()) {
                    String insertSql = "INSERT INTO attendance_logs " +
                                     "(staff_id, roster_id, sign_in_time, status) " +
                                     "VALUES (?, ?, NOW(), 'Present')";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, staffId);
                    insertStmt.setInt(2, shiftRs.getInt("roster_id"));
                    insertStmt.executeUpdate();
                    sendAttendanceNotification(staffId, "in");
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No shift scheduled for today", 
                        "No Shift", 
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error recording attendance: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void sendAttendanceNotification(int staffId, String action) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT email, full_name FROM staff WHERE staff_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String subject = action.equals("in") ? "Sign-In Confirmation" : "Sign-Out Confirmation";
                String body = "Dear " + rs.getString("full_name") + ",\n\n" +
                              "You have successfully signed " + (action.equals("in") ? "in" : "out") + 
                              " at " + new java.util.Date() + "\n\n" +
                              "This is an automated notification.";
                
                EmailUtils.sendEmail(rs.getString("email"), subject, body);
            }
        } catch (Exception ex) {
            System.err.println("Error sending attendance notification: " + ex.getMessage());
        }
    }
    
    private void loadShiftsData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT r.roster_date, sh.shift_name, " +
                         "DATE_FORMAT(sh.start_time, '%H:%i') AS start_time, " +
                         "DATE_FORMAT(sh.end_time, '%H:%i') AS end_time " +
                         "FROM rosters r " +
                         "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                         "WHERE r.staff_id = ? AND r.roster_date >= CURDATE() " +
                         "ORDER BY r.roster_date LIMIT 30";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getDate("roster_date"),
                    rs.getString("shift_name"),
                    rs.getString("start_time"),
                    rs.getString("end_time")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading shifts: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadPendingSwaps(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sw.swap_id, s.full_name AS requested_staff, " +
                         "r.roster_date, sh1.shift_name AS current_shift, sh2.shift_name AS requested_shift, sw.status " +
                         "FROM shift_swaps sw " +
                         "JOIN staff s ON sw.requested_staff_id = s.staff_id " +
                         "JOIN rosters r ON sw.roster_id = r.roster_id " +
                         "JOIN shifts sh1 ON r.shift_id = sh1.shift_id " +
                         "JOIN rosters r2 ON sw.requested_roster_id = r2.roster_id " +
                         "JOIN shifts sh2 ON r2.shift_id = sh2.shift_id " +
                         "WHERE sw.requester_id = ? AND sw.status = 'Pending' " +
                         "ORDER BY r.roster_date";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("swap_id"),
                    rs.getString("requested_staff"),
                    rs.getDate("roster_date"),
                    rs.getString("current_shift"),
                    rs.getString("requested_shift"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading pending swaps: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSwapHistory(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sw.swap_id, s.full_name AS requested_staff, " +
                         "r.roster_date, sw.status " +
                         "FROM shift_swaps sw " +
                         "JOIN staff s ON sw.requested_staff_id = s.staff_id " +
                         "JOIN rosters r ON sw.roster_id = r.roster_id " +
                         "WHERE sw.requester_id = ? AND sw.status != 'Pending' " +
                         "ORDER BY r.roster_date DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("swap_id"),
                    rs.getString("requested_staff"),
                    rs.getDate("roster_date"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading swap history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAttendanceData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT r.roster_date, sh.shift_name, " +
                         "DATE_FORMAT(a.sign_in_time, '%H:%i') AS sign_in, " +
                         "DATE_FORMAT(a.sign_out_time, '%H:%i') AS sign_out, a.status " +
                         "FROM attendance_logs a " +
                         "JOIN rosters r ON a.roster_id = r.roster_id " +
                         "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                         "WHERE a.staff_id = ? AND r.roster_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                         "ORDER BY r.roster_date DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getDate("roster_date"),
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
    
    private String getStaffName(int staffId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT full_name FROM staff WHERE staff_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("full_name");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }
}

class NewSwapDialog extends JDialog {
    private final int staffId;
    private final JComboBox<String> dateCombo;
    private final JComboBox<String> staffCombo;
    private final JComboBox<String> shiftCombo;
    
    public NewSwapDialog(JFrame parent, int staffId) {
        super(parent, "Request New Shift Swap", true);
        this.staffId = staffId;
        setSize(500, 300);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Select Date:"), gbc);
        
        gbc.gridx = 1;
        dateCombo = new JComboBox<>();
        loadAvailableDates();
        panel.add(dateCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Select Staff:"), gbc);
        
        gbc.gridx = 1;
        staffCombo = new JComboBox<>();
        loadAvailableStaff();
        panel.add(staffCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Select Shift:"), gbc);
        
        gbc.gridx = 1;
        shiftCombo = new JComboBox<>();
        panel.add(shiftCombo, gbc);
        
        dateCombo.addActionListener(e -> updateShiftCombo());
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        JButton requestButton = new JButton("Request Swap");
        panel.add(requestButton, gbc);
        
        gbc.gridy = 4;
        JButton cancelButton = new JButton("Cancel");
        panel.add(cancelButton, gbc);
        
        add(panel);
        
        requestButton.addActionListener(e -> requestSwap());
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void loadAvailableDates() {
        dateCombo.removeAllItems();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT DISTINCT roster_date FROM rosters " +
                "WHERE staff_id = ? " +
                "ORDER BY roster_date");
            
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                dateCombo.addItem(rs.getDate("roster_date").toString());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading dates: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAvailableStaff() {
        try (Connection conn = DBConnection.getConnection()) {
            String unitSql = "SELECT u.unit_id FROM staff s JOIN units u ON s.unit_id = u.unit_id WHERE s.staff_id = ?";
            PreparedStatement unitStmt = conn.prepareStatement(unitSql);
            unitStmt.setInt(1, staffId);
            ResultSet unitRs = unitStmt.executeQuery();
            
            if (unitRs.next()) {
                int unitId = unitRs.getInt("unit_id");
                
                String staffSql = "SELECT staff_id, full_name FROM staff WHERE unit_id = ? AND staff_id != ? ORDER BY full_name";
                PreparedStatement staffStmt = conn.prepareStatement(staffSql);
                staffStmt.setInt(1, unitId);
                staffStmt.setInt(2, staffId);
                ResultSet staffRs = staffStmt.executeQuery();
                
                while (staffRs.next()) {
                    staffCombo.addItem(staffRs.getString("full_name") + " (" + staffRs.getInt("staff_id") + ")");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading staff: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateShiftCombo() {
        shiftCombo.removeAllItems();
        
        if (dateCombo.getSelectedItem() == null || staffCombo.getSelectedItem() == null) return;
        
        try (Connection conn = DBConnection.getConnection()) {
            String date = (String) dateCombo.getSelectedItem();
            String staffInfo = (String) staffCombo.getSelectedItem();
            int requestedStaffId = Integer.parseInt(staffInfo.substring(staffInfo.lastIndexOf("(") + 1, staffInfo.lastIndexOf(")")));
            
            String requesterSql = "SELECT sh.shift_id FROM rosters r " +
                                 "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                                 "WHERE r.staff_id = ? AND r.roster_date = ?";
            PreparedStatement requesterStmt = conn.prepareStatement(requesterSql);
            requesterStmt.setInt(1, staffId);
            requesterStmt.setString(2, date);
            ResultSet requesterRs = requesterStmt.executeQuery();
            
            if (requesterRs.next()) {
                int requesterShiftId = requesterRs.getInt("shift_id");
                
                String requestedSql = "SELECT sh.shift_name FROM rosters r " +
                                     "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                                     "WHERE r.staff_id = ? AND r.roster_date = ? AND sh.shift_id != ?";
                PreparedStatement requestedStmt = conn.prepareStatement(requestedSql);
                requestedStmt.setInt(1, requestedStaffId);
                requestedStmt.setString(2, date);
                requestedStmt.setInt(3, requesterShiftId);
                ResultSet requestedRs = requestedStmt.executeQuery();
                
                while (requestedRs.next()) {
                    shiftCombo.addItem(requestedRs.getString("shift_name"));
                }
                
                if (shiftCombo.getItemCount() == 0) {
                    shiftCombo.addItem("No available shifts to swap");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading shifts: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void requestSwap() {
        if (dateCombo.getSelectedItem() == null || staffCombo.getSelectedItem() == null || 
            shiftCombo.getSelectedItem() == null || shiftCombo.getSelectedItem().equals("No available shifts to swap")) {
            JOptionPane.showMessageDialog(this, "Please select all required fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String date = (String) dateCombo.getSelectedItem();
            String staffInfo = (String) staffCombo.getSelectedItem();
            int requestedStaffId = Integer.parseInt(staffInfo.substring(staffInfo.lastIndexOf("(") + 1, staffInfo.lastIndexOf(")")));
            String requestedShiftName = (String) shiftCombo.getSelectedItem();
            
            String requesterRosterSql = "SELECT r.roster_id FROM rosters r " +
                                       "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                                       "WHERE r.staff_id = ? AND r.roster_date = ?";
            PreparedStatement requesterRosterStmt = conn.prepareStatement(requesterRosterSql);
            requesterRosterStmt.setInt(1, staffId);
            requesterRosterStmt.setString(2, date);
            ResultSet requesterRosterRs = requesterRosterStmt.executeQuery();
            
            if (!requesterRosterRs.next()) {
                JOptionPane.showMessageDialog(this, "You don't have a shift scheduled for the selected date", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int requesterRosterId = requesterRosterRs.getInt("roster_id");
            
            String requestedRosterSql = "SELECT r.roster_id FROM rosters r " +
                                      "JOIN shifts sh ON r.shift_id = sh.shift_id " +
                                      "WHERE r.staff_id = ? AND r.roster_date = ? AND sh.shift_name = ?";
            PreparedStatement requestedRosterStmt = conn.prepareStatement(requestedRosterSql);
            requestedRosterStmt.setInt(1, requestedStaffId);
            requestedRosterStmt.setString(2, date);
            requestedRosterStmt.setString(3, requestedShiftName);
            ResultSet requestedRosterRs = requestedRosterStmt.executeQuery();
            
            if (!requestedRosterRs.next()) {
                JOptionPane.showMessageDialog(this, "The selected staff doesn't have the requested shift", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int requestedRosterId = requestedRosterRs.getInt("roster_id");
            
            String insertSql = "INSERT INTO shift_swaps (requester_id, requested_staff_id, roster_id, requested_roster_id, status) " +
                             "VALUES (?, ?, ?, ?, 'Pending')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, staffId);
            insertStmt.setInt(2, requestedStaffId);
            insertStmt.setInt(3, requesterRosterId);
            insertStmt.setInt(4, requestedRosterId);
            
            int affectedRows = insertStmt.executeUpdate();
            
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Shift swap request submitted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error requesting swap: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }  
    }
}