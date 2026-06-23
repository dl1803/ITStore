package com.example.itstore.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.itstore.R;
import com.example.itstore.activity.NotificationActivity;
import com.example.itstore.model.LoginResponse;
import com.example.itstore.utils.SharedPrefsManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = "IT Store";
        String body = "Bạn có thông báo mới!";
        String notiType = "system";


        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null)
                title = remoteMessage.getNotification().getTitle();
            if (remoteMessage.getNotification().getBody() != null)
                body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (remoteMessage.getData().size() > 0) {
            if (data.containsKey("notification")) {
                String jsonString = data.get("notification");
                try {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
                    if (jsonObject.has("title")) {
                        title = jsonObject.getString("title");
                    }
                    if (jsonObject.has("body")) {
                        body = jsonObject.getString("body");
                    }
                    if (jsonObject.has("type")) {
                        notiType = jsonObject.getString("type");
                    }
                } catch (org.json.JSONException e) {
                    Log.e("FCM_ERROR", "Lỗi Parse JSON từ Backend: " + e.getMessage());
                }
            }
            else {
                if (data.containsKey("title")) title = data.get("title");
                if (data.containsKey("body")) body = data.get("body");
                if (data.containsKey("type")) notiType = data.get("type");
            }
        }
        Intent broadcastIntent = new Intent("ACTION_UPDATE_NOTI_BADGE");
        broadcastIntent.setPackage(getPackageName());
        sendBroadcast(broadcastIntent);

        String token = SharedPrefsManager.getInstance(getApplicationContext()).getAccessToken();
        if (token == null || token.isEmpty()) {
            Log.d("FCM_SERVICE", "User đã đăng xuất, từ chối hiển thị thông báo cũ!");
            return;
        }

//        int userIdInSession = SharedPrefsManager.getInstance(getApplicationContext()).getUserId();
//        if (data.get("user_id").equals(String.valueOf(userIdInSession))){
//            return;
//        }

        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        if (notiType != null) {
            switch (notiType.toLowerCase()) {
                case "order":
                case "payment":
                    boolean isOrderEnabled = prefs.getBoolean("order_noti", true);
                    if (!isOrderEnabled) return;
                    break;
                case "promotion":
                    boolean isPromoEnabled = prefs.getBoolean("promo_noti", true);
                    if (!isPromoEnabled) return;
                    break;
                case "system":
                    break;
            }
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        String channelId = "itstore_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Thông báo IT-Store", NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_SERVICE", "Token mới phát hành: " + token);
    }
}