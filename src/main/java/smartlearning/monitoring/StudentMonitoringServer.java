
/**
 *
 * @author comin
 */
package smartlearning.monitoring;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class StudentMonitoringServer {

    private static final int PORT = 50051;

    public static void main(String[] args) {
        Server server = ServerBuilder.forPort(PORT)
                .addService(new StudentMonitoringServiceImpl())
                .build();

        try {
            server.start();
            System.out.println("Student Monitoring Service started on port " + PORT);

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
}
