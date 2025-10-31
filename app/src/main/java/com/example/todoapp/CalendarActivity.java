package com.example.todoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.adapter.TaskAdapter;
import com.example.todoapp.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private TextView tvSelectedDateTasks;
    private Toolbar toolbar;

    private TaskAdapter adapter;
    private List<Task> allTasks; // Danh sách chứa TẤT CẢ task
    private List<Object> displayedTasks; // Danh sách task để hiển thị (chỉ task của ngày đã chọn)

    private FirebaseTaskRepository taskRepository;
    private Calendar selectedDate;
    private SimpleDateFormat headerSdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Khởi tạo
        taskRepository = new FirebaseTaskRepository();
        allTasks = new ArrayList<>();
        displayedTasks = new ArrayList<>();
        selectedDate = Calendar.getInstance(); // Mặc định là hôm nay
        headerSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        initViews();
        setupToolbar();
        setupRecyclerView();

        // Tải tất cả task và lọc cho ngày hôm nay
        loadAllTasksAndFilter();

        setupCalendarListener();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCalendar);
        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.rvCalendarTasks);
        tvSelectedDateTasks = findViewById(R.id.tvSelectedDateTasks);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        // Chúng ta có thể tái sử dụng TaskAdapter
        adapter = new TaskAdapter(displayedTasks, new TaskAdapter.OnTaskListener() {
            @Override
            public void onTaskDelete(int position) {
                // (Bạn có thể thêm logic xóa nếu muốn)
            }

            @Override
            public void onTaskEdit(int position) {
                // (Bạn có thể thêm logic sửa nếu muốn)
            }

            @Override
            public void onTaskClick(int position) {
                // Mở TaskDetailActivity
                if (displayedTasks.get(position) instanceof Task) {
                    Task task = (Task) displayedTasks.get(position);
                    Intent intent = new Intent(CalendarActivity.this, com.example.todoapp.TaskDetail.TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getTaskId());
                    intent.putExtra("title", task.getTitle());
                    intent.putExtra("description", task.getDescription());
                    intent.putExtra("categoryId", task.getCategoryId());
                    intent.putExtra("priority", task.getPriority());
                    intent.putExtra("dueDate", task.getDueDate());
                    intent.putExtra("createdAt", task.getCreatedAt());
                    intent.putExtra("updatedAt", task.getUpdatedAt());
                    intent.putExtra("isCompleted", task.isCompleted());
                    if (task.getNotes() != null)
                        intent.putStringArrayListExtra("notes", new ArrayList<>(task.getNotes()));
                    if (task.getSubtasks() != null)
                        intent.putStringArrayListExtra("subtasks", new ArrayList<>(task.getSubtasks()));
                    startActivity(intent);
                }
            }

            @Override
            public void onTaskCheckChanged(int position, boolean isChecked) {
                // (Bạn có thể thêm logic cập nhật status nếu muốn)
                if (displayedTasks.get(position) instanceof Task) {
                    Task task = (Task) displayedTasks.get(position);
                    task.setCompleted(isChecked);
                    taskRepository.updateTask(task)
                            .addOnSuccessListener(aVoid -> filterTasksForDate(selectedDate)) // Lọc lại
                            .addOnFailureListener(e -> Toast.makeText(CalendarActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onHeaderClick(int position) {
                // Không dùng ở đây
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAllTasksAndFilter() {
        taskRepository.getAllTasks()
                .addOnSuccessListener(tasks -> {
                    allTasks.clear();
                    if (tasks != null) {
                        allTasks.addAll(tasks);
                    }
                    // Lọc cho ngày hôm nay (mặc định)
                    filterTasksForDate(Calendar.getInstance());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                filterTasksForDate(selectedDate);
            }
        });
    }

    // Hàm quan trọng: Lọc và hiển thị task
    private void filterTasksForDate(Calendar selectedDate) {
        displayedTasks.clear();

        // Đặt tiêu đề (ví dụ: "Công việc cho 01/11/2025")
        tvSelectedDateTasks.setText("Công việc cho " + headerSdf.format(selectedDate.getTime()));

        for (Task task : allTasks) {
            if (isSameDay(task.getDueDate(), selectedDate)) {
                displayedTasks.add(task);
            }
        }

        if (displayedTasks.isEmpty()) {
            // (Bạn có thể hiển thị một thông báo "Không có task")
        }

        adapter.notifyDataSetChanged();
    }

    // Hàm tiện ích để kiểm tra 2 ngày có trùng nhau không
    private boolean isSameDay(long taskMillis, Calendar selectedCalendar) {
        if (taskMillis == 0) return false;

        Calendar taskCalendar = Calendar.getInstance();
        taskCalendar.setTimeInMillis(taskMillis);

        return taskCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                taskCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedCalendar.get(Calendar.DAY_OF_MONTH);
    }
}