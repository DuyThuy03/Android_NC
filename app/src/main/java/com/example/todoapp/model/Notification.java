package com.example.todoapp.model;

public class Notification {
    private String notificationId;
    private String taskId;
    private String uid;
    private String type; // e.g., "reminder", "daily_summary"
    private long notificationTime;
    private boolean isSent;

    // Constructors
    public Notification() {
        // Default constructor
    }

    public Notification(String notificationId, String taskId, String uid, String type, long notificationTime, boolean isSent) {
        this.notificationId = notificationId;
        this.taskId = taskId;
        this.uid = uid;
        this.type = type;
        this.notificationTime = notificationTime;
        this.isSent = isSent;
    }

    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public long getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(long notificationTime) {
        this.notificationTime = notificationTime;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }
}
