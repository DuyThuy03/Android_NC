package com.example.todoapp;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.Auth.LoginActivity;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.example.todoapp.Repository.FirebaseCategoryRepository;
import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.model.Category;
import com.example.todoapp.model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private List<Task> allTasks;
    private ImageButton addTask, btnClearSearch;
    private ProgressBar progressBar;
    private EditText searchEditText;
    private LinearLayout chipsContainer;

    // Filter chips
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

        // Tìm HorizontalScrollView và lấy LinearLayout bên trong
        View searchBarLayout = findViewById(R.id.searchBarLayout);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView)
                ((LinearLayout) searchBarLayout).getChildAt(1);
        chipsContainer = (LinearLayout) horizontalScrollView.getChildAt(0);

        chipHighPriority = findViewById(R.id.chipHighPriority);
        chipMediumPriority = findViewById(R.id.chipMediumPriority);
        chipLowPriority = findViewById(R.id.chipLowPriority);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPending = findViewById(R.id.chipPending);

        categoryChipsMap = new HashMap<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();
        allTasks = new ArrayList<>();

        adapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskListener() {
            @Override
            public void onTaskDelete(int position) {
                Task deletedTask = taskList.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa task: " + deletedTask.getTitle() + "?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteTaskFromFirebase(deletedTask, position))
                        .setNegativeButton("Hủy", (dialog, which) -> adapter.notifyItemChanged(position))
                        .show();
            }

            @Override
            public void onTaskEdit(int position) {
                Toast.makeText(MainActivity.this, "Sửa task: " + taskList.get(position).getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTaskClick(int position) {
                Task task = taskList.get(position);
                Intent intent = new Intent(MainActivity.this, com.example.todoapp.TaskDetail.TaskDetailActivity.class);

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
        });

        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToRevealCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        setupSearchListener();
        setupStaticFilterChips();

        addTask.setOnClickListener(v -> openAddTask());

        loadCategoriesAndTasks();

        //Lưu trạng thái đăng nhập
        firebaseAuth = new FirebaseAuthRepository();

            if (firebaseAuth.getCurrentUser() != null) {
                // ✅ Đã đăng nhập → sang trang chính
                startActivity(new Intent(this, MainActivity.class));
            }



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
                applySearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchQuery = "";
        });
    }

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
        // Xóa các category chips cũ
        for (TextView chip : categoryChipsMap.values()) {
            chipsContainer.removeView(chip);
        }
        categoryChipsMap.clear();

        // Tạo chips mới cho từng category và thêm vào ĐẦU container
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
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (32 * getResources().getDisplayMetrics().density)
        );
        params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        chip.setLayoutParams(params);

        chip.setText(category.getName());
        chip.setTextSize(13);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                0,
                (int) (16 * getResources().getDisplayMetrics().density),
                0
        );

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

    private void applySearchFilter() {
        List<Task> filteredList = new ArrayList<>();

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

        taskList.clear();
        taskList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void loadTasksFromFirebase() {
        showLoading(true);

        if (currentFilterType.equals("none")) {
            taskRepository.getAllTasks()
                    .addOnSuccessListener(tasks -> {
                        showLoading(false);
                        allTasks.clear();
                        if (tasks != null) allTasks.addAll(tasks);
                        applySearchFilter();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (currentFilterType.equals("category")) {
            taskRepository.getTasksByCategory(currentFilterValue)
                    .addOnSuccessListener(tasks -> {
                        showLoading(false);
                        allTasks.clear();
                        if (tasks != null) allTasks.addAll(tasks);
                        applySearchFilter();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (currentFilterType.equals("priority")) {
            taskRepository.getTasksByPriority(currentFilterValue)
                    .addOnSuccessListener(tasks -> {
                        showLoading(false);
                        allTasks.clear();
                        if (tasks != null) allTasks.addAll(tasks);
                        applySearchFilter();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (currentFilterType.equals("completion")) {
            boolean isCompleted = currentFilterValue.equals("completed");
            taskRepository.getTasksByCompletionStatus(isCompleted)
                    .addOnSuccessListener(tasks -> {
                        showLoading(false);
                        allTasks.clear();
                        if (tasks != null) allTasks.addAll(tasks);
                        applySearchFilter();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteTaskFromFirebase(Task task, int position) {
        showLoading(true);
        taskRepository.deleteTask(task.getTaskId())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    allTasks.remove(task);
                    adapter.removeTask(position);
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

    // ==================== ADAPTER ====================
    public static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        private List<Task> taskList;
        private OnTaskListener listener;

        public interface OnTaskListener {
            void onTaskDelete(int position);
            void onTaskEdit(int position);
            void onTaskClick(int position);
        }

        public TaskAdapter(List<Task> taskList, OnTaskListener listener) {
            this.taskList = taskList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_swipe, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = taskList.get(position);
            holder.taskTitle.setText(task.getTitle());
            holder.taskSubtitle.setText(task.getDescription());

            holder.iconDelete.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION)
                    listener.onTaskDelete(pos);
            });

            holder.iconCheck.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Hoàn thành: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            });

            holder.iconShare.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Chia sẻ: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            });

            holder.cardTask.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION)
                    listener.onTaskClick(pos);
            });
        }

        @Override
        public int getItemCount() {
            return taskList.size();
        }

        public void removeTask(int position) {
            taskList.remove(position);
            notifyItemRemoved(position);
        }

        static class TaskViewHolder extends RecyclerView.ViewHolder {
            CardView cardTask;
            TextView taskTitle, taskSubtitle;
            ImageView iconDelete, iconShare, iconCheck;
            LinearLayout backgroundLeft, backgroundRight;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                cardTask = itemView.findViewById(R.id.cardTask);
                taskTitle = itemView.findViewById(R.id.taskTitle);
                taskSubtitle = itemView.findViewById(R.id.taskSubtitle);
                iconDelete = itemView.findViewById(R.id.iconDelete);
                iconShare = itemView.findViewById(R.id.iconShare);
                iconCheck = itemView.findViewById(R.id.iconCheck);
                backgroundLeft = itemView.findViewById(R.id.backgroundLeft);
                backgroundRight = itemView.findViewById(R.id.backgroundRight);
            }
        }
    }

    // ==================== SWIPE TO REVEAL ====================
    public class SwipeToRevealCallback extends ItemTouchHelper.SimpleCallback {
        public SwipeToRevealCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.notifyItemChanged(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(@NonNull Canvas c,
                                @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY,
                                int actionState,
                                boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;
                View cardTask = itemView.findViewById(R.id.cardTask);
                View bgLeft = itemView.findViewById(R.id.backgroundLeft);
                View bgRight = itemView.findViewById(R.id.backgroundRight);

                float maxSwipe = 250f;
                if (dX > maxSwipe) dX = maxSwipe;
                if (dX < -maxSwipe) dX = -maxSwipe;

                cardTask.setTranslationX(dX);

                if (dX > 0) {
                    bgRight.setVisibility(View.VISIBLE);
                    bgLeft.setVisibility(View.GONE);
                } else if (dX < 0) {
                    bgLeft.setVisibility(View.VISIBLE);
                    bgRight.setVisibility(View.GONE);
                } else {
                    bgLeft.setVisibility(View.GONE);
                    bgRight.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            View itemView = viewHolder.itemView;
            itemView.findViewById(R.id.cardTask).setTranslationX(0);
            itemView.findViewById(R.id.backgroundLeft).setVisibility(View.GONE);
            itemView.findViewById(R.id.backgroundRight).setVisibility(View.GONE);
        }
    }


}