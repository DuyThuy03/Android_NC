package com.example.todoapp.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        // Trả về Factory, nơi xử lý dữ liệu
        return new WidgetRemoteViewsFactory(this.getApplicationContext());
    }
}
