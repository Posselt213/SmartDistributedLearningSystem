/**
 *
 * @author comin
 */
package smartlearning.monitoring;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.ServiceInfo;
import smartlearning.naming.ServiceQuery;
import smartlearning.notification.AlertRequest;
import smartlearning.notification.AlertResponse;
import smartlearning.notification.NotificationServiceGrpc;
import smartlearning.notification.Priority;

/**
 * This class implements the StudentMonitoringService.
 *
 * Main responsibilities:
 * - record attendance
 * - store scores
 * - calculate progress
 * - determine risk level
 * - send an automatic alert when a student becomes HIGH risk
 */
public class StudentMonitoringServiceImpl extends StudentMonitoringServiceGrpc.StudentMonitoringServiceImplBase {

    // In-memory attendance storage
    // totalClassesMap = total number of classes recorded for each student
    // presentClassesMap = total number of attended classes for each student
    private final Map<String, Integer> totalClassesMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> presentClassesMap = new ConcurrentHashMap<>();


    // In-memory score storage
    // Each student ID is linked to a list of score percentages
    private final Map<String, List<Double>> scoreMap = new ConcurrentHashMap<>();

    /**
     * Unary RPC.
     *
     * Records attendance for one student and returns the updated attendance rate.
     */
    @Override
    public void recordAttendance(AttendanceRequest request, StreamObserver<AttendanceResponse> responseObserver) {

        // Validate input
        if (request.getStudentId().isBlank() || request.getDate().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID and date must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String studentId = request.getStudentId();

        // Increase total class count
        totalClassesMap.put(studentId, totalClassesMap.getOrDefault(studentId, 0) + 1);

        // Increase present count if student attended
        if (request.getPresent()) {
            presentClassesMap.put(studentId, presentClassesMap.getOrDefault(studentId, 0) + 1);
        }

        // Calculate updated attendance rate
        double attendanceRate = calculateAttendanceRate(studentId);

        AttendanceResponse response = AttendanceResponse.newBuilder()
                .setSuccess(true)
                .setAttendanceRate(attendanceRate)
                .setMessage("Attendance recorded successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Unary RPC.
     *
     * Stores a student assessment score and returns the updated average.
     */
    @Override
    public void submitScore(ScoreRequest request, StreamObserver<ScoreResponse> responseObserver) {

        // Validate input
        if (request.getStudentId().isBlank() || request.getAssessmentId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID and assessment ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        if (request.getScore() < 0 || request.getMaxScore() <= 0 || request.getScore() > request.getMaxScore()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Score values are invalid")
                            .asRuntimeException()
            );
            return;
        }

        String studentId = request.getStudentId();

        // Convert raw score into percentage between 0 and 1
        double percentage = request.getScore() / request.getMaxScore();

        // Store score
        scoreMap.putIfAbsent(studentId, new ArrayList<>());
        scoreMap.get(studentId).add(percentage);

        // Calculate updated average
        double newAverage = calculateAverageScore(studentId);

        ScoreResponse response = ScoreResponse.newBuilder()
                .setSuccess(true)
                .setNewAverage(newAverage)
                .setMessage("Score submitted successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Unary RPC.
     *
     * Returns the full student progress summary.
     * If the student is HIGH risk, an automatic alert is sent.
     */
    @Override
    public void getStudentProgress(StudentRequest request, StreamObserver<ProgressResponse> responseObserver) {

        // Validate input
        if (request.getStudentId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String studentId = request.getStudentId();

        // Check whether the student exists in memory
        boolean hasAttendance = totalClassesMap.containsKey(studentId);
        boolean hasScores = scoreMap.containsKey(studentId);

        if (!hasAttendance && !hasScores) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Student not found: " + studentId)
                            .asRuntimeException()
            );
            return;
        }

        // Calculate progress values
        double attendanceRate = calculateAttendanceRate(studentId);
        double averageScore = calculateAverageScore(studentId);
        RiskLevel riskLevel = determineRiskLevel(attendanceRate, averageScore);

        // If risk is HIGH, notify automatically
        if (riskLevel == RiskLevel.HIGH) {
            sendHighRiskNotification(studentId, attendanceRate, averageScore);
        }

        ProgressResponse response = ProgressResponse.newBuilder()
                .setStudentId(studentId)
                .setAttendanceRate(attendanceRate)
                .setAverageScore(averageScore)
                .setRiskLevel(riskLevel)
                .setMessage("Student progress retrieved successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Client-streaming RPC.
     *
     * Receives multiple score records from the client in one stream.
     * Valid entries are stored and invalid ones are counted as rejected.
     */
    @Override
    public StreamObserver<ScoreEvent> uploadScores(StreamObserver<UploadSummary> responseObserver) {

        return new StreamObserver<ScoreEvent>() {

            int receivedCount = 0;
            int acceptedCount = 0;
            int rejectedCount = 0;

            @Override
            public void onNext(ScoreEvent scoreEvent) {
                receivedCount++;

                // Validate each streamed score event
                if (scoreEvent.getStudentId().isBlank()
                        || scoreEvent.getAssessmentId().isBlank()
                        || scoreEvent.getScore() < 0
                        || scoreEvent.getMaxScore() <= 0
                        || scoreEvent.getScore() > scoreEvent.getMaxScore()) {
                    rejectedCount++;
                    return;
                }

                // Convert to percentage and store
                String studentId = scoreEvent.getStudentId();
                double percentage = scoreEvent.getScore() / scoreEvent.getMaxScore();

                scoreMap.putIfAbsent(studentId, new ArrayList<>());
                scoreMap.get(studentId).add(percentage);

                acceptedCount++;
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error in client streaming: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                UploadSummary response = UploadSummary.newBuilder()
                        .setReceivedCount(receivedCount)
                        .setAcceptedCount(acceptedCount)
                        .setRejectedCount(rejectedCount)
                        .setMessage("Score upload completed")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Helper method to calculate attendance rate.
     *
     * attendance rate = present classes / total classes
     */
    private double calculateAttendanceRate(String studentId) {
        int total = totalClassesMap.getOrDefault(studentId, 0);
        int present = presentClassesMap.getOrDefault(studentId, 0);

        if (total == 0) {
            return 0.0;
        }

        return (double) present / total;
    }

    /**
     * Helper method to calculate average score.
     *
     * Scores are stored as percentages between 0 and 1.
     */
    private double calculateAverageScore(String studentId) {
        List<Double> scores = scoreMap.getOrDefault(studentId, new ArrayList<>());

        if (scores.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (double score : scores) {
            sum += score;
        }

        return sum / scores.size();
    }

    /**
     * Helper method to determine risk level.
     */
    private RiskLevel determineRiskLevel(double attendanceRate, double averageScore) {
        if (attendanceRate < 0.70 || averageScore < 0.50) {
            return RiskLevel.HIGH;
        } else if (attendanceRate < 0.85 || averageScore < 0.70) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    /**
     * This method sends an automatic high-risk notification.
     *
     * 1. Discover NotificationService through Naming Service
     * 2. Connect to the discovered host and port
     * 3. Send the alert
     */
    private void sendHighRiskNotification(String studentId, double attendanceRate, double averageScore) {

        ManagedChannel namingChannel = null;
        ManagedChannel notificationChannel = null;

        try {
            // Connect to Naming Service
            namingChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                    NamingServiceGrpc.newBlockingStub(namingChannel);

            // Ask Naming Service where NotificationService is
            ServiceInfo notificationServiceInfo = namingStub.findService(
                    ServiceQuery.newBuilder()
                            .setName("NotificationService")
                            .build()
            );

            String notificationHost = notificationServiceInfo.getHost();
            int notificationPort = notificationServiceInfo.getPort();

            System.out.println("Discovered NotificationService at "
                    + notificationHost + ":" + notificationPort);

            // Connect to NotificationService using discovered address
            notificationChannel = ManagedChannelBuilder
                    .forAddress(notificationHost, notificationPort)
                    .usePlaintext()
                    .build();

            NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub =
                    NotificationServiceGrpc.newBlockingStub(notificationChannel);

            // Build automatic alert
            String alertMessage = "High risk detected for student " + studentId
                    + ". Attendance rate: " + attendanceRate
                    + ", Average score: " + averageScore;

            AlertRequest alertRequest = AlertRequest.newBuilder()
                    .setRecipientId(studentId)
                    .setMessage(alertMessage)
                    .setPriority(Priority.HIGH)
                    .build();

            //Send alert
            AlertResponse response = notificationStub.sendAlert(alertRequest);

            System.out.println("Automatic notification sent: " + response.getMessage());

        } catch (Exception e) {
            // We log the error, but we do not fail the main StudentMonitoring 
            System.err.println("Failed to send automatic high-risk notification: " + e.getMessage());

        } finally {
            // Close both channels safely
            if (namingChannel != null) {
                namingChannel.shutdown();
            }

            if (notificationChannel != null) {
                notificationChannel.shutdown();
            }
        }
    }
}