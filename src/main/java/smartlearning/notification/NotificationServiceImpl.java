
/**
 *
 * @author comin
 */
package smartlearning.notification;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    // Store notifications by recipient ID
    private final Map<String, List<Notification>> notificationStore = new ConcurrentHashMap<>();

    @Override
    public void sendAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {

        if (request.getRecipientId().isBlank() || request.getMessage().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Recipient ID and message must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String notificationId = UUID.randomUUID().toString();

        Notification notification = Notification.newBuilder()
                .setNotificationId(notificationId)
                .setRecipientId(request.getRecipientId())
                .setMessage(request.getMessage())
                .setPriority(request.getPriority())
                .setTimestamp(System.currentTimeMillis())
                .build();

        notificationStore.putIfAbsent(request.getRecipientId(), new ArrayList<>());
        notificationStore.get(request.getRecipientId()).add(notification);

        AlertResponse response = AlertResponse.newBuilder()
                .setDelivered(true)
                .setNotificationId(notificationId)
                .setMessage("Alert sent successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getNotifications(UserRequest request, StreamObserver<NotificationList> responseObserver) {

        if (request.getUserId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("User ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        List<Notification> notifications = notificationStore.getOrDefault(request.getUserId(), new ArrayList<>());

        NotificationList response = NotificationList.newBuilder()
                .addAllNotifications(notifications)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void streamNotifications(UserRequest request, StreamObserver<Notification> responseObserver) {

        if (request.getUserId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("User ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String userId = request.getUserId();
        List<Notification> notifications = notificationStore.getOrDefault(userId, new ArrayList<>());

        ServerCallStreamObserver<Notification> serverObserver =
                (ServerCallStreamObserver<Notification>) responseObserver;

        serverObserver.setOnCancelHandler(() ->
                System.out.println("Notification stream cancelled by client for user: " + userId)
        );

        try {
            for (Notification notification : notifications) {

                if (serverObserver.isCancelled()) {
                    System.out.println("Stopping stream because client cancelled for user: " + userId);
                    return;
                }

                responseObserver.onNext(notification);

                // Small delay so streaming can be observed clearly
                Thread.sleep(1000);
            }

            responseObserver.onCompleted();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Streaming interrupted")
                            .asRuntimeException()
            );
        }
    }
}
