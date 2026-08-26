/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.security.NoSuchAlgorithmException;
/**
 *
 * @author natha
 */
public class Login extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    
    public Login() {
        setTitle("Biometric Shift System - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("PAN-ATLANTIC UNIVERSITY", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        JLabel subtitleLabel = new JLabel("Biometric Shift Management System", JLabel.CENTER);
        gbc.gridy = 1;
        panel.add(subtitleLabel, gbc);
        
        JLabel usernameLabel = new JLabel("Username:");
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(usernameLabel, gbc);
        
        usernameField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);
        
        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(passwordLabel, gbc);
        
        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);
        
        loginButton = new JButton("Login");
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);
        
      
        
        add(panel);
        
        
            loginButton.addActionListener((java.awt.event.ActionEvent evt) -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                
                
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "Please enter both username and password",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "SELECT s.*, r.role_name, u.unit_name FROM staff s " +
                            "JOIN roles r ON s.role_id = r.role_id " +
                            "JOIN units u ON s.unit_id = u.unit_id " +
                            "WHERE username = ?";
                    
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash").toLowerCase().replaceAll("\\s+", "");
            String inputHash = PasswordUtils.hashPassword(password);
                        
                        
                        System.out.println("Entered password hash: " + inputHash);
                        System.out.println("Stored hash: " + storedHash);
                        
                        if (PasswordUtils.verifyPassword(password, storedHash)) {
                            
                            String role = rs.getString("role_name");
                            String unit = rs.getString("unit_name");
                            int staffId = rs.getInt("staff_id");
                            
                            
                              openDashboard(role, unit, staffId);
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Invalid username or password",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            passwordField.setText("");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Invalid username or password",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                    }
                } catch (SQLException | NoSuchAlgorithmException e) {
                    JOptionPane.showMessageDialog(null,
                            "System error: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
        });
            

    }   
    private void openDashboard(String role, String unit, int staffId) {
        switch (role) {
            case "IT Administrator":
                new AdminDashboard(staffId).setVisible(true);
                break;
            case "Director of Services":
            case "Head of IT Department":
                new ManagementDashboard(staffId).setVisible(true);
                break;
            case "Supervisor":
                new SupervisorDashboard(staffId, unit).setVisible(true);
                break;
            case "Non-Supervisor":
                new StaffDashboard(staffId).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown role: " + role, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Login().setVisible(true);
        });
    }
}
