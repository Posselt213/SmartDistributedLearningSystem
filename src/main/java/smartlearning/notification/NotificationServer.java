
/**
 *
 * @author comin
 */
package smartlearning.notification;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.RegisterResponse;
import smartlearning.naming.ServiceInfo;

/**
 * This class starts the Notification Service.
 *
 * It also registers the service with the Naming Service
 * so other services and the GUI can discover it dynamically.
 */
public class NotificationServer {

    // Port used by Notification Service
    private static final int PORT = 50053;

    public static void main(String[] args) {

        // Build the gRPC server and attach the service implementation
        Server server = ServerBuilder.forPort(PORT)
                .addService(new NotificationServiceImpl())
                .build();

        try {
            //  STEP 1: Start Notification Service
            server.start();
            System.out.println("Notification Service started on port " + PORT);

            //  Register this service in Naming Service
            registerWithNamingService();

            // Add shutdown hook for clean stop
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Notification Service...");
                server.shutdown();
            }));

            // Keep server running
            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Notification Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method connects to the Naming Service and registers
     * the Notification Service details.
     */
    private static void registerWithNamingService() {

        ManagedChannel channel = null;

        try {
            // Connect to Naming Service running on port 50050
            channel = ManagedChannelBuilder
                    .forAddress("localhost", 50050)
                    .usePlaintext()
                    .build();

            // Create blocking stub for unary RPC call
            NamingServiceGrpc.NamingServiceBlockingStub stub =
                    NamingServiceGrpc.newBlockingStub(channel);

            // Build information about this service
            ServiceInfo serviceInfo = ServiceInfo.newBuilder()
                    .setName("NotificationService")
                    .setHost("localhost")
                    .setPort(PORT)
                    .addCapabilities("alerts")
                    .addCapabilities("notification history")
                    .addCapabilities("notification streaming")
                    .build();

            // Register service with Naming Service
            RegisterResponse response = stub.register(serviceInfo);

            System.out.println("Registered with Naming Service: " + response.getMessage());

        } catch (Exception e) {
            System.err.println("Failed to register with Naming Service: " + e.getMessage());

        } finally {
            // Always close the channel
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}
