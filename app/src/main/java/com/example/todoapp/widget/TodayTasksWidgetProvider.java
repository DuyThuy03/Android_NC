package com.example.todoapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.content.ComponentName;

import com.example.todoapp.MainActivity;
import com.example.todoapp.R;

public class TodayTasksWidgetProvider extends AppWidgetProvider {

    public static final String WIDGET_DATA_CHANGED = "com.example.todoapp.WIDGET_DATA_CHANGED";

    /**
     * Được gọi khi widget được cập nhật (theo lịch hoặc thủ công)
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Tạo RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.today_tasks_widget);

        // 1. Cài đặt adapter cho ListView
        Intent serviceIntent = new Intent(context, WidgetRemoteViewsService.class);
        // Đặt một ID duy nhất (sử dụng appWidgetId) để đảm bảo Service Intent là duy nhất
        serviceIntent.setData(Uri.fromParts("intent", String.valueOf(appWidgetId), null));
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        // 2. Cài đặt Empty View (hiển thị khi danh sách trống)
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);

        // 3. Cài đặt PendingIntent Template (cho các click vào từng item)
        // Khi một item được click, nó sẽ mở MainActivity
        Intent clickIntent = new Intent(context, MainActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent);

        // 4. Cập nhật widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Cập nhật tất cả các widget đang hoạt động
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * Nhận các broadcast, đặc biệt là broadcast tùy chỉnh của chúng ta
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Nếu nhận được tin hiệu dữ liệu thay đổi
        if (WIDGET_DATA_CHANGED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, TodayTasksWidgetProvider.class)
            );
            // Thông báo cho tất cả widget cập nhật lại dữ liệu danh sách
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Widget đầu tiên được thêm
    }

    @Override
    public void onDisabled(Context context) {
        // Widget cuối cùng bị xóa
    }
}
