package com.example.todoapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.Repository.FirebaseCategoryRepository;
import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.example.todoapp.model.Category;
import com.example.todoapp.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import com.example.todoapp.NotificationScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;

public class AddTaskActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilDescription, tilDueDate, tilCategory;
    private EditText etTitle, etDescription, etDueDate, etNote;
    private AutoCompleteTextView actCategory;
    private RadioGroup rgPriority;
    private RecyclerView rvSubtasks;
    private TextView tvEmptySubtasks;
    private FloatingActionButton fabAddSubtask;
    private Button btnSave, btnCancel;
    private ImageButton btnManageCategory;
    private Toolbar toolbar;

    private List<SubtaskInput> subtaskList;
    private SubtaskInputAdapter subtaskAdapter;

    private Calendar dueDateCalendar;
    private List<Category> categoryList = new ArrayList<>();
    private FirebaseCategoryRepository categoryRepo;
    private FirebaseTaskRepository taskRepo;
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initViews();

        categoryRepo = new FirebaseCategoryRepository();
        taskRepo = new FirebaseTaskRepository();

        loadCategoriesFromFirebase();
        setupSubtasksRecyclerView();
        setupDatePicker();
        setupButtons();
    }

    private void initViews() {
        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        tilDueDate = findViewById(R.id.tilDueDate);
        tilCategory = findViewById(R.id.tilCategory);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDueDate = findViewById(R.id.etDueDate);
        actCategory = findViewById(R.id.actCategory);
        rgPriority = findViewById(R.id.rgPriority);
        rvSubtasks = findViewById(R.id.rvSubtasks);
        tvEmptySubtasks = findViewById(R.id.tvEmptySubtasks);
        fabAddSubtask = findViewById(R.id.fabAddSubtask);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnManageCategory = findViewById(R.id.btnManageCategory);
        etNote = findViewById(R.id.note_input_box);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        dueDateCalendar = Calendar.getInstance();
    }

    private void loadCategoriesFromFirebase() {
        categoryRepo.getAllCategories()
                .addOnSuccessListener(categories -> {
                    categoryList.clear();

                    if (categories == null || categories.isEmpty()) {
                        List<String> emptyList = new ArrayList<>();
                        emptyList.add("Không có danh mục nào");
                        categoryAdapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line,
                                emptyList);
                        actCategory.setAdapter(categoryAdapter);
                        actCategory.setOnClickListener(v -> actCategory.showDropDown());
                        return;
                    }

                    categoryList.addAll(categories);
                    List<String> categoryNames = new ArrayList<>();
                    for (Category c : categories) {
                        categoryNames.add(c.getName());
                    }

                    categoryAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line,
                            categoryNames);
                    actCategory.setAdapter(categoryAdapter);
                    actCategory.setOnClickListener(v -> actCategory.showDropDown());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSubtasksRecyclerView() {
        subtaskList = new ArrayList<>();
        subtaskAdapter = new SubtaskInputAdapter(subtaskList);
        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
        rvSubtasks.setAdapter(subtaskAdapter);
        updateSubtaskVisibility();
    }

    private void updateSubtaskVisibility() {
        if (subtaskList.isEmpty()) {
            rvSubtasks.setVisibility(View.GONE);
            tvEmptySubtasks.setVisibility(View.VISIBLE);
        } else {
            rvSubtasks.setVisibility(View.VISIBLE);
            tvEmptySubtasks.setVisibility(View.GONE);
        }
    }

    private void setupDatePicker() {
        etDueDate.setFocusable(false);
        etDueDate.setOnClickListener(v -> showDateTimePicker());
        tilDueDate.setStartIconOnClickListener(v -> showDateTimePicker());
    }

    private void showDateTimePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    dueDateCalendar.set(Calendar.YEAR, year);
                    dueDateCalendar.set(Calendar.MONTH, month);
                    dueDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                dueDateCalendar.get(Calendar.YEAR),
                dueDateCalendar.get(Calendar.MONTH),
                dueDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    dueDateCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    dueDateCalendar.set(Calendar.MINUTE, minute);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    etDueDate.setText(sdf.format(dueDateCalendar.getTime()));
                },
                dueDateCalendar.get(Calendar.HOUR_OF_DAY),
                dueDateCalendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void setupButtons() {
        fabAddSubtask.setOnClickListener(v -> showAddSubtaskDialog());
        btnCancel.setOnClickListener(v -> finish());
        btnManageCategory.setOnClickListener(v -> showManageCategoryDialog());
        btnSave.setOnClickListener(v -> saveTask());
    }

    private void showAddSubtaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm công việc con");

        final EditText input = new EditText(this);
        input.setHint("Tên công việc con");
        builder.setView(input);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String subtaskTitle = input.getText().toString().trim();
            if (!subtaskTitle.isEmpty()) {
                subtaskList.add(new SubtaskInput(subtaskTitle, false));
                subtaskAdapter.notifyItemInserted(subtaskList.size() - 1);
                updateSubtaskVisibility();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String catName = actCategory.getText().toString().trim();
        String note = etNote.getText().toString().trim(); // Không bắt buộc

        // Kiểm tra các trường bắt buộc (không bao gồm note)
        if (title.isEmpty() || desc.isEmpty() || catName.isEmpty() ||
                etDueDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        long dueDate = dueDateCalendar.getTimeInMillis();

        // 🔽 THÊM DÒNG NÀY 🔽
        long triggerTime_5Hour = dueDate - (5 * 60 * 60 * 1000); // 5 giờ

        // Kiểm tra thời gian (cho phép 1 phút đệm)
        if (dueDate <= System.currentTimeMillis() - 60000) {
            Toast.makeText(this, "Vui lòng chọn ngày giờ ở tương lai", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryId = null;
        for (Category c : categoryList) {
            if (c.getName().equals(catName)) {
                categoryId = c.getCategoryId();
                break;
            }
        }

        if (categoryId == null) {
            Toast.makeText(this, "Danh mục không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "medium";
        int id = rgPriority.getCheckedRadioButtonId();
        if (id == R.id.rbLow) priority = "low";
        else if (id == R.id.rbHigh) priority = "high";

        // Chuẩn bị subtasks
        List<String> subtasks = new ArrayList<>();
        for (SubtaskInput s : subtaskList) {
            subtasks.add(s.title);
        }

        // Chuẩn bị notes - nếu rỗng thì để list rỗng
        List<String> notes = new ArrayList<>();
        if (!note.isEmpty()) {
            notes.add(note);
        }

        // Tạo Task với trạng thái mặc định là Pending (false = chưa hoàn thành)
        Task task = new Task(
                null,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                title,
                desc,
                dueDate,
                priority,
                categoryId,
                false, // Mặc định là Pending (chưa hoàn thành)
                subtasks,
                notes,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null
        );

        taskRepo.addTask(task)
                .addOnSuccessListener(taskId -> {
                    Toast.makeText(this, "Đã lưu nhiệm vụ", Toast.LENGTH_SHORT).show();

                    // 🔽 CẬP NHẬT LOGIC LÊN LỊCH 🔽

                    // 1. Đặt lịch thông báo LÚC ĐẾN HẠN
                    NotificationScheduler.scheduleNotification(
                            getApplicationContext(),
                            dueDate, // Lịch 1: lúc đến hạn
                            taskId,
                            title,
                            "Công việc của bạn sắp đến hạn!",
                            NotificationScheduler.SUFFIX_MAIN // ⬅️ Suffix 1
                    );

                    // 2. Đặt lịch thông báo TRƯỚC 5 TIẾNG (nếu thời gian hợp lệ)
                    if (triggerTime_5Hour > System.currentTimeMillis()) {
                        NotificationScheduler.scheduleNotification(
                                getApplicationContext(),
                                triggerTime_5Hour, // Lịch 2: trước 5 tiếng
                                taskId,
                                title,
                                "Công việc sẽ đến hạn sau 5 tiếng!",
                                NotificationScheduler.SUFFIX_5_HOUR // ⬅️ Suffix 2
                        );
                    }
                    // 🔼 KẾT THÚC CẬP NHẬT 🔼

                    // Thông báo cho Widget
                    notifyWidgetDataChanged();

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("AddTask", "Error saving task", e);
                });
    }

    private void showManageCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quản lý danh mục");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_category, null);
        builder.setView(view);

        EditText etNewCategory = view.findViewById(R.id.etNewCategory);
        RecyclerView rv = view.findViewById(R.id.rvCategories);
        Button btnAdd = view.findViewById(R.id.btnAddCategory);

        CategoryAdapter adapter = new CategoryAdapter(categoryList, view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String name = etNewCategory.getText().toString().trim();
            if (name.isEmpty()) {
                etNewCategory.setError("Vui lòng nhập tên danh mục");
                return;
            }

            Category newCat = new Category(null, null, name, "#FFFFFF");

            categoryRepo.addCategory(newCat)
                    .addOnSuccessListener(categoryId -> {
                        categoryList.add(newCat);
                        adapter.notifyItemInserted(categoryList.size() - 1);

                        etNewCategory.setText("");

                        updateCategoryDropdown();

                        Log.d("AddTask", "Category added with ID: " + categoryId);
                    })
                    .addOnFailureListener(e -> {
                        etNewCategory.setError("Lỗi: " + e.getMessage());
                        Log.e("AddTask", "Failed to add category", e);
                    });
        });

        builder.setPositiveButton("Xong", (d, w) -> {});
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }

    private void updateCategoryDropdown() {
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categoryList) {
            categoryNames.add(c.getName());
        }

        categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames);
        actCategory.setAdapter(categoryAdapter);
    }

    private static class SubtaskInput {
        String title;
        boolean done;

        SubtaskInput(String title, boolean done) {
            this.title = title;
            this.done = done;
        }
    }

    private class SubtaskInputAdapter extends RecyclerView.Adapter<SubtaskInputAdapter.Holder> {
        private List<SubtaskInput> list;

        SubtaskInputAdapter(List<SubtaskInput> list) {
            this.list = list;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtask_input, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            h.tv.setText(list.get(pos).title);
            h.btn.setOnClickListener(v -> {
                list.remove(pos);
                notifyItemRemoved(pos);
                updateSubtaskVisibility();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tv;
            ImageButton btn;

            Holder(View v) {
                super(v);
                tv = v.findViewById(R.id.tvSubtaskTitle);
                btn = v.findViewById(R.id.btnRemove);
            }
        }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
        private List<Category> list;
        private View rootView;

        CategoryAdapter(List<Category> list, View rootView) {
            this.list = list;
            this.rootView = rootView;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup p, int vType) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            Category c = list.get(pos);
            h.tv.setText(c.getName());

            h.tv.setOnClickListener(v -> showEditCategoryDialog(c, pos));

            h.tv.setOnLongClickListener(v -> {
                new AlertDialog.Builder(AddTaskActivity.this)
                        .setTitle("Xóa danh mục?")
                        .setMessage("Bạn có chắc muốn xóa \"" + c.getName() + "\"?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            if (c.getCategoryId() == null) {
                                com.google.android.material.snackbar.Snackbar.make(
                                        rootView,
                                        "Lỗi: Danh mục không có ID",
                                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            categoryRepo.deleteCategory(c.getCategoryId())
                                    .addOnSuccessListener(unused -> {
                                        int position = list.indexOf(c);
                                        if (position >= 0) {
                                            list.remove(position);
                                            notifyItemRemoved(position);
                                            updateCategoryDropdown();

                                            com.google.android.material.snackbar.Snackbar.make(
                                                    rootView,
                                                    "Đã xóa \"" + c.getName() + "\"",
                                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                            ).show();
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            com.google.android.material.snackbar.Snackbar.make(
                                                    rootView,
                                                    "Lỗi xóa: " + e.getMessage(),
                                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                            ).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tv;

            Holder(View v) {
                super(v);
                tv = v.findViewById(android.R.id.text1);
            }
        }

        private void showEditCategoryDialog(Category category, int position) {
            View dialogView = LayoutInflater.from(AddTaskActivity.this)
                    .inflate(R.layout.dialog_edit_category, null);

            EditText etName = dialogView.findViewById(R.id.etCategoryName);

            etName.setText(category.getName());

            new AlertDialog.Builder(AddTaskActivity.this)
                    .setTitle("Chỉnh sửa danh mục")
                    .setView(dialogView)
                    .setPositiveButton("Lưu", (d, w) -> {
                        String newName = etName.getText().toString().trim();

                        if (newName.isEmpty()) {
                            Toast.makeText(AddTaskActivity.this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        category.setName(newName);

                        categoryRepo.updateCategory(category)
                                .addOnSuccessListener(unused -> {
                                    list.set(position, category);
                                    notifyItemChanged(position);
                                    updateCategoryDropdown();
                                    com.google.android.material.snackbar.Snackbar.make(
                                            rootView,
                                            "Đã cập nhật danh mục",
                                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                    ).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(AddTaskActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    private void notifyWidgetDataChanged() {
        Intent intent = new Intent(this, com.example.todoapp.widget.TodayTasksWidgetProvider.class);
        intent.setAction(com.example.todoapp.widget.TodayTasksWidgetProvider.WIDGET_DATA_CHANGED);
        sendBroadcast(intent);
    }
}