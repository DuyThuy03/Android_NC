package com.example.todoapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Lớp trợ giúp để ĐẶT LỊCH (schedule) và HỦY LỊCH (cancel)
 * các thông báo bằng AlarmManager.
 */
public class NotificationScheduler {

    /**
     * Đặt lịch thông báo
     * @param context Context
     * @param triggerTime Thời gian (milliseconds) mà thông báo sẽ reo
     * @param taskId ID của task (dùng để tạo ID thông báo duy nhất)
     * @param title Tiêu đề
     * @param message Nội dung
     */
    public static void scheduleNotification(Context context, long triggerTime, String taskId, String title, String message) {

        // Chỉ đặt lịch nếu thời gian là ở tương lai
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w("Scheduler", "Không đặt lịch cho thời gian đã qua.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Cần có ID duy nhất cho mỗi báo thức, dùng hashCode của taskId
        int notificationId = taskId.hashCode();

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("taskTitle", title);
        intent.putExtra("taskMessage", message);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Dùng ID duy nhất
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Kiểm tra quyền đặt báo thức chính xác (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager == null || !alarmManager.canScheduleExactAlarms()) {
                Log.w("Scheduler", "Không có quyền đặt báo thức chính xác.");
                Toast.makeText(context, "Vui lòng cấp quyền báo thức chính xác cho ứng dụng", Toast.LENGTH_LONG).show();
                // Hướng người dùng đến cài đặt
                Intent permIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(permIntent);
                } catch (Exception e) {
                    Log.e("Scheduler", "Không thể mở cài đặt báo thức", e);
                }
                return; // Không đặt lịch nếu chưa có quyền
            }
        }

        // Đặt báo thức chính xác
        try {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
            Log.d("Scheduler", "Đã đặt báo thức cho '" + title + "' (ID: " + notificationId + ") lúc " + triggerTime);
        } catch (Exception e) {
            Log.e("Scheduler", "Lỗi đặt báo thức: " + e.getMessage());
        }
    }

    /**
     * Hủy lịch thông báo
     * @param context Context
     * @param taskId ID của task (phải giống hệt lúc đặt lịch)
     */
    public static void cancelNotification(Context context, String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int notificationId = taskId.hashCode(); // ID phải TRÙNG KHỚP

        Intent intent = new Intent(context, NotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Dùng ID duy nhất
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("Scheduler", "Đã hủy báo thức cho (ID: " + notificationId + ")");
        }
    }
}