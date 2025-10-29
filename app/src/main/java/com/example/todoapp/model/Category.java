package com.example.todoapp.model;

public class Category {
    private String categoryId;
    private String uid;
    private String name;
    private String color;

    // Constructors
    public Category() {
        // Default constructor
    }

    public Category(String categoryId, String uid, String name, String color) {
        this.categoryId = categoryId;
        this.uid = uid;
        this.name = name;
        this.color = color;
    }

    // Getters and Setters
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}