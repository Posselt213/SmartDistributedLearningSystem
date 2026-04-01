
/**
 *
 * @author comin
 */
package smartlearning.notification;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to demonstrate how cancellation works in gRPC.
 *
 * The idea:
 * - First we send some notifications so the server has data
 * - Then we start a streaming call
 * - After receiving a few messages, we cancel the stream from the client side
 *
 */
public class NotificationCancellationTest {

    public static void main(String[] args) {

        // Create connection to Notification Service
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50053)
                .usePlaintext()
                .build();

        // Blocking stub → used for sending alerts (normal unary calls)
        NotificationServiceGrpc.NotificationServiceBlockingStub blockingStub =
                NotificationServiceGrpc.newBlockingStub(channel);

        // Async stub → used for streaming calls
        NotificationServiceGrpc.NotificationServiceStub asyncStub =
                NotificationServiceGrpc.newStub(channel);

        try {

            String userId = "S2001";

            // =====================================================
            // STEP 1: Prepare data (send notifications first)
            // =====================================================
            // We send multiple alerts so the stream has something to return

            blockingStub.sendAlert(AlertRequest.newBuilder()
                    .setRecipientId(userId)
                    .setMessage("First notification")
                    .setPriority(Priority.LOW)
                    .build());

            blockingStub.sendAlert(AlertRequest.newBuilder()
                    .setRecipientId(userId)
                    .setMessage("Second notification")
                    .setPriority(Priority.MEDIUM)
                    .build());

            blockingStub.sendAlert(AlertRequest.newBuilder()
                    .setRecipientId(userId)
                    .setMessage("Third notification")
                    .setPriority(Priority.HIGH)
                    .build());

            // Latch used to wait until streaming finishes or is cancelled
            CountDownLatch latch = new CountDownLatch(1);

            // =====================================================
            // STEP 2: Create a cancellable context
            // =====================================================
            // This allows us to cancel the RPC manually from the client
            
            Context.CancellableContext cancellableContext = Context.current().withCancellation();

            System.out.println("Starting notification stream...");

            // Run the streaming call inside the cancellable context
            cancellableContext.run(() -> {

                asyncStub.streamNotifications(
                        UserRequest.newBuilder()
                                .setUserId(userId)
                                .build(),

                        // Observer that receives streamed messages
                        new StreamObserver<Notification>() {

                            int receivedCount = 0;

                            /**
                             * Called every time the server sends a message
                             */
                            @Override
                            public void onNext(Notification notification) {

                                receivedCount++;

                                System.out.println("Received: " + notification.getMessage());

                                // =====================================================
                                // STEP 3: Cancel after receiving 2 messages
                                // =====================================================
                                // This simulates a user stopping the stream manually
                                if (receivedCount == 2) {
                                    System.out.println("Cancelling stream from client side...");
                                    cancellableContext.cancel(null);
                                }
                            }

                            /**
                             * Called when an error occurs (expected after cancellation)
                             */
                            @Override
                            public void onError(Throwable throwable) {
                                System.err.println("Stream error after cancellation: " + throwable.getMessage());
                                latch.countDown();
                            }

                            /**
                             * Called if the stream finishes normally (won’t happen here)
                             */
                            @Override
                            public void onCompleted() {
                                System.out.println("Stream completed normally.");
                                latch.countDown();
                            }
                        }
                );
            });

            // Wait for streaming to finish or cancel
            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Always close the channel to release resources
            channel.shutdown();
        }
    }
}