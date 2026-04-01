/**
 *
 * @author comin
 */
package smartlearning.adaptive;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the AdaptiveLearningService.
 *
 * Main idea of this service:
 * - analyse student progress
 * - recommend a difficulty level
 * - return learning materials
 * - simulate a live tutoring conversation using bidirectional streaming
 */
public class AdaptiveLearningServiceImpl extends AdaptiveLearningServiceGrpc.AdaptiveLearningServiceImplBase {

    /**
     * Unary RPC.
     *
     * This method receives student progress data and returns:
     * - a risk level
     * - a recommended difficulty level
     * - a list of suggested learning materials
     */
    @Override
    public void getRecommendations(RecommendationRequest request,
            StreamObserver<RecommendationResponse> responseObserver) {

    
        // Validate the request
       
        // We check the most important fields before processing
        if (request.getStudentId().isBlank() || request.getTopic().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Student ID and topic must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        if (request.getAttendanceRate() < 0 || request.getAttendanceRate() > 1
                || request.getAverageScore() < 0 || request.getAverageScore() > 1) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Attendance rate and average score must be between 0 and 1")
                            .asRuntimeException()
            );
            return;
        }

        //Determine risk level
      
        RiskLevel riskLevel = determineRiskLevel(
                request.getAttendanceRate(),
                request.getAverageScore()
        );

        //  Determine recommended difficulty
      
        DifficultyLevel difficultyLevel = determineDifficultyLevel(
                request.getAttendanceRate(),
                request.getAverageScore()
        );

        //  Build recommended materials list
        
        List<LearningMaterial> materials = buildMaterials(
                request.getTopic(),
                difficultyLevel
        );

        //  Build and send response
        
        RecommendationResponse response = RecommendationResponse.newBuilder()
                .setStudentId(request.getStudentId())
                .setRiskLevel(riskLevel)
                .setDifficultyLevel(difficultyLevel)
                .addAllMaterials(materials)
                .setMessage("Recommendatons generated successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Bidirectional streaming RPC.
     *
     * This method simulates a live tutoring session.
     * The client sends messages and the server responds with guidance.
     *
     * Both sides can keep sending messages during the same session.
     */
    @Override
    public StreamObserver<TutorMessage> tutorSession(StreamObserver<TutorMessage> responseObserver) {

        // Return the observer that will receive messages from the client
        return new StreamObserver<TutorMessage>() {

            /**
             * Called every time the client sends a new tutoring message.
             */
            @Override
            public void onNext(TutorMessage request) {

                // Validate incoming message
                
                if (request.getStudentId().isBlank() || request.getText().isBlank()) {
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("Student ID and message text must not be empty")
                                    .asRuntimeException()
                    );
                    return;
                }

                // Generate a simple tutor response
                
                // Here I am using a simple rule-based approach
                // so the tutoring conversation is easy to test and explain
                String studentText = request.getText().toLowerCase();
                String tutorReply;

                if (studentText.contains("fractions")) {
                    tutorReply = "Lets review fractions. Try simplifying 6/12. What answeer do you get?";
                } else if (studentText.contains("algebra")) {
                    tutorReply = "Lets practice algebra. Solve x + 5 = 12. What is x?";
                } else if (studentText.contains("geometry")) {
                    tutorReply = "Lets review geometry. What is the formula for the area of a rectangle?";
                } else if (studentText.contains("help")) {
                    tutorReply = "Im here to help. Tell me which topic you are struggling with.";
                } else {
                    tutorReply = "Good attempt. Please explain your question in more detail so I can guide you better.";
                }

                //  Build response message
               
                TutorMessage response = TutorMessage.newBuilder()
                        .setSessionId(request.getSessionId())
                        .setStudentId(request.getStudentId())
                        .setText(tutorReply)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                // Send tutor reply back to the client
                responseObserver.onNext(response);
            }

            /**
             * Called if the client stream fails with an error.
             */
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error in tutor session: " + throwable.getMessage());
            }

            /**
             * Called when the client finishes sending messages.
             */
            @Override
            public void onCompleted() {

                // Once the client is finished, the server also completes the stream
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Determines the student's risk level.
     * This is based on attendance and average score.
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
     * Determines the recommended difficulty level for learning materials.
     */
    private DifficultyLevel determineDifficultyLevel(double attendanceRate, double averageScore) {

        if (attendanceRate < 0.70 || averageScore < 0.50) {
            return DifficultyLevel.FOUNDATION;
        } else if (attendanceRate < 0.85 || averageScore < 0.70) {
            return DifficultyLevel.INTERMEDIATE;
        } else {
            return DifficultyLevel.ADVANCED;
        }
    }

    // Builds a list of learning materials based on topic and difficulty level.
    
    private List<LearningMaterial> buildMaterials(String topic, DifficultyLevel difficultyLevel) {

        List<LearningMaterial> materials = new ArrayList<>();

        switch (difficultyLevel) {

            case FOUNDATION:
                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Basics Video")
                        .setType("Video")
                        .setDifficulty("FOUNDATION")
                        .setLink("https://example.com/foundation-video")
                        .build());

                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Intro Notes")
                        .setType("Notes")
                        .setDifficulty("FOUNDATION")
                        .setLink("https://example.com/foundation-notes")
                        .build());
                break;

            case INTERMEDIATE:
                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Practice Exercises")
                        .setType("Worksheet")
                        .setDifficulty("INTERMEDIATE")
                        .setLink("https://example.com/intermediate-worksheet")
                        .build());

                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Guided Tutorial")
                        .setType("Tutorial")
                        .setDifficulty("INTERMEDIATE")
                        .setLink("https://example.com/intermediate-tutorial")
                        .build());
                break;

            case ADVANCED:
                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Advanced Challenge Set")
                        .setType("Exercise Set")
                        .setDifficulty("ADVANCED")
                        .setLink("https://example.com/advanced-exercises")
                        .build());

                materials.add(LearningMaterial.newBuilder()
                        .setTitle(topic + " Extension Reading")
                        .setType("Reading")
                        .setDifficulty("ADVANCED")
                        .setLink("https://example.com/advanced-reading")
                        .build());
                break;
        }

        return materials;
    }
}
