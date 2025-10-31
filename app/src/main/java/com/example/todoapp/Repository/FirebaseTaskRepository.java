package com.example.todoapp.Repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.todoapp.model.Task;
import java.util.List;

public class FirebaseTaskRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final String COLLECTION_NAME = "tasks";

    public FirebaseTaskRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // 🔽 CHỈNH SỬA HÀM NÀY 🔽
    // 🔹 Thêm Task
    public com.google.android.gms.tasks.Task<String> addTask(Task task) { // ⬅️ Sửa kiểu trả về thành Task<String>
        if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
            task.setTaskId(db.collection(COLLECTION_NAME).document().getId());
        }

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        task.setUid(uid);
        task.setCreatedAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());

        final String taskId = task.getTaskId(); // ⬅️ Lưu lại ID

        return db.collection(COLLECTION_NAME)
                .document(taskId) // ⬅️ Dùng taskId ở đây
                .set(task)
                .continueWith(innerTask -> { // ⬅️ Thêm continueWith để trả về ID
                    if (innerTask.isSuccessful()) {
                        return taskId; // ⬅️ Trả về ID
                    } else {
                        throw innerTask.getException();
                    }
                });
    }
    // 🔼 KẾT THÚC CHỈNH SỬA 🔼


    // 🔹 Lấy toàn bộ Task của user
    public com.google.android.gms.tasks.Task<List<Task>> getAllTasks() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Task.class);
                    } else {
                        throw task.getException();
                    }
                });
    }

    // 🔹 Cập nhật Task
    public com.google.android.gms.tasks.Task<Void> updateTask(Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        return db.collection(COLLECTION_NAME)
                .document(task.getTaskId())
                .set(task);
    }

    // 🔹 Xóa Task
    public com.google.android.gms.tasks.Task<Void> deleteTask(String taskId) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .delete();
    }

    // 🔹 Lấy Task theo ID
    public com.google.android.gms.tasks.Task<Task> getTaskById(String taskId) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().toObject(Task.class);
                    } else {
                        return null;
                    }
                });
    }

    // 🔹 Lọc Task theo Category
    public com.google.android.gms.tasks.Task<List<Task>> getTasksByCategory(String categoryId) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Task.class);
                    } else {
                        throw task.getException();
                    }
                });
    }

    // 🔹 Lọc Task theo Priority
    public com.google.android.gms.tasks.Task<List<Task>> getTasksByPriority(String priority) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .whereEqualTo("priority", priority)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Task.class);
                    } else {
                        throw task.getException();
                    }
                });
    }
    // Lọc theo trạng thái hoàn thành hoặc chưa hoàn thành
    // 🔹 Lọc Task theo trạng thái hoàn thành (true = đã hoàn thành, false = chưa hoàn thành)
    public com.google.android.gms.tasks.Task<List<Task>> getTasksByCompletionStatus(boolean isCompleted) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .whereEqualTo("completed", isCompleted)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().toObjects(Task.class);
                    } else {
                        throw task.getException();
                    }
                });
    }

}