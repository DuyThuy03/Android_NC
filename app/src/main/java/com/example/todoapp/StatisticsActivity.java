package com.example.todoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.model.Task;

import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private TextView tvHighPriority, tvMediumPriority, tvLowPriority;

    private FirebaseTaskRepository taskRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Ánh xạ views
        toolbar = findViewById(R.id.toolbarStatistics);
        progressBar = findViewById(R.id.statsProgressBar);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        tvHighPriority = findViewById(R.id.tvHighPriority);
        tvMediumPriority = findViewById(R.id.tvMediumPriority);
        tvLowPriority = findViewById(R.id.tvLowPriority);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Khởi tạo repository
        taskRepository = new FirebaseTaskRepository();

        // Tải dữ liệu
        loadStatistics();
    }

    private void loadStatistics() {
        progressBar.setVisibility(View.VISIBLE);

        // Chúng ta sẽ dùng hàm getAllTasks (đã được cập nhật để lọc theo 'members')
        taskRepository.getAllTasks()
                .addOnSuccessListener(tasks -> {
                    progressBar.setVisibility(View.GONE);
                    if (tasks == null || tasks.isEmpty()) {
                        Toast.makeText(this, "Không có dữ liệu công việc", Toast.LENGTH_SHORT).show();
                        // Hiển thị số 0
                        displayStats(0, 0, 0, 0, 0, 0);
                        return;
                    }

                    // Bắt đầu tính toán
                    int total = tasks.size();
                    int completed = 0;
                    int high = 0;
                    int medium = 0;
                    int low = 0;

                    for (Task task : tasks) {
                        if (task.isCompleted()) {
                            completed++;
                        }

                        String priority = task.getPriority();
                        if (priority != null) {
                            switch (priority) {
                                case "high":
                                    high++;
                                    break;
                                case "medium":
                                    medium++;
                                    break;
                                case "low":
                                    low++;
                                    break;
                            }
                        }
                    }

                    int pending = total - completed;

                    // Hiển thị dữ liệu
                    displayStats(total, completed, pending, high, medium, low);

                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải thống kê: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayStats(int total, int completed, int pending, int high, int medium, int low) {
        tvTotalTasks.setText("Tổng số công việc: " + total);
        tvCompletedTasks.setText("Đã hoàn thành: " + completed);
        tvPendingTasks.setText("Chưa hoàn thành: " + pending);

        tvHighPriority.setText("🔴 Cao: " + high);
        tvMediumPriority.setText("🟡 Trung bình: " + medium);
        tvLowPriority.setText("🟢 Thấp: " + low);
    }
}