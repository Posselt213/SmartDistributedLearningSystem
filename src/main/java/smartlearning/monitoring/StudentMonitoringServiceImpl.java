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
import smartlearning.notification.AlertRequest;
import smartlearning.notification.AlertResponse;
import smartlearning.notification.NotificationServiceGrpc;
import smartlearning.notification.Priority;

/**
 * This class implements the StudentMonitoringService.
 *
 * Main responsibilities:
 * - record attendance
 * - store assessment scores
 * - calculate student progress
 * - determine risk level
 * - automatically notify a student when the risk level becomes HIGH
 */
public class StudentMonitoringServiceImpl extends StudentMonitoringServiceGrpc.StudentMonitoringServiceImplBase {

    //  In-memory storage for attendance
   
    // totalClassesMap stores how many classes each student had
    // presentClassesMap stores how many classes each student attended
    private final Map<String, Integer> totalClassesMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> presentClassesMap = new ConcurrentHashMap<>();

    // In-memory storage for scores
 
    // Each student ID is linked to a list of score percentages
    private final Map<String, List<Double>> scoreMap = new ConcurrentHashMap<>();

    /**
     * Unary RPC.
     *
     * This method records attendance for one student on one date.
     * It updates the attendance counters and returns the new attendance rate.
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

        // Update total number of classes
        totalClassesMap.put(studentId, totalClassesMap.getOrDefault(studentId, 0) + 1);

        // If the student was present, update attended classes
        if (request.getPresent()) {
            presentClassesMap.put(studentId, presentClassesMap.getOrDefault(studentId, 0) + 1);
        }

        //Calculate new attendance rate
        double attendanceRate = calculateAttendanceRate(studentId);

        //Return response
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
     * This method stores one score for one assessment.
     * The score is converted into a percentage before being stored.
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

        //  Convert score to percentage
        double percentage = request.getScore() / request.getMaxScore();

        // Store score
        scoreMap.putIfAbsent(studentId, new ArrayList<>());
        scoreMap.get(studentId).add(percentage);

        // Calculate updated average
        double newAverage = calculateAverageScore(studentId);

        // Return response
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
     * This method returns a summary of student progress:
     * - attendance rate
     * - average score
     * - risk level
     *
     * If the risk level is HIGH, the service automatically sends a notification
     * by calling NotificationService.
     */
    @Override
    public void getStudentProgress(StudentRequest request, StreamObserver<ProgressResponse> responseObserver) {

        //  Validate input
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

        // If student is HIGH risk, notify automatically
        if (riskLevel == RiskLevel.HIGH) {
            sendHighRiskNotification(studentId, attendanceRate, averageScore);
        }

        // -Build and return response
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
     * This method receives multiple score events from the client.
     * It validates each one, stores valid scores, and counts rejected entries.
     */
    @Override
    public StreamObserver<ScoreEvent> uploadScores(StreamObserver<UploadSummary> responseObserver) {

        return new StreamObserver<ScoreEvent>() {

            int receivedCount = 0;
            int acceptedCount = 0;
            int rejectedCount = 0;

            /**
             * Called every time the client sends a new score event.
             */
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

                // Convert to percentage and store it
                String studentId = scoreEvent.getStudentId();
                double percentage = scoreEvent.getScore() / scoreEvent.getMaxScore();

                scoreMap.putIfAbsent(studentId, new ArrayList<>());
                scoreMap.get(studentId).add(percentage);

                acceptedCount++;
            }

            /**
             * Called if the client stream fails.
             */
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error in client streaming: " + throwable.getMessage());
            }

            /**
             * Called when the client finishes sending all score events.
             */
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
     * Attendance rate = present classes / total classes
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
     * Helper method to determine student risk level.
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
     * This method connects to NotificationService and sends an automatic alert
     * when a student is considered HIGH risk.
     *
     * For now the notification is sent directly to the same student ID.
     * Later in the GUI/report you can explain that this could also be sent
     * to a teacher or support staff member.
     */
    private void sendHighRiskNotification(String studentId, double attendanceRate, double averageScore) {

        ManagedChannel channel = null;

        try {
            // Connect to NotificationService
            channel = ManagedChannelBuilder
                    .forAddress("localhost", 50053)
                    .usePlaintext()
                    .build();

            NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub =
                    NotificationServiceGrpc.newBlockingStub(channel);

            // Build alert message
            String alertMessage = "High risk detected for student " + studentId
                    + ". Attendance rate: " + attendanceRate
                    + ", Average score: " + averageScore;

            AlertRequest alertRequest = AlertRequest.newBuilder()
                    .setRecipientId(studentId)
                    .setMessage(alertMessage)
                    .setPriority(Priority.HIGH)
                    .build();

            // Send alert
            AlertResponse response = notificationStub.sendAlert(alertRequest);

            System.out.println("Automatic notification sent: " + response.getMessage());

        } catch (Exception e) {
            // If notification fails, we log the error but do not stop the main RPC
            System.err.println("Failed to send automatic high-risk notification: " + e.getMessage());

        } finally {
            // Close  channel
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}