
/**
 *
 * @author comin
 */
package smartlearning.naming;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class NamingServer {

    private static final int PORT = 50050;

    public static void main(String[] args) {
        Server server = ServerBuilder.forPort(PORT)      //Start Grpc server on port 50050 
                .addService(new NamingServiceImpl())        //Connects to NamingServiceImpl.class            Creates Connection between gRPC and the JAVA code 
                .build();

        try {
            server.start();
            System.out.println("Naming Service started on port " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Naming Service...");
                server.shutdown();
            }));

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting Naming Service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}