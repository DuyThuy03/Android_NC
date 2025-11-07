package com.example.todoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

import com.example.todoapp.adapter.TaskAdapter;
import com.example.todoapp.Auth.LoginActivity;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.example.todoapp.Repository.FirebaseCategoryRepository;
import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.model.Category;
import com.example.todoapp.model.DateHeader;
import com.example.todoapp.model.Task;
import com.example.todoapp.model.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;

    private List<Object> displayList;
    private List<Task> allTasks;

    private ImageButton addTask, btnClearSearch, btncalendar;
    private ProgressBar progressBar;
    private EditText searchEditText;
    private LinearLayout chipsContainer;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navEmail;
    private ImageView navAvatarImage;
    private FirebaseFirestore db;

    private TextView chipHighPriority, chipMediumPriority, chipLowPriority;
    private TextView chipCompleted, chipPending, chipAll;
    private TextView currentSelectedChip;
    private Map<String, TextView> categoryChipsMap;

    private FirebaseTaskRepository taskRepository;
    private FirebaseCategoryRepository categoryRepository;
    private FirebaseAuthRepository firebaseAuth;

    private String currentFilterType = "none";
    private String currentFilterValue = "";
    private String currentSearchQuery = "";

    private Map<String, Boolean> groupExpansionState = new HashMap<>();

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private ListenerRegistration taskListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskRepository = new FirebaseTaskRepository();
        categoryRepository = new FirebaseCategoryRepository();
        firebaseAuth = new FirebaseAuthRepository();
        db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        recyclerView = findViewById(R.id.tasksRecyclerView);
        addTask = findViewById(R.id.nav_add);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btncalendar = findViewById(R.id.nav_calendar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        setupNavigationDrawer();

        btncalendar.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        View searchBarLayout = findViewById(R.id.searchBarLayout);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView)
                ((LinearLayout) searchBarLayout).getChildAt(1);
        chipsContainer = (LinearLayout) horizontalScrollView.getChildAt(0);

        chipAll = findViewById(R.id.All);
        chipHighPriority = findViewById(R.id.chipHighPriority);
        chipMediumPriority = findViewById(R.id.chipMediumPriority);
        chipLowPriority = findViewById(R.id.chipLowPriority);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPending = findViewById(R.id.chipPending);

        categoryChipsMap = new HashMap<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        displayList = new ArrayList<>();
        allTasks = new ArrayList<>();

        adapter = new TaskAdapter(displayList, new TaskAdapter.OnTaskListener() {
            @Override
            public void onTaskDelete(int position) {
                if (displayList.get(position) instanceof Task) {
                    Task deletedTask = (Task) displayList.get(position);
                    showDeleteConfirmation(deletedTask, position);
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

                if (isChecked) {
                    NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId());
                } else {
                    NotificationScheduler.scheduleNotification(
                            getApplicationContext(),
                            task.getDueDate(),
                            task.getTaskId(),
                            task.getTitle(),
                            "Công việc của bạn sắp đến hạn!"
                    );
                }

                taskRepository.updateTask(task)
                        .addOnSuccessListener(aVoid -> {
                            updateGroupedList();
                            saveTasksForWidget(allTasks);
                            notifyWidgetDataChanged();
                        })
                        .addOnFailureListener(e -> {
                            task.setCompleted(!isChecked);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(MainActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onHeaderClick(int position) {
                if (displayList.get(position) instanceof DateHeader) {
                    DateHeader header = (DateHeader) displayList.get(position);
                    boolean isExpanded = groupExpansionState.getOrDefault(header.title, true);
                    groupExpansionState.put(header.title, !isExpanded);
                    updateGroupedList();
                }
            }
        });

        recyclerView.setAdapter(adapter);

        // ⭐ THÊM SWIPE TO DELETE
        setupSwipeToDelete();

        setupSearchListener();
        setupStaticFilterChips();

        addTask.setOnClickListener(v -> openAddTask());

        loadUserInfo();
        loadCategoriesAndTasks();

        requestNotificationPermission();
    }

    // ⭐ HÀM MỚI: Setup Swipe to Delete
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.RED);
            private final Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (displayList.get(position) instanceof Task) {
                    Task task = (Task) displayList.get(position);
                    showDeleteConfirmation(task, position);
                }
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Chỉ cho phép swipe với Task, không cho Header
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < displayList.size() && displayList.get(position) instanceof Task) {
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }
                return 0;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (dX < 0) {
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    int deleteIconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int deleteIconTop = itemView.getTop() + deleteIconMargin;
                    int deleteIconBottom = deleteIconTop + deleteIcon.getIntrinsicHeight();
                    int deleteIconLeft = itemView.getRight() - deleteIconMargin - deleteIcon.getIntrinsicWidth();
                    int deleteIconRight = itemView.getRight() - deleteIconMargin;

                    deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(Task task, int position) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task: " + task.getTitle() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTaskFromFirebase(task, position))
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Khôi phục lại item khi hủy
                    adapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);

        android.util.Log.d("DEBUG_NAV", "HeaderView: " + headerView);

        navUsername = headerView.findViewById(R.id.navUsername);
        navEmail = headerView.findViewById(R.id.navEmail);
        navAvatarImage = headerView.findViewById(R.id.navAvatarImage);

        android.util.Log.d("DEBUG_NAV", "navUsername: " + navUsername);
        android.util.Log.d("DEBUG_NAV", "navEmail: " + navEmail);

        if (navUsername == null) {
            android.util.Log.e("DEBUG_NAV", "❌ navUsername is NULL!");
        }
        if (navEmail == null) {
            android.util.Log.e("DEBUG_NAV", "❌ navEmail is NULL!");
        }

        ImageButton menuButton = findViewById(R.id.nav_menu);
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadUserInfo() {
        String uid = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;

        android.util.Log.d("DEBUG_USER", "Starting loadUserInfo, UID: " + uid);

        if (uid != null) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");

                            android.util.Log.d("DEBUG_USER", "✅ Username from Firestore: [" + username + "]");
                            android.util.Log.d("DEBUG_USER", "✅ Email from Firestore: [" + email + "]");

                            android.util.Log.d("DEBUG_USER", "navUsername TextView: " + navUsername);
                            android.util.Log.d("DEBUG_USER", "navEmail TextView: " + navEmail);

                            if (navUsername != null) {
                                navUsername.setText(username != null ? username : "User");
                                android.util.Log.d("DEBUG_USER", "✅ Set username thành công!");
                            } else {
                                android.util.Log.e("DEBUG_USER", "❌ navUsername is NULL, cannot set text!");
                            }

                            if (navEmail != null) {
                                navEmail.setText(email != null ? email : "");
                                android.util.Log.d("DEBUG_USER", "✅ Set email thành công!");
                            } else {
                                android.util.Log.e("DEBUG_USER", "❌ navEmail is NULL, cannot set text!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("DEBUG_USER", "❌ Error: " + e.getMessage());
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            Toast.makeText(this, "Thông tin cá nhân", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.nav_statistics) {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "Về ứng dụng", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    firebaseAuth.logout();
                    redirectToLogin();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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

    // ⭐ CẬP NHẬT: Thêm chip "All"
    private void setupStaticFilterChips() {
        chipAll.setOnClickListener(v -> selectChip(chipAll, "none", ""));
        chipHighPriority.setOnClickListener(v -> selectChip(chipHighPriority, "priority", "high"));
        chipMediumPriority.setOnClickListener(v -> selectChip(chipMediumPriority, "priority", "medium"));
        chipLowPriority.setOnClickListener(v -> selectChip(chipLowPriority, "priority", "low"));
        chipCompleted.setOnClickListener(v -> selectChip(chipCompleted, "completion", "completed"));
        chipPending.setOnClickListener(v -> selectChip(chipPending, "completion", "pending"));

        // Set "All" là selected mặc định
        selectChip(chipAll, "none", "");
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

    // ⭐ CẬP NHẬT: Category chips luôn thêm sau "All"
    private void createCategoryChips(List<Category> categories) {
        // Xóa các category chips cũ (không xóa chips cố định)
        for (TextView chip : categoryChipsMap.values()) {
            chipsContainer.removeView(chip);
        }
        categoryChipsMap.clear();

        // Thêm category chips sau vị trí của chipPending (chip cố định cuối cùng)
        int insertIndex = chipsContainer.indexOfChild(chipPending) + 1;

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            TextView chip = createCategoryChip(category);
            chipsContainer.addView(chip, insertIndex + i);
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
            return; // Không deselect chip đang chọn
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

    private String getGroupKey(long dueDate) {
        if (dueDate == 0) {
            return "4_Không có ngày";
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

    private String getGroupTitleFromKey(String key) {
        return key.substring(2);
    }

    private void updateGroupedList() {
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

        Collections.sort(filteredList, (t1, t2) -> {
            String groupKey1 = getGroupKey(t1.getDueDate());
            String groupKey2 = getGroupKey(t2.getDueDate());
            int groupCompare = groupKey1.compareTo(groupKey2);
            if (groupCompare != 0) {
                return groupCompare;
            }

            if (t1.isCompleted() != t2.isCompleted()) {
                return t1.isCompleted() ? 1 : -1;
            }

            return Long.compare(t1.getDueDate(), t2.getDueDate());
        });

        displayList.clear();
        String currentGroupKey = "";

        for (Task task : filteredList) {
            String taskGroupKey = getGroupKey(task.getDueDate());

            if (!taskGroupKey.equals(currentGroupKey)) {
                currentGroupKey = taskGroupKey;
                String title = getGroupTitleFromKey(taskGroupKey);
                boolean isExpanded = groupExpansionState.getOrDefault(title, true);
                displayList.add(new DateHeader(title, isExpanded));
            }

            if (groupExpansionState.getOrDefault(getGroupTitleFromKey(taskGroupKey), true)) {
                displayList.add(task);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void loadTasksFromFirebase() {
        showLoading(true);

        if (taskListener != null) {
            taskListener.remove();
        }

        String uid = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "anonymous";

        taskListener = taskRepository.getFilteredTasksListener(uid, currentFilterType, currentFilterValue,
                (value, error) -> {

                    showLoading(false);

                    if (error != null) {
                        android.util.Log.e("MainActivity", "Lỗi lắng nghe task: ", error);
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allTasks.clear();
                    if (value != null) {
                        allTasks.addAll(value.toObjects(Task.class));
                    }

                    updateGroupedList();

                    saveTasksForWidget(allTasks);
                    notifyWidgetDataChanged();
                });
    }

    private void deleteTaskFromFirebase(Task task, int position) {
        showLoading(true);

        NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId());

        taskRepository.deleteTask(task.getTaskId())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    allTasks.remove(task);
                    updateGroupedList();
                    Toast.makeText(this, "Đã xóa task", Toast.LENGTH_SHORT).show();

                    saveTasksForWidget(allTasks);
                    notifyWidgetDataChanged();
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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bạn đã từ chối quyền thông báo. Một số tính năng có thể bị hạn chế.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isToday(long milliseconds) {
        if (milliseconds == 0) return false;

        Calendar taskDate = Calendar.getInstance();
        taskDate.setTimeInMillis(milliseconds);

        Calendar now = Calendar.getInstance();

        return now.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR);
    }

    private void saveTasksForWidget(List<Task> tasks) {
        SharedPreferences prefs = getSharedPreferences(com.example.todoapp.widget.WidgetRemoteViewsFactory.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> todayTasksSet = new HashSet<>();
        if (tasks != null) {
            for (Task task : tasks) {
                if (isToday(task.getDueDate()) && !task.isCompleted()) {
                    todayTasksSet.add(task.getTitle());
                }
            }
        }

        editor.putStringSet(com.example.todoapp.widget.WidgetRemoteViewsFactory.PREFS_KEY_TASKS, todayTasksSet);
        editor.apply();
    }

    private void notifyWidgetDataChanged() {
        Intent intent = new Intent(this, com.example.todoapp.widget.TodayTasksWidgetProvider.class);
        intent.setAction(com.example.todoapp.widget.TodayTasksWidgetProvider.WIDGET_DATA_CHANGED);
        sendBroadcast(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }
}