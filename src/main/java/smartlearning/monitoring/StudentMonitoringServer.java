
/**
 *
 * @author comin
 */
package smartlearning.monitoring;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import smartlearning.naming.NamingServiceGrpc;
import smartlearning.naming.RegisterResponse;
import smartlearning.naming.ServiceInfo;

/**
 * This class starts the Student Monitoring Service.
 *
 * Additionally, it registers itself with the Naming Service
 * so other services can discover it dynamically.
 */
public class StudentMonitoringServer {

    private static final int PORT = 50051;

    public static void main(String[] args) {

        Server server = ServerBuilder.forPort(PORT)
                .addService(new StudentMonitoringServiceImpl())
                .build();

        try {
            //  Start the server
            server.start();
            System.out.println("Student Monitoring Service started on port " + PORT);

            // Register service in Naming Service
            registerWithNamingService();

            // shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Student Monitoring Service...");
                server.shutdown();
            }));

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Student Monitoring Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method connects to the Naming Service and registers this service.
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
                    .setName("StudentMonitoringService")
                    .setHost("localhost")
                    .setPort(PORT)
                    .addCapabilities("attendance")
                    .addCapabilities("scores")
                    .addCapabilities("progress tracking")
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