package com.example.todoapp.Repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.todoapp.model.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseCategoryRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final String COLLECTION_NAME = "categories";

    public FirebaseCategoryRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // 🔹 Thêm danh mục mới
    public Task<String> addCategory(Category category) {
        // Tạo ID mới nếu chưa có
        if (category.getCategoryId() == null || category.getCategoryId().isEmpty()) {
            category.setCategoryId(db.collection(COLLECTION_NAME).document().getId());
        }

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        category.setUid(uid);

        final String categoryId = category.getCategoryId();

        // 🧩 Bước 1: Kiểm tra trùng tên
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .whereEqualTo("name", category.getName())
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Nếu đã có danh mục trùng tên
                    if (!task.getResult().isEmpty()) {
                        throw new Exception("Tên danh mục đã tồn tại!");
                    }

                    // 🧩 Bước 2: Nếu chưa trùng, tiến hành thêm mới
                    return db.collection(COLLECTION_NAME)
                            .document(categoryId)
                            .set(category)
                            .continueWith(innerTask -> {
                                if (innerTask.isSuccessful()) {
                                    return categoryId;
                                } else {
                                    throw innerTask.getException();
                                }
                            });
                });
    }

    // 🔹 Lấy toàn bộ danh mục theo người dùng hiện tại
    public Task<List<Category>> getAllCategories() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Category.class);
                    } else {
                        throw task.getException();
                    }
                });
    }

    // 🔹 Cập nhật danh mục (tên + màu)
    public Task<Void> updateCategory(Category category) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", category.getName());
        updates.put("color", category.getColor());

        return db.collection(COLLECTION_NAME)
                .document(category.getCategoryId())
                .update(updates);
    }

    // 🔹 Xóa danh mục
    public Task<Void> deleteCategory(String categoryId) {
        return db.collection(COLLECTION_NAME)
                .document(categoryId)
                .delete();
    }

    // 🔹 Lấy danh mục theo ID
    public Task<Category> getCategoryById(String categoryId) {
        return db.collection(COLLECTION_NAME)
                .document(categoryId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().toObject(Category.class);
                    } else {
                        return null;
                    }
                });
    }
}
