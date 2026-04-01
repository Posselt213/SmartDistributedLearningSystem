/**
 *
 * @author comin
 */
package smartlearning.adaptive;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.RegisterResponse;
import smartlearning.naming.ServiceInfo;

/**
 * This class starts the Adaptive Learning Service.
 *
 * It also registers itself with the Naming Service
 * so it can be discovered dynamically.
 */
public class AdaptiveLearningServer {

    private static final int PORT = 50052;

    public static void main(String[] args) {

        Server server = ServerBuilder.forPort(PORT)
                .addService(new AdaptiveLearningServiceImpl())
                .build();

        try {
            // Start the server
            server.start();
            System.out.println("Adaptive Learning Service started on port " + PORT);

            // Register in Naming Service
            registerWithNamingService();

            // Shutdown hook for clean stop
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Adaptive Learning Service...");
                server.shutdown();
            }));

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Adaptive Learning Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers this service with the Naming Service.
     */
    private static void registerWithNamingService() {

        ManagedChannel channel = null;

        try {
            // Connect to Naming Service
            channel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            NamingServiceGrpc.NamingServiceBlockingStub stub =
                    NamingServiceGrpc.newBlockingStub(channel);

            // Build service information
            ServiceInfo serviceInfo = ServiceInfo.newBuilder()
                    .setName("AdaptiveLearningService")
                    .setHost("localhost")
                    .setPort(PORT)
                    .addCapabilities("recommendations")
                    .addCapabilities("tutoring")
                    .addCapabilities("bidirectional streaming")
                    .build();

            // Register service
            RegisterResponse response = stub.register(serviceInfo);

            System.out.println("Registered with Naming Service: " + response.getMessage());

        } catch (Exception e) {
            System.err.println("Failed to register with Naming Service: " + e.getMessage());

        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}