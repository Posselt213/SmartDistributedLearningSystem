
/**
 *
 * @author comin
 */
package smartlearning.monitoring;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StudentMonitoringClientTest {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        StudentMonitoringServiceGrpc.StudentMonitoringServiceBlockingStub blockingStub =
                StudentMonitoringServiceGrpc.newBlockingStub(channel);

        StudentMonitoringServiceGrpc.StudentMonitoringServiceStub asyncStub =
                StudentMonitoringServiceGrpc.newStub(channel);

        try {
            // 1. Record attendance
            AttendanceResponse attendanceResponse = blockingStub.recordAttendance(
                    AttendanceRequest.newBuilder()
                            .setStudentId("S1001")
                            .setDate("2026-03-31")
                            .setPresent(false)
                            .build()
            );

            System.out.println("Attendance Response:");
            System.out.println("Success: " + attendanceResponse.getSuccess());
            System.out.println("Attendance Rate: " + attendanceResponse.getAttendanceRate());
            System.out.println("Message: " + attendanceResponse.getMessage());

            // 2. Submit score
            ScoreResponse scoreResponse = blockingStub.submitScore(
                    ScoreRequest.newBuilder()
                            .setStudentId("S1001")
                            .setAssessmentId("Quiz1")
                            .setScore(0)
                            .setMaxScore(50)
                            .build()
            );

            System.out.println("\nScore Response:");
            System.out.println("Success: " + scoreResponse.getSuccess());
            System.out.println("New Average: " + scoreResponse.getNewAverage());
            System.out.println("Message: " + scoreResponse.getMessage());

            // 3. Get progress
            ProgressResponse progressResponse = blockingStub.getStudentProgress(
                    StudentRequest.newBuilder()
                            .setStudentId("S1001")
                            .build()
            );

            System.out.println("\nProgress Response:");
            System.out.println("Student ID: " + progressResponse.getStudentId());
            System.out.println("Attendance Rate: " + progressResponse.getAttendanceRate());
            System.out.println("Average Score: " + progressResponse.getAverageScore());
            System.out.println("Risk Level: " + progressResponse.getRiskLevel());
            System.out.println("Message: " + progressResponse.getMessage());

            // 4. Client streaming test
            CountDownLatch latch = new CountDownLatch(1);

            StreamObserver<UploadSummary> responseObserver = new StreamObserver<UploadSummary>() {
                @Override
                public void onNext(UploadSummary summary) {
                    System.out.println("\nUpload Summary:");
                    System.out.println("Received: " + summary.getReceivedCount());
                    System.out.println("Accepted: " + summary.getAcceptedCount());
                    System.out.println("Rejected: " + summary.getRejectedCount());
                    System.out.println("Message: " + summary.getMessage());
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Streaming Error: " + throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Client streaming completed.");
                    latch.countDown();
                }
            };

            StreamObserver<ScoreEvent> requestObserver = asyncStub.uploadScores(responseObserver);

            requestObserver.onNext(ScoreEvent.newBuilder()
                    .setStudentId("S1001")
                    .setAssessmentId("Quiz2")
                    .setScore(0)
                    .setMaxScore(50)
                    .build());

            requestObserver.onNext(ScoreEvent.newBuilder()
                    .setStudentId("S1001")
                    .setAssessmentId("Quiz3")
                    .setScore(0)
                    .setMaxScore(50)
                    .build());

            requestObserver.onNext(ScoreEvent.newBuilder()
                    .setStudentId("S1001")
                    .setAssessmentId("Quiz4")
                    .setScore(0)   // invalid on purpose
                    .setMaxScore(50)
                    .build());

            requestObserver.onCompleted();

            latch.await(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
