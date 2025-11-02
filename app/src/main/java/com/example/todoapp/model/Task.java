package com.example.todoapp.model;

import java.util.List;

public class Task {
    private String taskId;
    private String uid;
    private String title;
    private String description;
    private long dueDate;
    private String priority;
    private String categoryId;
    private boolean isCompleted;
    private List<String> subtasks;
    private List<String> notes;
    private long createdAt;
    private long updatedAt;
    private List<String> members;

    // Constructors
    public Task() {
        // Default constructor
    }

    public Task(String taskId, String uid, String title, String description, long dueDate, String priority, String categoryId, boolean isCompleted, List<String> subtasks, List<String> notes, long createdAt, long updatedAt,List<String> members) {
        this.taskId = taskId;
        this.uid = uid;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.categoryId = categoryId;
        this.isCompleted = isCompleted;
        this.subtasks = subtasks;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.members = members;
    }

    // Getters and Setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public List<String> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<String> subtasks) {
        this.subtasks = subtasks;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}