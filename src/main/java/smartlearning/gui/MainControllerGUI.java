/**
 *
 * @author comin
 */
package smartlearning.gui;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import smartlearning.adaptive.AdaptiveLearningServiceGrpc;
import smartlearning.adaptive.LearningMaterial;
import smartlearning.adaptive.RecommendationRequest;
import smartlearning.adaptive.RecommendationResponse;
import smartlearning.monitoring.AttendanceRequest;
import smartlearning.monitoring.AttendanceResponse;
import smartlearning.monitoring.ProgressResponse;
import smartlearning.monitoring.ScoreRequest;
import smartlearning.monitoring.ScoreResponse;
import smartlearning.monitoring.StudentMonitoringServiceGrpc;
import smartlearning.monitoring.StudentRequest;
import smartlearning.naming.Empty;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.ServiceInfo;
import smartlearning.naming.ServiceList;
import smartlearning.naming.ServiceQuery;
import smartlearning.notification.AlertRequest;
import smartlearning.notification.AlertResponse;
import smartlearning.notification.Notification;
import smartlearning.notification.NotificationList;
import smartlearning.notification.NotificationServiceGrpc;
import smartlearning.notification.Priority;
import smartlearning.notification.UserRequest;

/**
 * This class is the main GUI controller for the project.
 *
 * Current GUI features:
 * - Discover registered services from Naming Service
 * - Student Monitoring controls
 * - Notification controls
 * - Adaptive Learning recommendation controls
 * - Output area for responses, logs and errors
 */
public class MainControllerGUI extends JFrame {

    // Top button
    private JButton discoverServicesButton;

    // Output area
    private JTextArea outputArea;

    //Student Monitoring input fields
    private JTextField studentIdField;
    private JTextField dateField;
    private JCheckBox presentCheckBox;
    private JTextField assessmentIdField;
    private JTextField scoreField;
    private JTextField maxScoreField;

    // Student Monitoring buttons
    private JButton recordAttendanceButton;
    private JButton submitScoreButton;
    private JButton getProgressButton;

    // Notification input fields
    private JTextField notificationUserIdField;
    private JTextField notificationMessageField;

    // Notification buttons
    private JButton sendAlertButton;
    private JButton getNotificationsButton;

    // Adaptive Learning input fields
    private JTextField adaptiveStudentIdField;
    private JTextField attendanceRateField;
    private JTextField averageScoreField;
    private JTextField topicField;

    // Adaptive Learning button
    private JButton getRecommendationsButton;

    /**
     * Constructor.
     */
    public MainControllerGUI() {
        initializeGUI();
    }

    /**
     * This method creates the full GUI layout and components.
     */
    private void initializeGUI() {

        // ---------------------------------------------------------
        // Basic window settings
        // ---------------------------------------------------------
        setTitle("Smart Distributed Learning Support System");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ---------------------------------------------------------
        // Top panel
        // ---------------------------------------------------------
        JPanel topPanel = new JPanel();

        discoverServicesButton = new JButton("Discover Services");
        topPanel.add(discoverServicesButton);

        add(topPanel, BorderLayout.NORTH);

        // Left panel for Student Monitoring controls
        JPanel monitoringPanel = new JPanel();
        monitoringPanel.setLayout(new GridLayout(0, 2, 5, 5));

        // Create Student Monitoring input fields
        studentIdField = new JTextField();
        dateField = new JTextField();
        presentCheckBox = new JCheckBox("Present");
        assessmentIdField = new JTextField();
        scoreField = new JTextField();
        maxScoreField = new JTextField();

        // Create Student Monitoring buttons
        recordAttendanceButton = new JButton("Record Attendance");
        submitScoreButton = new JButton("Submit Score");
        getProgressButton = new JButton("Get Progress");

        // Add Student Monitoring components
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

        monitoringPanel.add(recordAttendanceButton);
        monitoringPanel.add(submitScoreButton);
        monitoringPanel.add(getProgressButton);

        add(monitoringPanel, BorderLayout.WEST);

        // Right side container for Notification + Adaptive Learning
        JPanel rightContainer = new JPanel();
        rightContainer.setLayout(new GridLayout(2, 1, 5, 5));

        // Notification panel
        JPanel notificationPanel = new JPanel();
        notificationPanel.setLayout(new GridLayout(0, 2, 5, 5));

        notificationUserIdField = new JTextField();
        notificationMessageField = new JTextField();

        sendAlertButton = new JButton("Send Alert");
        getNotificationsButton = new JButton("Get Notifications");

        notificationPanel.add(new JLabel("User ID:"));
        notificationPanel.add(notificationUserIdField);

        notificationPanel.add(new JLabel("Message:"));
        notificationPanel.add(notificationMessageField);

        notificationPanel.add(sendAlertButton);
        notificationPanel.add(getNotificationsButton);

        //  Adaptive Learning panel
        JPanel adaptivePanel = new JPanel();
        adaptivePanel.setLayout(new GridLayout(0, 2, 5, 5));

        adaptiveStudentIdField = new JTextField();
        attendanceRateField = new JTextField();
        averageScoreField = new JTextField();
        topicField = new JTextField();

        getRecommendationsButton = new JButton("Get Recommendations");

        adaptivePanel.add(new JLabel("Student ID:"));
        adaptivePanel.add(adaptiveStudentIdField);

        adaptivePanel.add(new JLabel("Attendance Rate:"));
        adaptivePanel.add(attendanceRateField);

        adaptivePanel.add(new JLabel("Average Score:"));
        adaptivePanel.add(averageScoreField);

        adaptivePanel.add(new JLabel("Topic:"));
        adaptivePanel.add(topicField);

        adaptivePanel.add(getRecommendationsButton);

        // Add both panels to right side container
        rightContainer.add(notificationPanel);
        rightContainer.add(adaptivePanel);

        add(rightContainer, BorderLayout.EAST);

        // Output area in center
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Button actions
        discoverServicesButton.addActionListener(e -> discoverServices());
        recordAttendanceButton.addActionListener(e -> recordAttendance());
        submitScoreButton.addActionListener(e -> submitScore());
        getProgressButton.addActionListener(e -> getStudentProgress());
        sendAlertButton.addActionListener(e -> sendAlert());
        getNotificationsButton.addActionListener(e -> getNotifications());
        getRecommendationsButton.addActionListener(e -> getRecommendations());

        //  Initial output
        outputArea.append("GUI started successfully.\n");
        outputArea.append("Start Naming Service and the 3 core services before using the GUI.\n");
    }

    /**
     * This method connects to Naming Service and displays all registered services.
     */
    private void discoverServices() {

        ManagedChannel channel = null;

        try {
            outputArea.append("\nConnecting to Naming Service...\n");

            channel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub stub =
                    NamingServiceGrpc.newBlockingStub(channel);

            ServiceList serviceList = stub.listServices(Empty.newBuilder().build());

            outputArea.append("Services discovered:\n");

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
     * Sends a RecordAttendance request to StudentMonitoringService.
     */
    private void recordAttendance() {

        ManagedChannel namingChannel = null;
        ManagedChannel monitoringChannel = null;

        try {
            outputArea.append("\nRecording attendance...\n");

            // Discover StudentMonitoringService using Naming Service
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub =
                    StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

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
     * Sends a SubmitScore request to StudentMonitoringService.
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

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub =
                    StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

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
     * Requests full student progress from StudentMonitoringService.
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

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            // Connect to StudentMonitoringService
            monitoringChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub stub =
                    StudentMonitoringServiceGrpc.newBlockingStub(monitoringChannel);

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

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

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

            NotificationServiceGrpc.NotificationServiceBlockingStub stub =
                    NotificationServiceGrpc.newBlockingStub(notificationChannel);

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

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

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

            NotificationServiceGrpc.NotificationServiceBlockingStub stub =
                    NotificationServiceGrpc.newBlockingStub(notificationChannel);

            NotificationList list = stub.getNotifications(
                    UserRequest.newBuilder()
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
     * Calls AdaptiveLearningService to get recommendations based on
     * the values entered in the GUI.
     */
    private void getRecommendations() {

        ManagedChannel namingChannel = null;
        ManagedChannel adaptiveChannel = null;

        try {
            outputArea.append("\nGetting recommendations...\n");

            // Discover AdaptiveLearningService using Naming Service
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

            ServiceInfo serviceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("AdaptiveLearningService")
                            .build()
            );

            // Connect to AdaptiveLearningService
            adaptiveChannel = ManagedChannelBuilder
                    .forAddress(serviceInfo.getHost(), serviceInfo.getPort())
                    .usePlaintext()
                    .build();

            AdaptiveLearningServiceGrpc.AdaptiveLearningServiceBlockingStub stub =
                    AdaptiveLearningServiceGrpc.newBlockingStub(adaptiveChannel);

            RecommendationResponse response = stub.getRecommendations(
                    RecommendationRequest.newBuilder()
                            .setStudentId(adaptiveStudentIdField.getText().trim())
                            .setAttendanceRate(Double.parseDouble(attendanceRateField.getText().trim()))
                            .setAverageScore(Double.parseDouble(averageScoreField.getText().trim()))
                            .setTopic(topicField.getText().trim())
                            .build()
            );

            outputArea.append("Student ID: " + response.getStudentId() + "\n");
            outputArea.append("Risk Level: " + response.getRiskLevel() + "\n");
            outputArea.append("Difficulty Level: " + response.getDifficultyLevel() + "\n");
            outputArea.append("Message: " + response.getMessage() + "\n");
            outputArea.append("Recommended Materials:\n");

            for (LearningMaterial material : response.getMaterialsList()) {
                outputArea.append("-----------------------------------\n");
                outputArea.append("Title: " + material.getTitle() + "\n");
                outputArea.append("Type: " + material.getType() + "\n");
                outputArea.append("Difficulty: " + material.getDifficulty() + "\n");
                outputArea.append("Link: " + material.getLink() + "\n");
            }

        } catch (Exception e) {
            outputArea.append("Error getting recommendations: " + e.getMessage() + "\n");

        } finally {
            if (namingChannel != null) {
                namingChannel.shutdown();
            }
            if (adaptiveChannel != null) {
                adaptiveChannel.shutdown();
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