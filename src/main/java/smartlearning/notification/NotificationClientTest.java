/**
 *
 * @author comin
 */
package smartlearning.notification;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import io.grpc.stub.StreamObserver;

/**
 * This class is used to test the NotificationService.
 * Here I test:
 * - sending alerts (unary)
 * - retrieving stored notifications (unary)
 * - streaming notifications (server streaming)
 */
public class NotificationClientTest {

    public static void main(String[] args) {

        // Create a connection (channel) to the Notification Service running on localhost
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50053)
                .usePlaintext()
                .build();

        // Blocking stub → used for normal (unary) calls
        NotificationServiceGrpc.NotificationServiceBlockingStub blockingStub =
                NotificationServiceGrpc.newBlockingStub(channel);

        // Async stub → used for streaming calls
        NotificationServiceGrpc.NotificationServiceStub asyncStub =
                NotificationServiceGrpc.newStub(channel);

        try {

            // I will use the same user for all tests
            String userId = "S1001";

            // =====================================================
            // 1. SEND FIRST ALERT (Unary RPC)
            // =====================================================

            AlertResponse alert1 = blockingStub.sendAlert(
                    AlertRequest.newBuilder()
                            .setRecipientId(userId)   // who receives the alert
                            .setMessage("Your attendance is below the expected level.")
                            .setPriority(Priority.HIGH)
                            .build()
            );

            System.out.println("First Alert Response:");
            System.out.println("Delivered: " + alert1.getDelivered());
            System.out.println("Notification ID: " + alert1.getNotificationId());
            System.out.println("Message: " + alert1.getMessage());

            // =====================================================
            // 2. SEND SECOND ALERT
            // =====================================================

            AlertResponse alert2 = blockingStub.sendAlert(
                    AlertRequest.newBuilder()
                            .setRecipientId(userId)
                            .setMessage("Please review the recommended study materials.")
                            .setPriority(Priority.MEDIUM)
                            .build()
            );

            System.out.println("\nSecond Alert Response:");
            System.out.println("Delivered: " + alert2.getDelivered());
            System.out.println("Notification ID: " + alert2.getNotificationId());
            System.out.println("Message: " + alert2.getMessage());

            // =====================================================
            // 3. GET ALL STORED NOTIFICATIONS (Unary RPC)
            // =====================================================

            NotificationList notificationList = blockingStub.getNotifications(
                    UserRequest.newBuilder()
                            .setUserId(userId)
                            .build()
            );

            System.out.println("\nStored Notifications:");

            // Loop through all notifications returned by the server
            for (Notification notification : notificationList.getNotificationsList()) {
                System.out.println("-----------------------------------");
                System.out.println("ID: " + notification.getNotificationId());
                System.out.println("Recipient: " + notification.getRecipientId());
                System.out.println("Message: " + notification.getMessage());
                System.out.println("Priority: " + notification.getPriority());
                System.out.println("Timestamp: " + notification.getTimestamp());
            }

            // =====================================================
            // 4. SERVER STREAMING TEST
            // =====================================================

            // This latch is used to wait until the stream finishes
            CountDownLatch latch = new CountDownLatch(1);

            System.out.println("\nStreaming Notifications:");

            // Start streaming notifications from the server
            asyncStub.streamNotifications(
                    UserRequest.newBuilder()
                            .setUserId(userId)
                            .build(),

                    // This observer receives messages from the server
                    new StreamObserver<Notification>() {

                        // Called every time the server sends a notification
                        @Override
                        public void onNext(Notification notification) {
                            System.out.println("Streamed Notification:");
                            System.out.println("ID: " + notification.getNotificationId());
                            System.out.println("Message: " + notification.getMessage());
                            System.out.println("Priority: " + notification.getPriority());
                            System.out.println();
                        }

                        // Called if an error happens during streaming
                        @Override
                        public void onError(Throwable throwable) {
                            System.err.println("Streaming Error: " + throwable.getMessage());
                            latch.countDown();
                        }

                        // Called when the server finishes sending messages
                        @Override
                        public void onCompleted() {
                            System.out.println("Notification streaming completed.");
                            latch.countDown();
                        }
                    }
            );

            // Wait up to 10 seconds for the streaming to finish
            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Always close the channel at the end
            channel.shutdown();
        }
    }
}
