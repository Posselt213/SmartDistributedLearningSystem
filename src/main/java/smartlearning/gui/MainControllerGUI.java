/**
 *
 * @author comin
 */
package smartlearning.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.ServiceList;
import smartlearning.naming.ServiceInfo;
import smartlearning.naming.Empty;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import smartlearning.monitoring.AttendanceRequest;
import smartlearning.monitoring.AttendanceResponse;
import smartlearning.monitoring.ProgressResponse;
import smartlearning.monitoring.ScoreRequest;
import smartlearning.monitoring.ScoreResponse;
import smartlearning.monitoring.StudentMonitoringServiceGrpc;
import smartlearning.monitoring.StudentRequest;
import smartlearning.notification.AlertRequest;
import smartlearning.notification.AlertResponse;
import smartlearning.notification.Notification;
import smartlearning.notification.NotificationList;
import smartlearning.notification.NotificationServiceGrpc;
import smartlearning.notification.Priority;
import smartlearning.naming.ServiceQuery;

/**
 * This class is the main GUI controller for the project. INFO
 * https://docs.oracle.com/javase/8/docs/api/javax/swing/package-summary.html -
 * the main window - a top panel with one button - a large text area to display
 * output
 *
 */
public class MainControllerGUI extends JFrame {

    // Button
    private JButton discoverServicesButton;

    // Text to display responses, logs, and errors
    private JTextArea outputArea;

    /**
     * Constructor.
     *
     * This sets up the main window and all visual components.
     */
    public MainControllerGUI() {
        initializeGUI();

    }

    // Student Monitoring input fields
    private JTextField studentIdField;
    private JTextField dateField;
    private JCheckBox presentCheckBox;
    private JTextField assessmentIdField;
    private JTextField scoreField;
    private JTextField maxScoreField;
    // Notification inputs
    private JTextField notificationUserIdField;
    private JTextField notificationMessageField;

    // Student Monitoring buttons
    private JButton recordAttendanceButton;
    private JButton submitScoreButton;
    private JButton getProgressButton;
    // Notification buttons
    private JButton sendAlertButton;
    private JButton getNotificationsButton;

    /**
     * This method creates the window layout and components.
     */
    private void initializeGUI() {

        //  Basic window settings
        setTitle("Smart Distributed Learning Support System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centers the window on screen
        setLayout(new BorderLayout());

        // Top panel for buttons
        JPanel topPanel = new JPanel();

        //Right panel for Notification controls
        JPanel notificationPanel = new JPanel();
        notificationPanel.setLayout(new GridLayout(0, 2, 5, 5));

        // Buttons
        discoverServicesButton = new JButton("Discover Services");
        recordAttendanceButton = new JButton("Record Attendance");
        submitScoreButton = new JButton("Submit Score");
        getProgressButton = new JButton("Get Progress");
        // Buttons Notification
        sendAlertButton = new JButton("Send Alert");
        getNotificationsButton = new JButton("Get Notifications");

        // When the button is clicked, call discoverServices()
        discoverServicesButton.addActionListener(e -> discoverServices());
        recordAttendanceButton.addActionListener(e -> recordAttendance());
        submitScoreButton.addActionListener(e -> submitScore());
        getProgressButton.addActionListener(e -> getStudentProgress());
        sendAlertButton.addActionListener(e -> sendAlert());
        getNotificationsButton.addActionListener(e -> getNotifications());

        // Add button to top panel
        topPanel.add(discoverServicesButton);

        // Add top panel to top area of window
        add(topPanel, BorderLayout.NORTH);

        // Left panel for Student Monitoring controls
        JPanel monitoringPanel = new JPanel();
        monitoringPanel.setLayout(new GridLayout(0, 2, 5, 5));

        // Input fields
        studentIdField = new JTextField();
        dateField = new JTextField();
        presentCheckBox = new JCheckBox("Present");
        assessmentIdField = new JTextField();
        scoreField = new JTextField();
        maxScoreField = new JTextField();
        // Inputs Notifications
        notificationUserIdField = new JTextField();
        notificationMessageField = new JTextField();

        // Add labels and fields
        monitoringPanel.add(new JLabel("Student ID:"));
        monitoringPanel.add(studentIdField);

        monitoringPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        monitoringPanel.add(dateField);

        monitoringPanel.add(new JLabel("Attendance:"));
        monitoringPanel.add(presentCheckBox);

        monitoringPanel.add(new JLabel("Assessment ID:"));
        monitoringPanel.add(assessmentIdField);

        monitoringPanel.add(new JLabel("Score:"));
        monitoringPanel.add(scoreField);

        monitoringPanel.add(new JLabel("Max Score:"));
        monitoringPanel.add(maxScoreField);

        // Add buttons
        monitoringPanel.add(recordAttendanceButton);
        monitoringPanel.add(submitScoreButton);
        monitoringPanel.add(getProgressButton);

        // Add components
        notificationPanel.add(new JLabel("User ID:"));
        notificationPanel.add(notificationUserIdField);

        notificationPanel.add(new JLabel("Message:"));
        notificationPanel.add(notificationMessageField);

        notificationPanel.add(sendAlertButton);
        notificationPanel.add(getNotificationsButton);

        // Add panel to the RIGHT side
        add(notificationPanel, BorderLayout.EAST);

        // Add panel to the left side of the window
        add(monitoringPanel, BorderLayout.WEST);

        // Output area in the center
        outputArea = new JTextArea();
        outputArea.setEditable(false); // user should not type here
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Put text area inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Add scroll pane to center of window
        add(scrollPane, BorderLayout.CENTER);

        // Initial message for the user
        outputArea.append("GUI started successfully.\n");
        outputArea.append("Click 'Discover Services' when the Naming Service is running.\n");
    }

    /**
     * This method connects to the Naming Service and retrieves all registered
     * services. It then displays them in the output area.
     */
    private void discoverServices() {

        ManagedChannel channel = null;

        try {
            outputArea.append("\nConnecting to Naming Service...\n");

            // Connect to Naming Service
            channel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub stub
                    = NamingServiceGrpc.newBlockingStub(channel);

            // Call ListServices RPC
            ServiceList serviceList = stub.listServices(Empty.newBuilder().build());

            outputArea.append("Services discovered:\n");

            // Loop through all services and print them
            for (ServiceInfo service : serviceList.getServicesList()) {

                outputArea.append("-----------------------------------\n");
                outputArea.append("Name: " + service.getName() + "\n");
                outputArea.append("Host: " + service.getHost() + "\n");
                outputArea.append("Port: " + service.getPort() + "\n");

                outputArea.append("Capabilities:\n");

                for (String capability : service.getCapabilitiesList()) {
                    outputArea.append(" - " + capability + "\n");
                }
            }

            outputArea.append("\nDiscovery completed successfully.\n");

        } catch (Exception e) {
            outputArea.append("Error discovering services: " + e.getMessage() + "\n");

        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

    /**
     * This method sends a RecordAttendance request to StudentMonitoringService.
     */
    private void recordAttendance() {

        ManagedChannel namingChannel = null;
        ManagedChannel monitoringChannel = null;

        try {
            outputArea.append("\nRecording attendance...\n");

            // First discover StudentMonitoringService from Naming Service
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub
                    = NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    smartlearning.naming.ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService using discovered host and port
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub
                    = StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

            AttendanceResponse response = stub.recordAttendance(
                    AttendanceRequest.newBuilder()
                            .setStudentId(studentIdField.getText().trim())
                            .setDate(dateField.getText().trim())
                            .setPresent(presentCheckBox.isSelected())
                            .build()
            );

            outputArea.append("Attendance recorded successfully.\n");
            outputArea.append("Attendance Rate: " + response.getAttendanceRate() + "\n");

        } catch (Exception e) {
            outputArea.append("Error recording attendance: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (monitoringChannel != null) {
                monitoringChannel.shutdown();
            }
        }
    }

    /**
     * This method sends a SubmitScore request to StudentMonitoringService.
     */
    private void submitScore() {

        ManagedChannel namingChannel = null;
        ManagedChannel monitoringChannel = null;

        try {
            outputArea.append("\nSubmitting score...\n");

            // Discover StudentMonitoringService
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub
                    = NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    smartlearning.naming.ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub
                    = StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

            ScoreResponse response = stub.submitScore(
                    ScoreRequest.newBuilder()
                            .setStudentId(studentIdField.getText().trim())
                            .setAssessmentId(assessmentIdField.getText().trim())
                            .setScore(Double.parseDouble(scoreField.getText().trim()))
                            .setMaxScore(Double.parseDouble(maxScoreField.getText().trim()))
                            .build()
            );

            outputArea.append("Score submitted successfully.\n");
            outputArea.append("New Average: " + response.getNewAverage() + "\n");

        } catch (Exception e) {
            outputArea.append("Error submitting score: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (monitoringChannel != null) {
                monitoringChannel.shutdown();
            }
        }
    }

    /**
     * This method requests student progress from StudentMonitoringService.
     */
    private void getStudentProgress() {

        ManagedChannel namingChannel = null;
        ManagedChannel monitoringChannel = null;

        try {
            outputArea.append("\nGetting student progress...\n");

            // Discover StudentMonitoringService
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub
                    = NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    smartlearning.naming.ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub
                    = StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

            ProgressResponse response = stub.getStudentProgress(
                    StudentRequest.newBuilder()
                            .setStudentId(studentIdField.getText().trim())
                            .build()
            );

            outputArea.append("Student ID: " + response.getStudentId() + "\n");
            outputArea.append("Attendance Rate: " + response.getAttendanceRate() + "\n");
            outputArea.append("Average Score: " + response.getAverageScore() + "\n");
            outputArea.append("Risk Level: " + response.getRiskLevel() + "\n");
            outputArea.append("Message: " + response.getMessage() + "\n");

        } catch (Exception e) {
            outputArea.append("Error getting progress: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (monitoringChannel != null) {
                monitoringChannel.shutdown();
            }
        }
    }

    /**
     * Sends an alert using NotificationService.
     */
    private void sendAlert() {

        ManagedChannel namingChannel = null;
        ManagedChannel notificationChannel = null;

        try {
            outputArea.append("\nSending alert...\n");

            // Discover NotificationService
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub
                    = NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                       ServiceQuery.newBuilder()
                            .setName("NotificationService")
                            .build()
            );

            // Connect to NotificationService
            notificationChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            NotificationServiceGrpc.NotificationServiceBlockingStub stub
                    = NotificationServiceGrpc.newBlockingStub(notificationChannel);

            AlertResponse response = stub.sendAlert(
                    AlertRequest.newBuilder()
                            .setRecipientId(notificationUserIdField.getText().trim())
                            .setMessage(notificationMessageField.getText().trim())
                            .setPriority(Priority.HIGH)
                            .build()
            );

            outputArea.append("Alert sent successfully.\n");
            outputArea.append("Notification ID: " + response.getNotificationId() + "\n");

        } catch (Exception e) {
            outputArea.append("Error sending alert: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (notificationChannel != null) {
                notificationChannel.shutdown();
            }
        }
    }

    /**
     * Retrieves stored notifications for a user.
     */
    private void getNotifications() {

        ManagedChannel namingChannel = null;
        ManagedChannel notificationChannel = null;

        try {
            outputArea.append("\nGetting notifications...\n");

            // Discover NotificationService
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub
                    = NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("NotificationService")
                            .build()
            );

            // Connect to NotificationService
            notificationChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            NotificationServiceGrpc.NotificationServiceBlockingStub stub
                    = NotificationServiceGrpc.newBlockingStub(notificationChannel);

            NotificationList list = stub.getNotifications(
                    smartlearning.notification.UserRequest.newBuilder()
                            .setUserId(notificationUserIdField.getText().trim())
                            .build()
            );

            for (Notification n : list.getNotificationsList()) {
                outputArea.append("-----------------------------------\n");
                outputArea.append("ID: " + n.getNotificationId() + "\n");
                outputArea.append("Message: " + n.getMessage() + "\n");
                outputArea.append("Priority: " + n.getPriority() + "\n");
            }

        } catch (Exception e) {
            outputArea.append("Error getting notifications: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (notificationChannel != null) {
                notificationChannel.shutdown();
            }
        }
    }

    /**
     * Main method used to launch the GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainControllerGUI().setVisible(true);
        });
    }
}
