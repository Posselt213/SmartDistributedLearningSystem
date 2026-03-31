
/**
 *
 * @author comin
 */

package smartlearning.naming;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class NamingClientTest {

    public static void main(String[] args) {

        // Connect to Naming Service
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50050)
                .usePlaintext()
                .build();

        NamingServiceGrpc.NamingServiceBlockingStub stub =
                NamingServiceGrpc.newBlockingStub(channel);     //Create client Stub 

        try {

            // 1. Register a service
            ServiceInfo service = ServiceInfo.newBuilder()
                    .setName("StudentMonitoringService")
                    .setHost("localhost")
                    .setPort(50051)
                    .addCapabilities("attendance")
                    .addCapabilities("scores")
                    .build();

            RegisterResponse registerResponse = stub.register(service);

            System.out.println("Register Response: " + registerResponse.getMessage());

            // 2. List all services
            ServiceList list = stub.listServices(Empty.newBuilder().build());

            System.out.println("\nRegistered Services:");
            for (ServiceInfo s : list.getServicesList()) {
                System.out.println("- " + s.getName() + " at " + s.getHost() + ":" + s.getPort());
            }

            // 3. Find specific service
            ServiceInfo found = stub.findService(
                    ServiceQuery.newBuilder()
                            .setName("StudentMonitoringService")
                            .build()
            );

            System.out.println("\nFound Service:");
            System.out.println(found.getName() + " -> " + found.getHost() + ":" + found.getPort());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
