/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package biometricshiftsystem;
import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.*;
import com.digitalpersona.onetouch.capture.event.*;
import com.digitalpersona.onetouch.processing.*;
import static com.digitalpersona.onetouch.processing.DPFPTemplateStatus.TEMPLATE_STATUS_READY;
import com.digitalpersona.onetouch.verification.*;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
/**
 *
 * @author natha
 */




public class FingerprintUtils {
    private DPFPCapture capture;
    DPFPEnrollment enrollment;
    DPFPVerification verifier;
    boolean isCapturing = false;
     private Runnable attendanceCallback;
    
    public FingerprintUtils() {
        initialize();
    }
    
    
        public void setAttendanceCallback(Runnable callback) {
        this.attendanceCallback = callback;
    }
        
    private void initialize() {
        capture = DPFPGlobal.getCaptureFactory().createCapture();
        enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();
        verifier = DPFPGlobal.getVerificationFactory().createVerification();
    }
    
    
    public synchronized void startCapture(JLabel statusLabel, JLabel imageLabel) {
        if (isCapturing) {
            stopCapture();
        }
        
        try {
            capture.addDataListener(new DPFPDataAdapter() {
                @Override
                public void dataAcquired(DPFPDataEvent e) {
                    processFingerprint(e.getSample(), statusLabel, imageLabel);
                }
            });
            
            capture.addSensorListener(new DPFPSensorAdapter() {
                @Override
                public void fingerTouched(DPFPSensorEvent e) {
                    statusLabel.setText("Reading fingerprint...");
                }
                
                @Override
                public void fingerGone(DPFPSensorEvent e) {
                    statusLabel.setText("Place finger on scanner");
                }
            });
            
            capture.startCapture();
            isCapturing = true;
            statusLabel.setText("Ready to scan fingerprint");
            
        } catch (Exception e) {
    statusLabel.setText("Error: " + e.getMessage());
    e.printStackTrace();
}

    }
    
    public synchronized void stopCapture() {
        if (isCapturing && capture != null) {
            try {
                capture.stopCapture();
            } catch (IllegalStateException e) {
                // Already stopped
            } finally {
                isCapturing = false;
            }
        }
    }
    
    
      private void processFingerprint(DPFPSample sample, JLabel statusLabel, JLabel imageLabel) {
        SwingUtilities.invokeLater(() -> {
            try {
                
                Image image = DPFPGlobal.getSampleConversionFactory().createImage(sample);
                imageLabel.setIcon(new ImageIcon(image.getScaledInstance(
                    imageLabel.getWidth(), 
                    imageLabel.getHeight(), 
                    Image.SCALE_SMOOTH)));
                
                
                DPFPFeatureSet features = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                if (features == null) {
                    statusLabel.setText("Poor quality, try again");
                    return;
                }
                
                enrollment.addFeatures(features);
                
                switch (enrollment.getTemplateStatus()) {
                    case TEMPLATE_STATUS_READY:
                        statusLabel.setText("Fingerprint captured!");
                        stopCapture();
                        if (attendanceCallback != null) {
                            attendanceCallback.run();
                        }
                        break;
                    case TEMPLATE_STATUS_FAILED:
                        enrollment.clear();
                        statusLabel.setText("Capture failed. Try again.");
                        break;
                    default:
                        statusLabel.setText("Scan again (" + 
                            enrollment.getFeaturesNeeded() + " more needed)");
                        break;
                }
            } catch (Exception e) {
                statusLabel.setText("Processing error");
                e.printStackTrace();
            }
        });
    } 
    
    
    
 
    
    public DPFPFeatureSet extractFeatures(DPFPSample sample, DPFPDataPurpose purpose) {
        try {
            return DPFPGlobal.getFeatureExtractionFactory()
                .createFeatureExtraction()
                .createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }
    
    public byte[] getFingerprintTemplate() {
        return (enrollment.getTemplateStatus() == TEMPLATE_STATUS_READY) 
            ? enrollment.getTemplate().serialize() 
            : null;
    }
    
    public void cleanup() {
        stopCapture();
        if (enrollment != null) {
            enrollment.clear();
        }
    }
}