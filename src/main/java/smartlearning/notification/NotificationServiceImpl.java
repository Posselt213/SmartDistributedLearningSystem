
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

/**
 * NotificationServiceImpl handles all notification-related operations.
 * It stores notifications in memory and supports unary and streaming RPCs.
 */
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    // In-memory storage for notifications grouped by recipient ID
    private final Map<String, List<Notification>> notificationStore = new ConcurrentHashMap<>();

    /**
     * Sends a notification (alert) to a specific user.
     * Unary RPC.
     */
    @Override
    public void sendAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {

        // Validate input
        if (request.getRecipientId().isBlank() || request.getMessage().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Recipient ID and message must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        // Generate a unique ID for the notification
        String notificationId = UUID.randomUUID().toString();

        // Build notification object
        Notification notification = Notification.newBuilder()
                .setNotificationId(notificationId)
                .setRecipientId(request.getRecipientId())
                .setMessage(request.getMessage())
                .setPriority(request.getPriority())
                .setTimestamp(System.currentTimeMillis())
                .build();

        // Store notification in memory
        notificationStore.putIfAbsent(request.getRecipientId(), new ArrayList<>());
        notificationStore.get(request.getRecipientId()).add(notification);

        // Build response
        AlertResponse response = AlertResponse.newBuilder()
                .setDelivered(true)
                .setNotificationId(notificationId)
                .setMessage("Alert sent successfully")
                .build();

        // Send response back to client
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Returns all stored notifications for a given user.
     * Unary RPC.
     */
    @Override
    public void getNotifications(UserRequest request, StreamObserver<NotificationList> responseObserver) {

        // Validate input
        if (request.getUserId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("User ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        // Retrieve notifications or return empty list
        List<Notification> notifications = notificationStore.getOrDefault(request.getUserId(), new ArrayList<>());

        // Build response
        NotificationList response = NotificationList.newBuilder()
                .addAllNotifications(notifications)
                .build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Streams notifications to the client in real time.
     * Server-streaming RPC.
     */
    @Override
    public void streamNotifications(UserRequest request, StreamObserver<Notification> responseObserver) {

        // Validate input
        if (request.getUserId().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("User ID must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String userId = request.getUserId();

        // Get notifications for the user
        List<Notification> notifications = notificationStore.getOrDefault(userId, new ArrayList<>());

        // Cast observer to support cancellation detection
        ServerCallStreamObserver<Notification> serverObserver =
                (ServerCallStreamObserver<Notification>) responseObserver;

        // Define behavior when client cancels the stream
        serverObserver.setOnCancelHandler(() ->
                System.out.println("Notification stream cancelled by client for user: " + userId)
        );

        try {
            // Send each notification one by one
            for (Notification notification : notifications) {

                // Stop sending if client has cancelled the stream
                if (serverObserver.isCancelled()) {
                    System.out.println("Stopping stream because client cancelled for user: " + userId);
                    return;
                }

                // Send notification to client
                responseObserver.onNext(notification);

                // Delay to simulate real-time streaming
                Thread.sleep(1000);
            }

            // Complete stream
            responseObserver.onCompleted();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            // Handle unexpected interruption
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Streaming interrupted")
                            .asRuntimeException()
            );
        }
    }
}
