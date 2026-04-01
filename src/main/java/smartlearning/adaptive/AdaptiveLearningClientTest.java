/**
 *
 * @author comin
 */
package smartlearning.adaptive;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to test the AdaptiveLearningService.
 *
 * Here I test:
 * 1. Unary RPC -> GetRecommendations
 * 2. Bidirectional streaming RPC -> TutorSession
 */
public class AdaptiveLearningClientTest {

    public static void main(String[] args) {

        // Create connection to the Adaptive Learning Service
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        // Blocking stub is used for normal unary calls
        AdaptiveLearningServiceGrpc.AdaptiveLearningServiceBlockingStub blockingStub =
                AdaptiveLearningServiceGrpc.newBlockingStub(channel);

        // Async stub is used for streaming calls
        AdaptiveLearningServiceGrpc.AdaptiveLearningServiceStub asyncStub =
                AdaptiveLearningServiceGrpc.newStub(channel);

        try {

            // TEST GetRecommendations (Unary RPC)
        
            // Here I send student progress data to the server
            // so it can decide the risk level, difficulty level,
            // and recommended materials.
            RecommendationResponse recommendationResponse = blockingStub.getRecommendations(
                    RecommendationRequest.newBuilder()
                            .setStudentId("S1001")
                            .setAttendanceRate(0.65)
                            .setAverageScore(0.55)
                            .setTopic("Fractions")
                            .build()
            );

            System.out.println("Recommendation Response:");
            System.out.println("Student ID: " + recommendationResponse.getStudentId());
            System.out.println("Risk Level: " + recommendationResponse.getRiskLevel());
            System.out.println("Difficulty Level: " + recommendationResponse.getDifficultyLevel());
            System.out.println("Message: " + recommendationResponse.getMessage());

            System.out.println("\nRecommended Materials:");
            for (LearningMaterial material : recommendationResponse.getMaterialsList()) {
                System.out.println("-----------------------------------");
                System.out.println("Title: " + material.getTitle());
                System.out.println("Type: " + material.getType());
                System.out.println("Difficulty: " + material.getDifficulty());
                System.out.println("Link: " + material.getLink());
            }

            // TEST TutorSession (Bidirectional Streaming RPC)
            
            // This latch is used to wait until the tutor session finishes
            CountDownLatch latch = new CountDownLatch(1);

            System.out.println("\nStarting Tutor Session...");

            // This observer receives messages from the server
            StreamObserver<TutorMessage> responseObserver = new StreamObserver<TutorMessage>() {

                @Override
                public void onNext(TutorMessage tutorMessage) {
                    System.out.println("Tutor Reply:");
                    System.out.println("Session ID: " + tutorMessage.getSessionId());
                    System.out.println("Student ID: " + tutorMessage.getStudentId());
                    System.out.println("Text: " + tutorMessage.getText());
                    System.out.println("Timestamp: " + tutorMessage.getTimestamp());
                    System.out.println();
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Tutor Session Error: " + throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Tutor session completed.");
                    latch.countDown();
                }
            };

            // This observer is used to send messages to the server
            StreamObserver<TutorMessage> requestObserver = asyncStub.tutorSession(responseObserver);

            // First student message
            requestObserver.onNext(TutorMessage.newBuilder()
                    .setSessionId("SESSION-001")
                    .setStudentId("S1001")
                    .setText("I need help with fractions")
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            // Second student message
            requestObserver.onNext(TutorMessage.newBuilder()
                    .setSessionId("SESSION-001")
                    .setStudentId("S1001")
                    .setText("I also need help with algebra")
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            // Third student message
            requestObserver.onNext(TutorMessage.newBuilder()
                    .setSessionId("SESION-001")
                    .setStudentId("S1001")
                    .setText("help")
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            // Tell the server that the client finished sending messages
            requestObserver.onCompleted();

            // Wait until the tutor session finishes
            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Always close the channel when finished
            channel.shutdown();
        }
    }
}