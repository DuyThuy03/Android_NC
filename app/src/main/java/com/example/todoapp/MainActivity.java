package com.example.todoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
// 🔽 THÊM CÁC IMPORT NÀY 🔽
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import com.example.todoapp.widget.TodayTasksWidgetProvider;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // (Giữ nguyên tất cả các biến toàn cục...)
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
    private ListenerRegistration notificationListener; // ⬅️ ĐÃ ĐỔI TÊN

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

        // (Giữ nguyên code ánh xạ view...)
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
            // (Giữ nguyên onTaskDelete, onTaskEdit, onTaskClick)
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

            // (Giữ nguyên onTaskCheckChanged - đã được cập nhật ở câu trả lời trước
            // để xử lý 2 lịch (SUFFIX_MAIN, SUFFIX_5_HOUR) và gọi sendCompletionNotification)
            @Override
            public void onTaskCheckChanged(int position, boolean isChecked) {
                if (position < 0 || position >= displayList.size() || !(displayList.get(position) instanceof Task)) {
                    return;
                }
                Task task = (Task) displayList.get(position);
                boolean wasCompleted = task.isCompleted();
                task.setCompleted(isChecked);

                if (isChecked) {
                    NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId(), NotificationScheduler.SUFFIX_MAIN);
                    NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId(), NotificationScheduler.SUFFIX_5_HOUR);
                    if (!wasCompleted) {
                        sendCompletionNotification(task);
                    }
                } else {
                    long dueDate = task.getDueDate();
                    long triggerTime_5Hour = dueDate - (5 * 60 * 60 * 1000);
                    if (dueDate > System.currentTimeMillis()) {
                        NotificationScheduler.scheduleNotification(
                                getApplicationContext(), dueDate, task.getTaskId(), task.getTitle(),
                                "Công việc của bạn sắp đến hạn!", NotificationScheduler.SUFFIX_MAIN
                        );
                    }
                    if (triggerTime_5Hour > System.currentTimeMillis()) {
                        NotificationScheduler.scheduleNotification(
                                getApplicationContext(), triggerTime_5Hour, task.getTaskId(), task.getTitle(),
                                "Công việc sẽ đến hạn sau 5 tiếng!", NotificationScheduler.SUFFIX_5_HOUR
                        );
                    }
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

            // (Giữ nguyên onHeaderClick)
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
        setupSwipeToDelete();
        setupSearchListener();
        setupStaticFilterChips();
        addTask.setOnClickListener(v -> openAddTask());
        loadUserInfo();
        loadCategoriesAndTasks();

        // 🔽 THAY ĐỔI TÊN HÀM NÀY 🔽
        setupAppNotificationsListener();

        requestNotificationPermission();
    }

    // (Giữ nguyên setupSwipeToDelete)
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.RED);
            private final Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (displayList.get(position) instanceof Task) {
                    Task task = (Task) displayList.get(position);
                    showDeleteConfirmation(task, position);
                }
            }
            @Override public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < displayList.size() && displayList.get(position) instanceof Task) {
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }
                return 0;
            }
            @Override public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                if (dX < 0) {
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
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

    // (Giữ nguyên showDeleteConfirmation)
    private void showDeleteConfirmation(Task task, int position) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task: " + task.getTitle() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTaskFromFirebase(task, position))
                .setNegativeButton("Hủy", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    // (Giữ nguyên setupNavigationDrawer)
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        navUsername = headerView.findViewById(R.id.navUsername);
        navEmail = headerView.findViewById(R.id.navEmail);
        navAvatarImage = headerView.findViewById(R.id.navAvatarImage);
        ImageButton menuButton = findViewById(R.id.nav_menu);
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    // (Giữ nguyên loadUserInfo)
    private void loadUserInfo() {
        String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (uid != null) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");
                            if (navUsername != null) {
                                navUsername.setText(username != null ? username : "User");
                            }
                            if (navEmail != null) {
                                navEmail.setText(email != null ? email : "");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("DEBUG_USER", "❌ Error: " + e.getMessage());
                    });
        }
    }

    // (Giữ nguyên onNavigationItemSelected, showLogoutDialog, redirectToLogin, onBackPressed)
    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) Toast.makeText(this, "Thông tin cá nhân", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_statistics) startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        else if (id == R.id.nav_settings) Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_about) Toast.makeText(this, "Về ứng dụng", Toast.LENGTH_SHORT).show();
        else if (id == R.id.nav_add_widget) {
            requestPinWidget();
        }
        else if (id == R.id.nav_logout) showLogoutDialog();
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
    @Override public void onBackPressed() {
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
        // 🔽 THAY ĐỔI TÊN HÀM NÀY 🔽
        setupAppNotificationsListener();
    }

    // (Giữ nguyên setupSearchListener, setupStaticFilterChips, loadCategoriesAndTasks, createCategoryChips, createCategoryChip, selectChip, resetChipSelection)
    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                updateGroupedList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchQuery = "";
        });
    }
    private void setupStaticFilterChips() {
        chipAll.setOnClickListener(v -> selectChip(chipAll, "none", ""));
        chipHighPriority.setOnClickListener(v -> selectChip(chipHighPriority, "priority", "high"));
        chipMediumPriority.setOnClickListener(v -> selectChip(chipMediumPriority, "priority", "medium"));
        chipLowPriority.setOnClickListener(v -> selectChip(chipLowPriority, "priority", "low"));
        chipCompleted.setOnClickListener(v -> selectChip(chipCompleted, "completion", "completed"));
        chipPending.setOnClickListener(v -> selectChip(chipPending, "completion", "pending"));
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
    private void createCategoryChips(List<Category> categories) {
        for (TextView chip : categoryChipsMap.values()) {
            chipsContainer.removeView(chip);
        }
        categoryChipsMap.clear();
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
        if (currentSelectedChip == chip) return;
        if (currentSelectedChip != null) resetChipSelection(currentSelectedChip);
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

    // (Giữ nguyên getGroupKey, getGroupTitleFromKey, updateGroupedList, loadTasksFromFirebase)
    private String getGroupKey(long dueDate) {
        if (dueDate == 0) return "4_Không có ngày";
        Calendar now = Calendar.getInstance();
        Calendar taskDate = Calendar.getInstance();
        taskDate.setTimeInMillis(dueDate);
        now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0);
        taskDate.set(Calendar.HOUR_OF_DAY, 0); taskDate.set(Calendar.MINUTE, 0); taskDate.set(Calendar.SECOND, 0); taskDate.set(Calendar.MILLISECOND, 0);
        long diff = taskDate.getTimeInMillis() - now.getTimeInMillis();
        if (diff < 0) return "1_Hôm trước";
        else if (diff == 0) return "2_Hôm nay";
        else return "3_Sắp tới";
    }
    private String getGroupTitleFromKey(String key) { return key.substring(2); }
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
            if (matchSearch) filteredList.add(task);
        }
        Collections.sort(filteredList, (t1, t2) -> {
            String groupKey1 = getGroupKey(t1.getDueDate());
            String groupKey2 = getGroupKey(t2.getDueDate());
            int groupCompare = groupKey1.compareTo(groupKey2);
            if (groupCompare != 0) return groupCompare;
            if (t1.isCompleted() != t2.isCompleted()) return t1.isCompleted() ? 1 : -1;
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
        if (taskListener != null) taskListener.remove();
        String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : "anonymous";
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

    // (Giữ nguyên deleteTaskFromFirebase - đã được cập nhật ở câu trả lời trước
    // để hủy cả 2 lịch (SUFFIX_MAIN và SUFFIX_5_HOUR))
    private void deleteTaskFromFirebase(Task task, int position) {
        showLoading(true);
        NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId(), NotificationScheduler.SUFFIX_MAIN);
        NotificationScheduler.cancelNotification(getApplicationContext(), task.getTaskId(), NotificationScheduler.SUFFIX_5_HOUR);
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

    // (Giữ nguyên showLoading, openAddTask, requestNotificationPermission, onRequestPermissionsResult, isToday, saveTasksForWidget, notifyWidgetDataChanged)
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    private void openAddTask() {
        startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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


    // 🔽 HÀM NÀY GIỮ NGUYÊN (từ câu trả lời trước) 🔽
    /**
     * Ghi một tài liệu thông báo vào Firestore khi task chia sẻ được hoàn thành.
     */
    private void sendCompletionNotification(Task completedTask) {
        String currentUserId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        String taskCreatorId = completedTask.getUid();
        if (currentUserId == null) return;
        boolean isShared = completedTask.getMembers() != null && completedTask.getMembers().size() > 1;
        boolean isNotCreator = !currentUserId.equals(taskCreatorId);

        if (isShared && isNotCreator) {
            String notificationId = db.collection("notifications").document().getId();
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("notificationId", notificationId);
            notifData.put("taskId", completedTask.getTaskId());
            notifData.put("uid", taskCreatorId); // Gửi TỚI người tạo task
            notifData.put("type", "task_completed"); // Loại thông báo
            notifData.put("notificationTime", System.currentTimeMillis());
            notifData.put("isSent", false);
            notifData.put("completerUid", currentUserId); // AI là người hoàn thành
            notifData.put("taskTitle", completedTask.getTitle()); // Tiêu đề task

            db.collection("notifications").document(notificationId).set(notifData)
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Đã tạo tài liệu thông báo hoàn thành cho task: " + completedTask.getTaskId()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Lỗi tạo tài liệu thông báo", e));
        }
    }


    // 🔽 HÀM NÀY ĐƯỢC CẬP NHẬT ĐỂ XỬ LÝ 3 LOẠI THÔNG BÁO 🔽
    /**
     * Lắng nghe collection "notifications" trong Firestore.
     * Xử lý 3 loại:
     * 1. task_completed: Ai đó hoàn thành task của tôi.
     * 2. task_invitation: Ai đó mời tôi tham gia task.
     * 3. invitation_response: Ai đó phản hồi lời mời của tôi.
     */
    private void setupAppNotificationsListener() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("notifications")
                .whereEqualTo("uid", currentUserId)
                .whereEqualTo("isSent", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("MainActivity", "Lỗi lắng nghe thông báo", e);
                        return;
                    }
                    if (snapshots == null) return;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String type = doc.getString("type");
                        String docId = doc.getId();
                        if (type == null) continue;

                        // Đánh dấu là đã xử lý ngay lập tức
                        db.collection("notifications").document(docId).update("isSent", true);

                        switch (type) {
                            case "task_completed":
                                handleTaskCompletedNotification(doc);
                                break;
                            case "task_invitation":
                                handleTaskInvitation(doc);
                                break;
                            case "invitation_response":
                                handleInvitationResponse(doc);
                                break;
                        }
                    }
                });
    }

    /**
     * Xử lý khi ai đó hoàn thành task của tôi (Loại 1)
     */
    private void handleTaskCompletedNotification(QueryDocumentSnapshot doc) {
        String completerUid = doc.getString("completerUid");
        String taskTitle = doc.getString("taskTitle");

        if (completerUid != null) {
            db.collection("users").document(completerUid).get()
                    .addOnSuccessListener(userDoc -> {
                        String completerName = userDoc.exists() ? userDoc.getString("username") : "Một thành viên";
                        String message = completerName + " đã hoàn thành nhiệm vụ: " + taskTitle;
                        sendLocalNotification("Nhiệm vụ Hoàn Thành", message, doc.getId().hashCode());
                    });
        }
    }

    /**
     * Xử lý khi tôi được mời tham gia task (Loại 2)
     */
    private void handleTaskInvitation(QueryDocumentSnapshot doc) {
        String sharerName = doc.getString("sharerName");
        String taskTitle = doc.getString("taskTitle");
        String taskId = doc.getString("taskId");

        if (sharerName == null || taskTitle == null || taskId == null) return;

        showInvitationDialog(doc, sharerName, taskTitle, taskId);
    }

    /**
     * Xử lý khi ai đó phản hồi lời mời của tôi (Loại 3)
     */
    private void handleInvitationResponse(QueryDocumentSnapshot doc) {
        String inviteeName = doc.getString("inviteeName");
        String taskTitle = doc.getString("taskTitle");
        String response = doc.getString("response"); // "accepted" hoặc "declined"

        if (inviteeName == null || taskTitle == null || response == null) return;

        String responseText = "accepted".equals(response) ? "chấp nhận" : "từ chối";
        String message = inviteeName + " đã " + responseText + " lời mời tham gia task: " + taskTitle;
        sendLocalNotification("Phản hồi lời mời", message, doc.getId().hashCode());
    }

    /**
     * Hiển thị hộp thoại Chấp nhận/Từ chối lời mời
     */
    private void showInvitationDialog(QueryDocumentSnapshot invitationDoc, String sharerName, String taskTitle, String taskId) {
        new AlertDialog.Builder(this)
                .setTitle("Lời mời tham gia Task")
                .setMessage(sharerName + " mời bạn tham gia nhiệm vụ:\n\n\"" + taskTitle + "\"")
                .setPositiveButton("Chấp nhận", (dialog, which) -> {
                    String currentUserId = firebaseAuth.getCurrentUser().getUid();
                    // 1. Thêm user vào mảng members của task
                    db.collection("tasks").document(taskId)
                            .update("members", FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã tham gia task!", Toast.LENGTH_SHORT).show();
                                loadTasksFromFirebase(); // Tải lại danh sách task
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi chấp nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });

                    // 2. Gửi thông báo phản hồi
                    sendInvitationResponse(invitationDoc, "accepted");
                })
                .setNegativeButton("Từ chối", (dialog, which) -> {
                    // 1. Gửi thông báo phản hồi
                    sendInvitationResponse(invitationDoc, "declined");
                })
                .setCancelable(false) // Bắt buộc phải chọn
                .show();
    }

    /**
     * Gửi thông báo phản hồi (accepted/declined) cho người chia sẻ
     */
    private void sendInvitationResponse(QueryDocumentSnapshot invitationDoc, String status) {
        String myName = (navUsername != null && navUsername.getText() != null) ?
                navUsername.getText().toString() : "Người dùng";

        Map<String, Object> response = new HashMap<>();
        response.put("type", "invitation_response");
        response.put("uid", invitationDoc.getString("sharerUid")); // Gửi cho người mời
        response.put("inviteeName", myName); // Tên của tôi
        response.put("taskTitle", invitationDoc.getString("taskTitle"));
        response.put("response", status); // "accepted" hoặc "declined"
        response.put("isSent", false);
        response.put("timestamp", System.currentTimeMillis());

        db.collection("notifications").document().set(response)
                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Đã gửi phản hồi lời mời: " + status));
    }

    /**
     * Hàm tiện ích gửi thông báo local (tái sử dụng NotificationReceiver)
     */
    private void sendLocalNotification(String title, String message, int notificationId) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskTitle", title);
        intent.putExtra("taskMessage", message);
        intent.putExtra("notificationId", notificationId);
        sendBroadcast(intent);
    }

    // 🔼 KẾT THÚC CÁC HÀM MỚI 🔼


    @Override
    protected void onStop() {
        super.onStop();
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
        // 🔽 CẬP NHẬT KHỐI NÀY 🔽
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
        // 🔼 KẾT THÚC CẬP NHẬT 🔼
    }

    private void requestPinWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);

            // Lấy ComponentName của Widget Provider của bạn
            ComponentName myProvider = new ComponentName(this, TodayTasksWidgetProvider.class);

            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
                // Hiển thị hộp thoại hệ thống để xin phép ghim
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
            } else {
                // Fallback nếu launcher không hỗ trợ
                Toast.makeText(this, "Trình khởi chạy của bạn không hỗ trợ ghim widget.", Toast.LENGTH_LONG).show();
                showManualWidgetToast();
            }
        } else {
            // Fallback cho các phiên bản Android cũ
            showManualWidgetToast();
        }
    }

    /**
     * Hiển thị hướng dẫn thêm widget thủ công cho các phiên bản Android cũ
     * hoặc launcher không hỗ trợ.
     */
    private void showManualWidgetToast() {
        Toast.makeText(this, "Để thêm widget, vui lòng nhấn giữ màn hình chính và chọn 'Widgets'", Toast.LENGTH_LONG).show();
    }
}