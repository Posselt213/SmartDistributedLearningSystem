/**
 *
 * @author comin
 */

package smartlearning.adaptive;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

/**
 * This class starts the Adaptive Learning gRPC server.
 */
public class AdaptiveLearningServer {

    private static final int PORT = 50052;

    public static void main(String[] args) {

        // Build the gRPC server and attach the service implementation
        Server server = ServerBuilder.forPort(PORT)
                .addService(new AdaptiveLearningServiceImpl())
                .build();

        try {
            // Start server
            server.start();
            System.out.println("Adaptive Learning Service started on port " + PORT);

            // Shutdown hook to stop server cleanly when the application closes
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Adaptive Learning Service...");
                server.shutdown();
            }));

            // Keep the server running
            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Adaptive Learning Service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}