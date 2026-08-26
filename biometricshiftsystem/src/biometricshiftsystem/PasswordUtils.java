/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
/**
 *
 * @author natha
 */
public class PasswordUtils {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final HexFormat hexFormat = HexFormat.of();

    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hashBytes = digest.digest(password.getBytes());
        return hexFormat.formatHex(hashBytes); 
    }

    public static boolean verifyPassword(String password, String storedHash) throws NoSuchAlgorithmException {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(storedHash);
    }
}