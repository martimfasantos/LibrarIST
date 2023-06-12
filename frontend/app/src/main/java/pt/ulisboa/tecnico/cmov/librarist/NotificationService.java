package pt.ulisboa.tecnico.cmov.librarist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.net.URI;

public class NotificationService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String SERVER = "ws://192.92.147.54:5000/ws";

    private WebSocketClient webSocketClient;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create and configure the WebSocket client
        URI serverUri = URI.create(SERVER);
        webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                // WebSocket connection is established
            }

            @Override
            public void onMessage(String message) {
                // Handle incoming message here
                if (message != null){
                    try {
                        JSONObject json = new JSONObject(message);
                        String title = json.getString("title");
                        Log.d("WEBSOCKET", "Received title: " + title);
                        displayNotification(title);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Bad Message in Socket!" + e);
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                // WebSocket connection closed
            }

            @Override
            public void onError(Exception ex) {
                // Handle any errors that occur during the connection
                ex.printStackTrace();
            }
        };
        // Connect tot he socket
        webSocketClient.connect();

        // Start the service in the foreground
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // You can handle start command requests here if needed
        return START_STICKY; // Or any other appropriate return value
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Disconnect the WebSocket when the service is destroyed
        webSocketClient.close();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("WebSocket connection is active")
                .setSmallIcon(R.drawable.notification_icon)
                .build();
    }

    private void displayNotification(String message) {
        // Create and display the notification with the received message
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Book Available")
                .setContentText("Book " + message + " is now available at your fav library!")
                .setSmallIcon(R.drawable.notification_icon);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}


