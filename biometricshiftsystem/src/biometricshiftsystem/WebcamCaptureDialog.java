/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package biometricshiftsystem;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.opencv.highgui.VideoCapture;
/**
 *
 * @author natha
 */
public class WebcamCaptureDialog extends JDialog{
 static {
    
   System.load("C:\\Users\\natha\\OneDrive\\Documents\\300 LEVEL\\Open CV\\opencv_java249.dll");
    
  
}
    

    
    private byte[] capturedImage;
    private JLabel cameraLabel;
    private VideoCapture webcam;
    private boolean isRunning;
    
    public WebcamCaptureDialog(Window parent) {
        super(parent, "Capture Passport Photo", ModalityType.APPLICATION_MODAL);
        setSize(640, 480);
        setLocationRelativeTo(parent);
        
        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(JLabel.CENTER);
        add(cameraLabel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton captureButton = new JButton("Capture");
        JButton retryButton = new JButton("Retry");
        JButton saveButton = new JButton("Save");
        
        buttonPanel.add(captureButton);
        buttonPanel.add(retryButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        
        webcam = new VideoCapture(0);
        isRunning = true;
        
        
        new Thread(() -> {
            Mat frame = new Mat();
            MatOfByte mem = new MatOfByte();
            
            while (isRunning) {
                if (webcam.grab()) {
                    try {
                        webcam.retrieve(frame);
                        Highgui.imencode(".jpg", frame, mem);
                        ImageIcon icon = new ImageIcon(mem.toArray());
                        cameraLabel.setIcon(icon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            webcam.release();
        }).start();
        
        
        captureButton.addActionListener(e -> {
            if (webcam.grab()) {
                Mat frame = new Mat();
                webcam.retrieve(frame);
                
                
                
                MatOfByte mem = new MatOfByte();
                Highgui.imencode(".jpg", frame, mem);
                capturedImage = mem.toArray();
                
                
                ImageIcon icon = new ImageIcon(capturedImage);
                cameraLabel.setIcon(icon);
            }
        });
        
        retryButton.addActionListener(e -> {
            capturedImage = null;
            cameraLabel.setIcon(null);
        });
        
        saveButton.addActionListener(e -> {
            if (capturedImage != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Please capture an image first", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isRunning = false;
            }
        });
    }
    
    public byte[] getCapturedImage() {
        return capturedImage;
    }
}
