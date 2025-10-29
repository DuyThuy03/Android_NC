package com.example.todoapp.TaskDetail;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.R;
import com.example.todoapp.Repository.FirebaseTaskRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    private ImageButton btnBack, btnShare, btnEdit, btnDelete;
    private TextView tvTitle, tvDescription, tvCategory, tvDueDate, tvCreatedAt, tvUpdatedAt;
    private Chip chipPriority, chipStatus;
    private EditText etNote;
    private RecyclerView rvSubtasks;
    private SubtaskAdapter subtaskAdapter;
    private List<String> subtaskList;
    private FirebaseFirestore db;
    private FirebaseTaskRepository taskRepository;

    private String taskId;
    private String currentTitle;
    private String currentDescription;
    private String currentPriority;
    private String currentCategoryId;
    private long currentDueDate;
    private boolean currentCompleted;
    private ArrayList<String> currentSubtasks;
    private ArrayList<String> currentNotes;

    // For category spinner
    private List<CategoryItem> categoryList;
    private ArrayAdapter<CategoryItem> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_task);

        // Khởi tạo Firestore và Repository
        db = FirebaseFirestore.getInstance();
        taskRepository = new FirebaseTaskRepository();
        categoryList = new ArrayList<>();

        // Ánh xạ header buttons
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        // Ánh xạ view
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        chipPriority = findViewById(R.id.chipPriority);
        chipStatus = findViewById(R.id.chipStatus);
        etNote = findViewById(R.id.etNote);
        rvSubtasks = findViewById(R.id.rvSubtasks);

        // RecyclerView
        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
        subtaskList = new ArrayList<>();
        subtaskAdapter = new SubtaskAdapter(subtaskList);
        rvSubtasks.setAdapter(subtaskAdapter);

        // Load categories
        loadCategories();

        // Nhận dữ liệu từ Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            taskId = extras.getString("taskId", "");
            currentTitle = extras.getString("title", "");
            currentDescription = extras.getString("description", "");
            currentCategoryId = extras.getString("categoryId", "");
            currentPriority = extras.getString("priority", "");
            currentDueDate = extras.getLong("dueDate", 0);
            long createdAt = extras.getLong("createdAt", 0);
            long updatedAt = extras.getLong("updatedAt", 0);
            currentCompleted = extras.getBoolean("completed", false);

            // Notes
            currentNotes = (ArrayList<String>) extras.getSerializable("notes");
            if (currentNotes != null && !currentNotes.isEmpty()) {
                etNote.setText(String.join("\n", currentNotes));
            } else {
                currentNotes = new ArrayList<>();
                etNote.setText("");
            }

            // Subtasks
            currentSubtasks = (ArrayList<String>) extras.getSerializable("subtasks");
            if (currentSubtasks == null) {
                currentSubtasks = new ArrayList<>();
            }

            // Hiển thị dữ liệu
            displayTaskDetail(currentTitle, currentDescription, currentPriority,
                    currentDueDate, createdAt, updatedAt, currentCompleted, currentSubtasks);

            // Lấy tên category
            if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
                fetchCategoryName(currentCategoryId);
            } else {
                tvCategory.setText("Danh mục: Không có");
            }
        } else {
            Toast.makeText(this, "Không có dữ liệu nhiệm vụ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Xử lý sự kiện header buttons
        setupHeaderButtons();

        // Tự động lưu note khi thay đổi
        setupNoteAutoSave();
    }

    private void loadCategories() {
        db.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    categoryList.add(new CategoryItem("", "Không có danh mục"));

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        if (name != null) {
                            categoryList.add(new CategoryItem(id, name));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDetailActivity", "Error loading categories", e);
                });
    }

    private void setupHeaderButtons() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnShare.setOnClickListener(v -> shareTask());
        btnEdit.setOnClickListener(v -> showFullEditDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void shareTask() {
        if (currentTitle == null || currentTitle.isEmpty()) {
            Toast.makeText(this, "Không có thông tin để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText = "📋 Nhiệm vụ: " + currentTitle + "\n\n";
        if (currentDescription != null && !currentDescription.isEmpty()) {
            shareText += "📝 Mô tả: " + currentDescription;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ nhiệm vụ");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }

    private void showFullEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chỉnh sửa nhiệm vụ");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);

        // Ánh xạ views
        EditText etEditTitle = dialogView.findViewById(R.id.etEditTitle);
        EditText etEditDescription = dialogView.findViewById(R.id.etEditDescription);
        RadioGroup rgEditPriority = dialogView.findViewById(R.id.rgEditPriority);
        RadioButton rbEditHigh = dialogView.findViewById(R.id.rbEditHigh);
        RadioButton rbEditMedium = dialogView.findViewById(R.id.rbEditMedium);
        RadioButton rbEditLow = dialogView.findViewById(R.id.rbEditLow);
        Spinner spinnerEditCategory = dialogView.findViewById(R.id.spinnerEditCategory);
        Button btnEditSelectDate = dialogView.findViewById(R.id.btnEditSelectDate);
        Button btnEditSelectTime = dialogView.findViewById(R.id.btnEditSelectTime);
        TextView tvEditSelectedDateTime = dialogView.findViewById(R.id.tvEditSelectedDateTime);
        CheckBox cbEditCompleted = dialogView.findViewById(R.id.cbEditCompleted);
        EditText etEditSubtasks = dialogView.findViewById(R.id.etEditSubtasks);
        EditText etEditNotes = dialogView.findViewById(R.id.etEditNotes);

        // Điền dữ liệu hiện tại
        etEditTitle.setText(currentTitle);
        etEditDescription.setText(currentDescription);

        // Set priority
        switch (currentPriority) {
            case "high":
                rbEditHigh.setChecked(true);
                break;
            case "medium":
                rbEditMedium.setChecked(true);
                break;
            case "low":
                rbEditLow.setChecked(true);
                break;
        }

        // Setup category spinner
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCategory.setAdapter(categoryAdapter);

        // Select current category
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).id.equals(currentCategoryId)) {
                spinnerEditCategory.setSelection(i);
                break;
            }
        }

        // Setup date time
        final Calendar selectedDateTime = Calendar.getInstance();
        if (currentDueDate > 0) {
            selectedDateTime.setTimeInMillis(currentDueDate);
            updateDateTimeDisplay(tvEditSelectedDateTime, selectedDateTime);
        }

        btnEditSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.MONTH, month);
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateTimeDisplay(tvEditSelectedDateTime, selectedDateTime);
                    },
                    selectedDateTime.get(Calendar.YEAR),
                    selectedDateTime.get(Calendar.MONTH),
                    selectedDateTime.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnEditSelectTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDateTime.set(Calendar.MINUTE, minute);
                        updateDateTimeDisplay(tvEditSelectedDateTime, selectedDateTime);
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        // Set completed status
        cbEditCompleted.setChecked(currentCompleted);

        // Set subtasks
        if (currentSubtasks != null && !currentSubtasks.isEmpty()) {
            etEditSubtasks.setText(String.join("\n", currentSubtasks));
        }

        // Set notes
        if (currentNotes != null && !currentNotes.isEmpty()) {
            etEditNotes.setText(String.join("\n", currentNotes));
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newTitle = etEditTitle.getText().toString().trim();
            String newDescription = etEditDescription.getText().toString().trim();

            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Tiêu đề không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get priority
            String newPriority = currentPriority; // Giữ nguyên nếu không chọn
            int selectedPriorityId = rgEditPriority.getCheckedRadioButtonId();
            if (selectedPriorityId == R.id.rbEditHigh) {
                newPriority = "high";
            } else if (selectedPriorityId == R.id.rbEditMedium) {
                newPriority = "medium";
            } else if (selectedPriorityId == R.id.rbEditLow) {
                newPriority = "low";
            }

            // Get category
            CategoryItem selectedCategory = (CategoryItem) spinnerEditCategory.getSelectedItem();
            String newCategoryId = selectedCategory != null ? selectedCategory.id : currentCategoryId;

            // Get due date
            long newDueDate = selectedDateTime.getTimeInMillis();

            // Get completed status
            boolean newCompleted = cbEditCompleted.isChecked();

            // Get subtasks - giữ nguyên nếu không nhập
            String subtasksText = etEditSubtasks.getText().toString().trim();
            List<String> newSubtasks = currentSubtasks; // Mặc định giữ nguyên
            if (!subtasksText.isEmpty()) {
                newSubtasks = new ArrayList<>();
                String[] subtaskLines = subtasksText.split("\n");
                for (String line : subtaskLines) {
                    if (!line.trim().isEmpty()) {
                        newSubtasks.add(line.trim());
                    }
                }
            }

            // Get notes - giữ nguyên nếu không nhập
            String notesText = etEditNotes.getText().toString().trim();
            List<String> newNotes = currentNotes; // Mặc định giữ nguyên
            if (!notesText.isEmpty()) {
                newNotes = new ArrayList<>();
                String[] noteLines = notesText.split("\n");
                for (String line : noteLines) {
                    if (!line.trim().isEmpty()) {
                        newNotes.add(line.trim());
                    }
                }
            }

            // Update to Firestore
            updateFullTaskInFirestore(newTitle, newDescription, newPriority,
                    newCategoryId, newDueDate, newCompleted, newSubtasks, newNotes);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void updateDateTimeDisplay(TextView tv, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tv.setText(sdf.format(calendar.getTime()));
    }

    private void updateFullTaskInFirestore(String newTitle, String newDescription,
                                           String newPriority, String newCategoryId,
                                           long newDueDate, boolean newCompleted,
                                           List<String> newSubtasks, List<String> newNotes) {
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID nhiệm vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chỉ update các field đã thay đổi
        Map<String, Object> updates = new HashMap<>();

        // Luôn update updatedAt
        updates.put("updatedAt", System.currentTimeMillis());

        // Kiểm tra từng field có thay đổi không
        if (!newTitle.equals(currentTitle)) {
            updates.put("title", newTitle);
        }

        if (!newDescription.equals(currentDescription)) {
            updates.put("description", newDescription);
        }

        if (!newPriority.equals(currentPriority)) {
            updates.put("priority", newPriority);
        }

        if (!newCategoryId.equals(currentCategoryId)) {
            updates.put("categoryId", newCategoryId);
        }

        if (newDueDate != currentDueDate) {
            updates.put("dueDate", newDueDate);
        }

        if (newCompleted != currentCompleted) {
            updates.put("completed", newCompleted);
        }

        if (!newSubtasks.equals(currentSubtasks)) {
            updates.put("subtasks", newSubtasks);
        }

        if (!newNotes.equals(currentNotes)) {
            updates.put("notes", newNotes);
        }

        // Nếu không có gì thay đổi (chỉ có updatedAt)
        if (updates.size() == 1) {
            Toast.makeText(this, "Không có thay đổi nào", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks").document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local variables
                    currentTitle = newTitle;
                    currentDescription = newDescription;
                    currentPriority = newPriority;
                    currentCategoryId = newCategoryId;
                    currentDueDate = newDueDate;
                    currentCompleted = newCompleted;
                    currentSubtasks = new ArrayList<>(newSubtasks);
                    currentNotes = new ArrayList<>(newNotes);

                    // Update UI
                    displayTaskDetail(newTitle, newDescription, newPriority,
                            newDueDate, 0, System.currentTimeMillis(),
                            newCompleted, newSubtasks);

                    // Update notes display
                    if (!newNotes.isEmpty()) {
                        etNote.setText(String.join("\n", newNotes));
                    } else {
                        etNote.setText("");
                    }

                    // Update category name
                    if (newCategoryId != null && !newCategoryId.isEmpty()) {
                        fetchCategoryName(newCategoryId);
                    } else {
                        tvCategory.setText("Danh mục: Không có");
                    }

                    Toast.makeText(this, "Đã cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDetailActivity", "Error updating task", e);
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupNoteAutoSave() {
        etNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveNoteToFirestore();
            }
        });
    }

    private void saveNoteToFirestore() {
        if (taskId == null || taskId.isEmpty()) return;

        String noteText = etNote.getText().toString().trim();
        List<String> notesList = new ArrayList<>();

        if (!noteText.isEmpty()) {
            String[] lines = noteText.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    notesList.add(line.trim());
                }
            }
        }

        // Chỉ update nếu có thay đổi
        if (!notesList.equals(currentNotes)) {
            currentNotes = new ArrayList<>(notesList);

            db.collection("tasks").document(taskId)
                    .update("notes", notesList, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TaskDetailActivity", "Notes saved successfully");
                        // Cập nhật thời gian hiển thị
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        tvUpdatedAt.setText("Cập nhật: " + sdf.format(new Date()));
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TaskDetailActivity", "Error saving notes", e);
                    });
        }
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTask())
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTask() {
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID nhiệm vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDetailActivity", "Error deleting task", e);
                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCategoryName(String categoryId) {
        db.collection("categories").document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String categoryName = documentSnapshot.getString("name");
                        tvCategory.setText("Danh mục: " +
                                (categoryName != null && !categoryName.isEmpty() ? categoryName : "Không có"));
                    } else {
                        tvCategory.setText("Danh mục: Không có");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDetailActivity", "Error fetching category", e);
                    tvCategory.setText("Danh mục: Không có");
                });
    }

    private void displayTaskDetail(String title, String description, String priority,
                                   long dueDate, long createdAt, long updatedAt,
                                   boolean completed, List<String> subtasks) {

        tvTitle.setText(title);
        tvDescription.setText(description);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDueDate.setText("Hạn: " + (dueDate > 0 ? sdf.format(new Date(dueDate)) : "Không có"));

        if (createdAt > 0) {
            tvCreatedAt.setText("Tạo lúc: " + sdf.format(new Date(createdAt)));
        }
        if (updatedAt > 0) {
            tvUpdatedAt.setText("Cập nhật: " + sdf.format(new Date(updatedAt)));
        }

        chipPriority.setText(getPriorityText(priority));
        setPriorityColor(chipPriority, priority);

        String status = completed ? "completed" : "in_progress";
        chipStatus.setText(getStatusText(status));
        setStatusColor(chipStatus, status);

        subtaskList.clear();
        subtaskList.addAll(subtasks);
        if (subtaskList.isEmpty()) {
            subtaskList.add("Không có công việc con");
        }
        subtaskAdapter.notifyDataSetChanged();
    }

    private String getPriorityText(String priority) {
        switch (priority) {
            case "high":
                return "Ưu tiên cao";
            case "medium":
                return "Ưu tiên trung bình";
            case "low":
                return "Ưu tiên thấp";
            default:
                return "Không xác định";
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "pending":
                return "Đang chờ";
            case "in_progress":
                return "Đang thực hiện";
            case "completed":
                return "Hoàn thành";
            default:
                return status;
        }
    }

    private void setPriorityColor(Chip chip, String priority) {
        switch (priority) {
            case "high":
                chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                chip.setTextColor(Color.WHITE);
                break;
            case "medium":
                chip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                chip.setTextColor(Color.WHITE);
                break;
            case "low":
                chip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                chip.setTextColor(Color.WHITE);
                break;
            default:
                chip.setChipBackgroundColorResource(android.R.color.darker_gray);
                chip.setTextColor(Color.WHITE);
        }
    }

    private void setStatusColor(Chip chip, String status) {
        if ("completed".equals(status)) {
            chip.setChipBackgroundColorResource(android.R.color.holo_green_light);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setChipBackgroundColorResource(android.R.color.darker_gray);
            chip.setTextColor(Color.WHITE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNoteToFirestore();
    }

    // === Category Item Class ===
    private static class CategoryItem {
        String id;
        String name;

        CategoryItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // === Adapter cho Subtasks ===
    private class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {
        private final List<String> subtasks;

        SubtaskAdapter(List<String> subtasks) {
            this.subtasks = subtasks;
        }

        @Override
        public SubtaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subtask, parent, false);
            return new SubtaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SubtaskViewHolder holder, int position) {
            String subtask = subtasks.get(position);
            holder.cbSubtask.setText(subtask);
        }

        @Override
        public int getItemCount() {
            return subtasks.size();
        }

        class SubtaskViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSubtask;
            SubtaskViewHolder(View itemView) {
                super(itemView);
                cbSubtask = itemView.findViewById(R.id.cbSubtask);
            }
        }
    }
}