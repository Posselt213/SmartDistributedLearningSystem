
/**
 *
 * @author comin
 */
package smartlearning.monitoring;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StudentMonitoringServiceImpl extends StudentMonitoringServiceGrpc.StudentMonitoringServiceImplBase {

    // Attendance records
    private final Map<String, Integer> totalClassesMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> presentClassesMap = new ConcurrentHashMap<>();

    // Score records
    private final Map<String, List<Double>> scoreMap = new ConcurrentHashMap<>();

    @Override
    public void recordAttendance(AttendanceRequest request, StreamObserver<AttendanceResponse> responseObserver) {

        if (request.getStudentId().isBlank() || request.getDate().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID and date must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String studentId = request.getStudentId();

        totalClassesMap.put(studentId, totalClassesMap.getOrDefault(studentId, 0) + 1);

        if (request.getPresent()) {
            presentClassesMap.put(studentId, presentClassesMap.getOrDefault(studentId, 0) + 1);
        }

        double attendanceRate = calculateAttendanceRate(studentId);

        AttendanceResponse response = AttendanceResponse.newBuilder()
                .setSuccess(true)
                .setAttendanceRate(attendanceRate)
                .setMessage("Attendance recorded successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void submitScore(ScoreRequest request, StreamObserver<ScoreResponse> responseObserver) {

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

        double percentage = request.getScore() / request.getMaxScore();

        scoreMap.putIfAbsent(studentId, new ArrayList<>());
        scoreMap.get(studentId).add(percentage);

        double newAverage = calculateAverageScore(studentId);

        ScoreResponse response = ScoreResponse.newBuilder()
                .setSuccess(true)
                .setNewAverage(newAverage)
                .setMessage("Score submitted successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStudentProgress(StudentRequest request, StreamObserver<ProgressResponse> responseObserver) {

        if (request.getStudentId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String studentId = request.getStudentId();

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

        double attendanceRate = calculateAttendanceRate(studentId);
        double averageScore = calculateAverageScore(studentId);
        RiskLevel riskLevel = determineRiskLevel(attendanceRate, averageScore);

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

    @Override
    public StreamObserver<ScoreEvent> uploadScores(StreamObserver<UploadSummary> responseObserver) {

        return new StreamObserver<ScoreEvent>() {

            int receivedCount = 0;
            int acceptedCount = 0;
            int rejectedCount = 0;

            @Override
            public void onNext(ScoreEvent scoreEvent) {
                receivedCount++;

                if (scoreEvent.getStudentId().isBlank()
                        || scoreEvent.getAssessmentId().isBlank()
                        || scoreEvent.getScore() < 0
                        || scoreEvent.getMaxScore() <= 0
                        || scoreEvent.getScore() > scoreEvent.getMaxScore()) {
                    rejectedCount++;
                    return;
                }

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

    private double calculateAttendanceRate(String studentId) {
        int total = totalClassesMap.getOrDefault(studentId, 0);
        int present = presentClassesMap.getOrDefault(studentId, 0);

        if (total == 0) {
            return 0.0;
        }

        return (double) present / total;
    }

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

    private RiskLevel determineRiskLevel(double attendanceRate, double averageScore) {
        if (attendanceRate < 0.70 || averageScore < 0.50) {
            return RiskLevel.HIGH;
        } else if (attendanceRate < 0.85 || averageScore < 0.70) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
}
