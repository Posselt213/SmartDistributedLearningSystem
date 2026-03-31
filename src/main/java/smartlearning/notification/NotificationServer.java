
/**
 *
 * @author comin
 */
package smartlearning.notification;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class NotificationServer {

    private static final int PORT = 50053;

    public static void main(String[] args) {
        Server server = ServerBuilder.forPort(PORT)
                .addService(new NotificationServiceImpl())
                .build();

        try {
            server.start();
            System.out.println("Notification Service started on port " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Notification Service...");
                server.shutdown();
            }));

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Notification Service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
