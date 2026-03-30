/**
 *
 * @author comin
 */
package smartlearning.naming;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {

    // Store services by name
    private final Map<String, ServiceInfo> registeredServices = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceInfo request, StreamObserver<RegisterResponse> responseObserver) {

        // Basic validation
        if (request.getName().isBlank() || request.getHost().isBlank() || request.getPort() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Service name, host and port must be valid")
                            .asRuntimeException()
            );
            return;
        }

        // Save or replace service
        registeredServices.put(request.getName(), request);

        RegisterResponse response = RegisterResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Service registered successfully: " + request.getName())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listServices(Empty request, StreamObserver<ServiceList> responseObserver) {

        ServiceList.Builder responseBuilder = ServiceList.newBuilder();

        for (ServiceInfo service : registeredServices.values()) {
            responseBuilder.addServices(service);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void findService(ServiceQuery request, StreamObserver<ServiceInfo> responseObserver) {

        if (request.getName().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Service name cannot be empty")
                            .asRuntimeException()
            );
            return;
        }

        ServiceInfo service = registeredServices.get(request.getName());

        if (service == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Service not found: " + request.getName())
                            .asRuntimeException()
            );
            return;
        }

        responseObserver.onNext(service);
        responseObserver.onCompleted();
    }
}