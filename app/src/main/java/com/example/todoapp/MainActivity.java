package com.example.todoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.adapter.TaskAdapter;
import com.example.todoapp.Auth.LoginActivity;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.example.todoapp.Repository.FirebaseCategoryRepository;
import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.model.Category;
import com.example.todoapp.model.DateHeader; // ⬅️ THÊM IMPORT
import com.example.todoapp.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;

    private List<Object> displayList;
    private List<Task> allTasks;

    private ImageButton addTask, btnClearSearch,navCalendar;
    private ProgressBar progressBar;
    private EditText searchEditText;
    private LinearLayout chipsContainer;

    private TextView chipHighPriority, chipMediumPriority, chipLowPriority;
    private TextView chipCompleted, chipPending;
    private TextView currentSelectedChip;
    private Map<String, TextView> categoryChipsMap;

    private FirebaseTaskRepository taskRepository;
    private FirebaseCategoryRepository categoryRepository;
    private FirebaseAuthRepository firebaseAuth;

    private String currentFilterType = "none";
    private String currentFilterValue = "";
    private String currentSearchQuery = "";

    // ⭐️ HÀM MỚI: Lưu trạng thái thu gọn/mở rộng
    private Map<String, Boolean> groupExpansionState = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskRepository = new FirebaseTaskRepository();
        categoryRepository = new FirebaseCategoryRepository();

        recyclerView = findViewById(R.id.tasksRecyclerView);
        addTask = findViewById(R.id.nav_add);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        View searchBarLayout = findViewById(R.id.searchBarLayout);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView)
                ((LinearLayout) searchBarLayout).getChildAt(1);
        chipsContainer = (LinearLayout) horizontalScrollView.getChildAt(0);

        chipHighPriority = findViewById(R.id.chipHighPriority);
        chipMediumPriority = findViewById(R.id.chipMediumPriority);
        chipLowPriority = findViewById(R.id.chipLowPriority);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPending = findViewById(R.id.chipPending);
        navCalendar = findViewById(R.id.nav_calendar);
        navCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        categoryChipsMap = new HashMap<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        displayList = new ArrayList<>();
        allTasks = new ArrayList<>();

        adapter = new TaskAdapter(displayList, new TaskAdapter.OnTaskListener() {
            @Override
            public void onTaskDelete(int position) {
                if (displayList.get(position) instanceof Task) {
                    Task deletedTask = (Task) displayList.get(position);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc muốn xóa task: " + deletedTask.getTitle() + "?")
                            .setPositiveButton("Xóa", (dialog, which) -> deleteTaskFromFirebase(deletedTask, position))
                            .setNegativeButton("Hủy", (dialog, which) -> adapter.notifyItemChanged(position))
                            .show();
                }
            }

            @Override
            public void onTaskEdit(int position) {
                if (displayList.get(position) instanceof Task) {
                    Task task = (Task) displayList.get(position);
                    Toast.makeText(MainActivity.this, "Sửa task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onTaskClick(int position) {
                if (displayList.get(position) instanceof Task) {
                    Task task = (Task) displayList.get(position);
                    Intent intent = new Intent(MainActivity.this, com.example.todoapp.TaskDetail.TaskDetailActivity.class);
                    // (Giữ nguyên putExtra)
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
                if (position < 0 || position >= displayList.size() || !(displayList.get(position) instanceof Task)) {
                    return;
                }

                Task task = (Task) displayList.get(position);
                task.setCompleted(isChecked);

                taskRepository.updateTask(task)
                        .addOnSuccessListener(aVoid -> {
                            // ⭐️ YÊU CẦU 2: Sắp xếp lại danh sách
                            updateGroupedList();
                        })
                        .addOnFailureListener(e -> {
                            task.setCompleted(!isChecked);
                            adapter.notifyItemChanged(position); // Hoàn tác
                            Toast.makeText(MainActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
                        });
            }

            // ⭐️ YÊU CẦU 3: Xử lý click vào Header
            @Override
            public void onHeaderClick(int position) {
                if (displayList.get(position) instanceof DateHeader) {
                    DateHeader header = (DateHeader) displayList.get(position);
                    // Lật trạng thái
                    boolean isExpanded = groupExpansionState.getOrDefault(header.title, true);
                    groupExpansionState.put(header.title, !isExpanded);
                    // Xây dựng lại danh sách
                    updateGroupedList();
                }
            }
        });

        recyclerView.setAdapter(adapter);

        setupSearchListener();
        setupStaticFilterChips();

        addTask.setOnClickListener(v -> openAddTask());
        loadCategoriesAndTasks();
        firebaseAuth = new FirebaseAuthRepository();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoriesAndTasks();
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                updateGroupedList();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchQuery = "";
        });
    }

    // (Các hàm setupStaticFilterChips, loadCategoriesAndTasks, createCategoryChips,
    // selectChip, resetChipSelection giữ nguyên, không cần thay đổi)

    // ... (Giữ nguyên các hàm từ setupStaticFilterChips đến resetChipSelection) ...
    private void setupStaticFilterChips() {
        chipHighPriority.setOnClickListener(v -> selectChip(chipHighPriority, "priority", "high"));
        chipMediumPriority.setOnClickListener(v -> selectChip(chipMediumPriority, "priority", "medium"));
        chipLowPriority.setOnClickListener(v -> selectChip(chipLowPriority, "priority", "low"));
        chipCompleted.setOnClickListener(v -> selectChip(chipCompleted, "completion", "completed"));
        chipPending.setOnClickListener(v -> selectChip(chipPending, "completion", "pending"));
    }

    private void loadCategoriesAndTasks() {
        showLoading(true);

        categoryRepository.getAllCategories()
                .addOnSuccessListener(categories -> {
                    if (categories != null && !categories.isEmpty()) {
                        createCategoryChips(categories);
                    }
                    loadTasksFromFirebase();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadTasksFromFirebase();
                });
    }

    private void createCategoryChips(List<Category> categories) {
        for (TextView chip : categoryChipsMap.values()) {
            chipsContainer.removeView(chip);
        }
        categoryChipsMap.clear();

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            TextView chip = createCategoryChip(category);
            chipsContainer.addView(chip, i);
            categoryChipsMap.put(category.getCategoryId(), chip);
        }
    }

    private TextView createCategoryChip(Category category) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) (32 * getResources().getDisplayMetrics().density));
        params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        chip.setLayoutParams(params);
        chip.setText(category.getName());
        chip.setTextSize(13);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density), 0,
                (int) (16 * getResources().getDisplayMetrics().density), 0);
        chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        chip.setOnClickListener(v -> selectChip(chip, "category", category.getCategoryId()));
        return chip;
    }

    private void selectChip(TextView chip, String filterType, String filterValue) {
        if (currentSelectedChip == chip) {
            resetChipSelection(chip);
            currentFilterType = "none";
            currentFilterValue = "";
            loadTasksFromFirebase();
            return;
        }
        if (currentSelectedChip != null) {
            resetChipSelection(currentSelectedChip);
        }
        chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        currentSelectedChip = chip;
        currentFilterType = filterType;
        currentFilterValue = filterValue;
        loadTasksFromFirebase();
    }

    private void resetChipSelection(TextView chip) {
        chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }


    // ⭐️ HÀM MỚI: Lấy khóa (key) để sắp xếp nhóm
    private String getGroupKey(long dueDate) {
        if (dueDate == 0) {
            return "4_Không có ngày"; // Luôn ở cuối
        }

        Calendar now = Calendar.getInstance();
        Calendar taskDate = Calendar.getInstance();
        taskDate.setTimeInMillis(dueDate);

        now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0);
        taskDate.set(Calendar.HOUR_OF_DAY, 0); taskDate.set(Calendar.MINUTE, 0); taskDate.set(Calendar.SECOND, 0); taskDate.set(Calendar.MILLISECOND, 0);

        long diff = taskDate.getTimeInMillis() - now.getTimeInMillis();

        if (diff < 0) {
            return "1_Hôm trước";
        } else if (diff == 0) {
            return "2_Hôm nay";
        } else {
            return "3_Sắp tới";
        }
    }

    // ⭐️ HÀM MỚI: Lấy tiêu đề từ khóa
    private String getGroupTitleFromKey(String key) {
        // Trả về "Hôm trước", "Hôm nay", ... từ "1_Hôm trước", "2_Hôm nay", ...
        return key.substring(2);
    }

    // ⭐️ HÀM CẬP NHẬT: Logic sắp xếp và nhóm
    private void updateGroupedList() {
        List<Task> filteredList = new ArrayList<>();

        // 1. Lọc
        for (Task task : allTasks) {
            boolean matchSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String query = currentSearchQuery.toLowerCase();
                String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
                String description = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
                matchSearch = title.contains(query) || description.contains(query);
            }
            if (matchSearch) {
                filteredList.add(task);
            }
        }

        // 2. Sắp xếp (Sort)
        Collections.sort(filteredList, (t1, t2) -> {
            // Sắp xếp theo nhóm ngày
            String groupKey1 = getGroupKey(t1.getDueDate());
            String groupKey2 = getGroupKey(t2.getDueDate());
            int groupCompare = groupKey1.compareTo(groupKey2);
            if (groupCompare != 0) {
                return groupCompare;
            }

            // ⭐️ YÊU CẦU 2: Cùng nhóm -> Sắp xếp theo hoàn thành
            // (false - chưa xong) lên trước (true - đã xong)
            if (t1.isCompleted() != t2.isCompleted()) {
                return t1.isCompleted() ? 1 : -1;
            }

            // Cùng trạng thái -> Sắp xếp theo ngày (cụ thể)
            return Long.compare(t1.getDueDate(), t2.getDueDate());
        });

        // 3. Xây dựng danh sách hiển thị
        displayList.clear();
        String currentGroupKey = "";

        for (Task task : filteredList) {
            String taskGroupKey = getGroupKey(task.getDueDate());

            if (!taskGroupKey.equals(currentGroupKey)) {
                // Bắt đầu nhóm mới
                currentGroupKey = taskGroupKey;
                String title = getGroupTitleFromKey(taskGroupKey);
                // Lấy trạng thái (mặc định là mở)
                boolean isExpanded = groupExpansionState.getOrDefault(title, true);

                displayList.add(new DateHeader(title, isExpanded));
            }

            // ⭐️ YÊU CẦU 3: Chỉ thêm task nếu nhóm đang MỞ
            if (groupExpansionState.getOrDefault(getGroupTitleFromKey(taskGroupKey), true)) {
                displayList.add(task);
            }
        }

        adapter.notifyDataSetChanged();
    }


    private void loadTasksFromFirebase() {
        showLoading(true);

        com.google.android.gms.tasks.Task<List<Task>> taskQuery;

        if (currentFilterType.equals("none")) {
            taskQuery = taskRepository.getAllTasks();
        } else if (currentFilterType.equals("category")) {
            taskQuery = taskRepository.getTasksByCategory(currentFilterValue);
        } else if (currentFilterType.equals("priority")) {
            taskQuery = taskRepository.getTasksByPriority(currentFilterValue);
        } else if (currentFilterType.equals("completion")) {
            boolean isCompleted = currentFilterValue.equals("completed");
            taskQuery = taskRepository.getTasksByCompletionStatus(isCompleted);
        } else {
            taskQuery = taskRepository.getAllTasks();
        }

        taskQuery.addOnSuccessListener(tasks -> {
                    showLoading(false);
                    allTasks.clear();
                    if (tasks != null) allTasks.addAll(tasks);
                    updateGroupedList(); // ⬅️ Gọi hàm update mới
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteTaskFromFirebase(Task task, int position) {
        showLoading(true);
        taskRepository.deleteTask(task.getTaskId())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    allTasks.remove(task);
                    updateGroupedList(); // ⬅️ Gọi hàm update mới
                    Toast.makeText(this, "Đã xóa task", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Lỗi xóa task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void openAddTask() {
        startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
    }


}