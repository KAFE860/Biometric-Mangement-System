/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.JFrame;


/**
 *
 * @author natha
 */
public class AdminDashboard extends JFrame {
    private final JTabbedPane tabbedPane;
    
    public AdminDashboard(int adminId) {
        setTitle("Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        
        
        tabbedPane.addTab("Manage Staff", createStaffManagementPanel());
        
        
        add(tabbedPane);
        
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });
        topPanel.add(logoutButton);
        add(topPanel, BorderLayout.NORTH);
    }
    
    private JPanel createStaffManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"ID", "Username", "Full Name", "Email", "Unit", "Role", "Status"});
        JTable staffTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(staffTable);
        
        
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Staff");
        JButton editButton = new JButton("Edit Staff");
        JButton deactivateButton = new JButton("Deactivate/Activate");
        JButton resetPasswordButton = new JButton("Reset Password");
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deactivateButton);
        buttonPanel.add(resetPasswordButton);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        
        loadStaffData(model);
        
        
        addButton.addActionListener(e -> {
            new AddStaffDialog(this).setVisible(true);
            loadStaffData(model);
        });
        
        editButton.addActionListener(e -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow >= 0) {
                int staffId = (int) staffTable.getValueAt(selectedRow, 0);
                new EditStaffDialog(this, staffId).setVisible(true);
                loadStaffData(model);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a staff member to edit", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        deactivateButton.addActionListener(e -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow >= 0) {
                int staffId = (int) staffTable.getValueAt(selectedRow, 0);
                boolean isActive = staffTable.getValueAt(selectedRow, 6).equals("Active");
                toggleStaffStatus(staffId, !isActive);
                loadStaffData(model);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a staff member", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        resetPasswordButton.addActionListener(e -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow >= 0) {
                int staffId = (int) staffTable.getValueAt(selectedRow, 0);
                resetStaffPassword(staffId);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a staff member", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
   
    
    private void loadStaffData(DefaultTableModel model) {
        model.setRowCount(0); 
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.staff_id, s.username, s.full_name, s.email, u.unit_name, r.role_name, " +
                         "CASE WHEN s.is_active THEN 'Active' ELSE 'Inactive' END AS status " +
                         "FROM staff s " +
                         "JOIN units u ON s.unit_id = u.unit_id " +
                         "JOIN roles r ON s.role_id = r.role_id " +
                         "ORDER BY s.staff_id";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("unit_name"),
                    rs.getString("role_name"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading staff data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void toggleStaffStatus(int staffId, boolean activate) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE staff SET is_active = ? WHERE staff_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, activate);
            stmt.setInt(2, staffId);
            stmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Staff status updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating staff status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resetStaffPassword(int staffId) {
    
    String newPassword = generateSimplePassword(); 
    
    try {
        
        String hashedPassword = PasswordUtils.hashPassword(newPassword);

        try (Connection conn = DBConnection.getConnection()) {
            
            String updateSql = "UPDATE staff SET password_hash = ? WHERE staff_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, hashedPassword);
                updateStmt.setInt(2, staffId);
                int updated = updateStmt.executeUpdate();

                if (updated == 0) {
                    JOptionPane.showMessageDialog(this, 
                        "No staff member found with ID: " + staffId,
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            
            String selectSql = "SELECT email FROM staff WHERE staff_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, staffId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        EmailUtils.sendEmail(
                            email,
                            "Password Reset Notification",
                            "Your new password is: " + newPassword + "\n\nPlease change after login."
                        );
                        
                        JOptionPane.showMessageDialog(this, 
                            "Password reset successful. Sent to staff email.",
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Staff found but no email on file.",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }
}


private String generateSimplePassword() {
    return UUID.randomUUID().toString().substring(0, 8); 
}




class EditStaffDialog extends JDialog {
    private final int staffId;
    private final JTextField fullNameField, dobField, phoneField, emailField, usernameField;
    private final JComboBox<String> genderCombo, unitCombo, roleCombo;
    private final JButton updateButton, cancelButton, changePhotoButton;
    private final JLabel photoLabel;
    private byte[] passportPhoto;
    
    public EditStaffDialog(JFrame parent, int staffId) {
        super(parent, "Edit Staff", true);
        this.staffId = staffId;
        setSize(600, 500);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        
        fullNameField = new JTextField(20);
        dobField = new JTextField(20);
        phoneField = new JTextField(20);
        emailField = new JTextField(20);
        usernameField = new JTextField(20);
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        unitCombo = new JComboBox<>();
        roleCombo = new JComboBox<>();
        changePhotoButton = new JButton("Change Photo");
        photoLabel = new JLabel();
        updateButton = new JButton("Update");
        cancelButton = new JButton("Cancel");
        
        
        loadUnits(unitCombo);
        loadRoles(roleCombo);
        
        
        addFormRow(panel, gbc, 0, "Full Name:", fullNameField);
        addFormRow(panel, gbc, 1, "Date of Birth (YYYY-MM-DD):", dobField);
        addFormRow(panel, gbc, 2, "Phone Number:", phoneField);
        addFormRow(panel, gbc, 3, "Email:", emailField);
        addFormRow(panel, gbc, 4, "Username:", usernameField);
        addFormRow(panel, gbc, 5, "Gender:", genderCombo);
        addFormRow(panel, gbc, 6, "Unit:", unitCombo);
        addFormRow(panel, gbc, 7, "Role:", roleCombo);
        
        
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("Passport Photo:"), gbc);
        
        gbc.gridx = 1;
        changePhotoButton.addActionListener(e -> changePhoto());
        panel.add(changePhotoButton, gbc);
        
        
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        updateButton.addActionListener(e -> updateStaff());
        panel.add(updateButton, gbc);
        
        gbc.gridy = 10;
        cancelButton.addActionListener(e -> dispose());
        panel.add(cancelButton, gbc);
        
        add(panel);
        
        
        loadStaffData();
    }
    
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1;
        panel.add(field, gbc);
    }
    
    private void loadStaffData() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.*, u.unit_name, r.role_name FROM staff s " +
                         "JOIN units u ON s.unit_id = u.unit_id " +
                         "JOIN roles r ON s.role_id = r.role_id " +
                         "WHERE s.staff_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                fullNameField.setText(rs.getString("full_name"));
                dobField.setText(rs.getString("date_of_birth"));
                phoneField.setText(rs.getString("phone_number"));
                emailField.setText(rs.getString("email"));
                usernameField.setText(rs.getString("username"));
                genderCombo.setSelectedItem(rs.getString("gender"));
                unitCombo.setSelectedItem(rs.getString("unit_name"));
                roleCombo.setSelectedItem(rs.getString("role_name"));
                
                passportPhoto = rs.getBytes("passport_photo");
                if (passportPhoto != null) {
                    ImageIcon icon = new ImageIcon(passportPhoto);
                    photoLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading staff data: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void changePhoto() {
        WebcamCaptureDialog webcamDialog = new WebcamCaptureDialog(this);
        webcamDialog.setVisible(true);
        byte[] newPhoto = webcamDialog.getCapturedImage();
        if (newPhoto != null) {
            passportPhoto = newPhoto;
            ImageIcon icon = new ImageIcon(passportPhoto);
            photoLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
        }
    }
    
    private void updateStaff() {
        
        if (fullNameField.getText().isEmpty() || dobField.getText().isEmpty() || 
            phoneField.getText().isEmpty() || emailField.getText().isEmpty() || 
            usernameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all required fields", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE staff SET " +
                         "full_name = ?, date_of_birth = ?, phone_number = ?, " +
                         "email = ?, username = ?, gender = ?, " +
                         "unit_id = ?, role_id = ?, " +
                         "passport_photo = ? " +
                         "WHERE staff_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullNameField.getText());
            stmt.setString(2, dobField.getText());
            stmt.setString(3, phoneField.getText());
            stmt.setString(4, emailField.getText());
            stmt.setString(5, usernameField.getText());
            stmt.setString(6, (String) genderCombo.getSelectedItem());
            stmt.setInt(7, getUnitId((String) unitCombo.getSelectedItem()));
            stmt.setInt(8, getRoleId((String) roleCombo.getSelectedItem()));
            stmt.setBytes(9, passportPhoto);
            stmt.setInt(10, staffId);
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this, 
                    "Staff updated successfully", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error updating staff: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadUnits(JComboBox<String> comboBox) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT unit_name FROM units";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                comboBox.addItem(rs.getString("unit_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading units: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadRoles(JComboBox<String> comboBox) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT role_name FROM roles";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                comboBox.addItem(rs.getString("role_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading roles: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int getUnitId(String unitName) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT unit_id FROM units WHERE unit_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, unitName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("unit_id") : -1;
        }
    }
    
    private int getRoleId(String roleName) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT role_id FROM roles WHERE role_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, roleName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("role_id") : -1;
        }
    }
}




class AddStaffDialog extends JDialog {

    private final JTextField fullNameField;
    private final JTextField dobField;
    private final JTextField phoneField;
    private final JTextField emailField;
    private final JTextField usernameField;
    private final JComboBox<String> genderCombo;
    private final JComboBox<String> unitCombo;
    private final JComboBox<String> roleCombo;
    private final JButton capturePhotoButton;
    private final JButton fingerprintButton;
    private final JButton saveButton;
    private final JButton cancelButton;
    private byte[] passportPhoto = null; 
    private byte[] fingerprintTemplate = null;
    private JLabel fingerprintStatusLabel = null;
    private JLabel fingerprintImageLabel = null;
    private FingerprintUtils fingerprintUtils;
    
    public AddStaffDialog(JFrame parent) {
        super(parent, "Add New Staff", true);
        setSize(600, 500);
        setLocationRelativeTo(parent);
        
         fingerprintUtils = new FingerprintUtils();
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Full Name:"), gbc);
        
        gbc.gridx = 1;
        fullNameField = new JTextField(20);
        panel.add(fullNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Date of Birth (YYYY-MM-DD):"), gbc);
        
        gbc.gridx = 1;
        dobField = new JTextField(20);
        panel.add(dobField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Phone Number:"), gbc);
        
        gbc.gridx = 1;
        phoneField = new JTextField(20);
        panel.add(phoneField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        emailField = new JTextField(20);
        panel.add(emailField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        panel.add(usernameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("Gender:"), gbc);
        
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        panel.add(genderCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(new JLabel("Unit:"), gbc);
        
        gbc.gridx = 1;
        unitCombo = new JComboBox<>();
        loadUnits(unitCombo);
        panel.add(unitCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("Role:"), gbc);
        
        gbc.gridx = 1;
        roleCombo = new JComboBox<>();
        loadRoles(roleCombo);
        panel.add(roleCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 8;
        capturePhotoButton = new JButton("Capture Passport Photo");
        panel.add(capturePhotoButton, gbc);
        
        gbc.gridx = 1;
        fingerprintButton = new JButton("Capture Fingerprint");
        panel.add(fingerprintButton, gbc);
        
        gbc.gridx = 5;
        gbc.gridy = 12;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        saveButton = new JButton("Save");
        panel.add(saveButton, gbc);
        
         
        gbc.gridx = 0;
        gbc.gridy = 12;
        cancelButton = new JButton("Cancel");
        panel.add(cancelButton, gbc);  
        
        
       gbc.gridx = 3;
       gbc.gridy = 12;
       JButton clearFingerprintButton = new JButton("Clear");

     clearFingerprintButton.addActionListener(e -> {
     fingerprintTemplate = null;
     fingerprintImageLabel.setIcon(null);
     fingerprintStatusLabel.setText("Fingerprint not captured");
    });
    panel.add(clearFingerprintButton, gbc);       
        
   gbc.gridx = 0;
   gbc.gridy = 10;
   gbc.gridwidth = 2;
   fingerprintStatusLabel = new JLabel("Fingerprint not captured", JLabel.CENTER);
   panel.add(fingerprintStatusLabel, gbc);
        
        
  gbc.gridy = 11;
  fingerprintImageLabel = new JLabel();
  fingerprintImageLabel.setPreferredSize(new Dimension(400, 500));
  fingerprintImageLabel.setBorder(BorderFactory.createEtchedBorder());
  fingerprintImageLabel.setHorizontalAlignment(JLabel.CENTER);
  panel.add(fingerprintImageLabel, gbc);
        
        add(panel);
        
       
        


    capturePhotoButton.addActionListener(e -> {
    WebcamCaptureDialog webcamDialog = new WebcamCaptureDialog(this);
    webcamDialog.setVisible(true);
    byte[] capturedImage = webcamDialog.getCapturedImage();
    if (capturedImage != null) {
     passportPhoto = capturedImage;
      JOptionPane.showMessageDialog(this,
            "Photo captured successfully!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    } else {
        JOptionPane.showMessageDialog(this, 
            "No photo was captured", 
            "Warning", 
            JOptionPane.WARNING_MESSAGE);
    }
});
   
        
      fingerprintButton.addActionListener((ActionEvent e) -> {
        fingerprintStatusLabel.setText("Initializing scanner...");
        fingerprintImageLabel.setIcon(null);
        fingerprintTemplate = null;
        
        
        fingerprintUtils.cleanup();
        
        
        SwingUtilities.invokeLater(() -> {
            try {
                fingerprintUtils.startCapture(fingerprintStatusLabel, fingerprintImageLabel);
                
                
                new Thread(() -> {
                    while (fingerprintUtils.getFingerprintTemplate() == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        fingerprintTemplate = fingerprintUtils.getFingerprintTemplate();
                        if (fingerprintTemplate != null) {
                            fingerprintStatusLabel.setText("Fingerprint ready!");
                            JOptionPane.showMessageDialog(this,
                                "Fingerprint captured successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                }).start();
                
            } catch (Exception ex) {
                fingerprintStatusLabel.setText("Scanner error");
                JOptionPane.showMessageDialog(this,
                    "Failed to initialize fingerprint reader:\n" + ex.getMessage(),
                    "Hardware Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    });
   
   
   
saveButton.addActionListener(e -> {
        
        if (fullNameField.getText().trim().isEmpty() || 
            dobField.getText().trim().isEmpty() ||
            phoneField.getText().trim().isEmpty() || 
            emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill all required fields", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (passportPhoto == null) {
            JOptionPane.showMessageDialog(this, 
                "Please capture passport photo", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

       
        if (fingerprintTemplate == null || fingerprintTemplate.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please capture a valid fingerprint before saving", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveStaff();
    });

    

   
   
   
   
    } 
    
    
    private void loadUnits(JComboBox<String> comboBox) {
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
    
    private void loadRoles(JComboBox<String> comboBox) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT role_name FROM roles WHERE role_name NOT IN ('IT Administrator', 'Director of Services', 'Head of IT Department')";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                comboBox.addItem(rs.getString("role_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading roles: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
     private void saveStaff() {
    Connection conn = null;
    PreparedStatement insertStmt = null;
    PreparedStatement updateStmt = null;
    ResultSet generatedKeys = null;
    
    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); 

       
        String newPassword = generateSimplePassword();
        String hashedPassword = PasswordUtils.hashPassword(newPassword);

        
        String insertSql = "INSERT INTO staff (username, password_hash, full_name, date_of_birth, " +
                         "phone_number, email, gender, passport_photo, fingerprint_template, " +
                         "unit_id, role_id, is_active) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)";
        
        insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
        insertStmt.setString(1, usernameField.getText());
        insertStmt.setString(2, hashedPassword); 
        insertStmt.setString(3, fullNameField.getText());
        insertStmt.setString(4, dobField.getText());
        insertStmt.setString(5, phoneField.getText());
        insertStmt.setString(6, emailField.getText());
        insertStmt.setString(7, (String) genderCombo.getSelectedItem());
        insertStmt.setBytes(8, passportPhoto);
        insertStmt.setBytes(9, fingerprintTemplate);
        insertStmt.setInt(10, getUnitId((String) unitCombo.getSelectedItem()));
        insertStmt.setInt(11, getRoleId((String) roleCombo.getSelectedItem()));
        
        int affectedRows = insertStmt.executeUpdate();
        
        if (affectedRows == 0) {
            throw new SQLException("Creating staff failed, no rows affected.");
        }
        
        
        generatedKeys = insertStmt.getGeneratedKeys();
        if (!generatedKeys.next()) {
            throw new SQLException("Creating staff failed, no ID obtained.");
        }
        
        
        EmailUtils.sendEmail(
            emailField.getText(),
            "Account Created - Password Reset",
            """
            Your account has been created.
            
            Username: """ + usernameField.getText() + "\n" +
            "Temporary Password: " + newPassword + "\n\n" +
            "Please change your password after logging in."
        );
        
        conn.commit(); 
        
        JOptionPane.showMessageDialog(this,
            "Staff added successfully. Login credentials sent to email.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
        
    } catch (SQLException ex) {
        try {
            if (conn != null) {
                conn.rollback(); 
            }
        } catch (SQLException ex2) {
            ex2.printStackTrace();
        }
        
        JOptionPane.showMessageDialog(this,
            "Error saving staff: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        
    } finally {
        
        try {
            if (generatedKeys != null) generatedKeys.close();
            if (updateStmt != null) updateStmt.close();
            if (insertStmt != null) insertStmt.close();
            if (conn != null) {
                conn.setAutoCommit(true); 
                conn.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

    
    private int getUnitId(String unitName) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT unit_id FROM units WHERE unit_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, unitName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("unit_id");
            }
        }
        return -1;
    }
    
    private int getRoleId(String roleName) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT role_id FROM roles WHERE role_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, roleName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("role_id");
            }
        }
  
        return -1;
}   }}

 

    
    
    
    
    
    
    
    
    
    
    
    


