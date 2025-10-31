package com.example.todoapp.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.todoapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory này chịu trách nhiệm cung cấp dữ liệu cho ListView của Widget.
 * Nó đọc dữ liệu từ SharedPreferences.
 */
public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private List<String> mTaskTitles = new ArrayList<>();

    public static final String PREFS_NAME = "WidgetData";
    public static final String PREFS_KEY_TASKS = "TodayTasks";

    public WidgetRemoteViewsFactory(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        // Không cần làm gì nhiều ở đây
    }

    /**
     * Được gọi khi widget được thông báo dữ liệu đã thay đổi (notifyAppWidgetViewDataChanged)
     */
    @Override
    public void onDataSetChanged() {
        // Đây là nơi lấy dữ liệu
        mTaskTitles.clear();

        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> tasksSet = prefs.getStringSet(PREFS_KEY_TASKS, new HashSet<>());

        mTaskTitles.addAll(tasksSet);
        Collections.sort(mTaskTitles); // Sắp xếp theo ABC
    }

    @Override
    public void onDestroy() {
        mTaskTitles.clear();
    }

    @Override
    public int getCount() {
        return mTaskTitles.size();
    }

    /**
     * Tạo và điền dữ liệu cho từng hàng trong ListView
     */
    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= mTaskTitles.size()) {
            return null;
        }

        String taskTitle = mTaskTitles.get(position);

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        rv.setTextViewText(R.id.widget_item_text, taskTitle);

        // Thêm "fill-in" intent để xử lý click
        // Khi item này được click, nó sẽ sử dụng PendingIntent Template
        // đã được cài đặt trong Provider.
        Intent fillInIntent = new Intent();
        // fillInIntent.putExtra("task_id", taskId); // (Có thể thêm nếu bạn lưu cả ID)
        rv.setOnClickFillInIntent(R.id.widget_item_text, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null; // Dùng loading view mặc định
    }

    @Override
    public int getViewTypeCount() {
        return 1; // Chỉ có 1 loại item
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
