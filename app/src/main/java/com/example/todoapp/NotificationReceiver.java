package com.example.todoapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.todoapp.MainActivity; // Bạn có thể đổi sang LoginActivity nếu muốn

/**
 * Lớp này nhận tín hiệu báo thức (Alarm) khi đến giờ
 * và chịu trách nhiệm hiển thị thông báo.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "todo_channel_id";
    private static final String CHANNEL_NAME = "Todo App Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Lấy dữ liệu từ Intent (do NotificationScheduler gửi qua)
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskMessage = intent.getStringExtra("taskMessage");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Log.d("NotificationReceiver", "Nhận được báo thức cho: " + taskTitle);


        if (taskTitle == null) taskTitle = "Công việc đến hạn!";
        if (taskMessage == null) taskMessage = "Bạn có một công việc cần hoàn thành.";

        // 1. Tạo Kênh Thông Báo (cho Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh thông báo cho ứng dụng Todo App");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // 2. Tạo Intent khi nhấn vào thông báo
        // Intent này sẽ mở MainActivity.
        // Vì MainActivity có launchMode="singleTop", nó sẽ:
        // - Mở app nếu app đang đóng.
        // - Không làm gì (chỉ đưa app ra trước) nếu app đang mở.
        Intent clickIntent = new Intent(context, MainActivity.class); // Mở MainActivity
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent clickPendingIntent = PendingIntent.getActivity(
                context,
                notificationId, // Dùng ID duy nhất
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Xây dựng Thông Báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ⬅️ Icon thông báo
                .setContentTitle(taskTitle)
                .setContentText(taskMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Tự động xóa khi nhấn
                .setContentIntent(clickPendingIntent); // ⬅️ Gán hành động nhấn

        // 4. Hiển thị Thông Báo
        notificationManager.notify(notificationId, builder.build());
    }
}