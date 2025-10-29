package com.example.todoapp.model;

public class Widget {
    private String widgetId;
    private String uid;
    private String type; // e.g., "task_list", "calendar"
    private String settings; // JSON string for widget settings

    // Constructors
    public Widget() {
        // Default constructor
    }

    public Widget(String widgetId, String uid, String type, String settings) {
        this.widgetId = widgetId;
        this.uid = uid;
        this.type = type;
        this.settings = settings;
    }

    // Getters and Setters
    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }
}