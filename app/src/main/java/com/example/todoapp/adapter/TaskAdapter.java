package com.example.todoapp.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat; // ⬅️ Thêm import

import com.example.todoapp.R;
import com.example.todoapp.model.DateHeader; // ⬅️ Thêm import
import com.example.todoapp.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    private List<Object> displayList;
    private OnTaskListener listener;

    public interface OnTaskListener {
        void onTaskDelete(int position);
        void onTaskEdit(int position);
        void onTaskClick(int position);
        void onTaskCheckChanged(int position, boolean isChecked);
        void onHeaderClick(int position); // ⬅️ THÊM HÀM MỚI
    }

    public TaskAdapter(List<Object> displayList, OnTaskListener listener) {
        this.displayList = displayList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // ⭐️ THAY ĐỔI: Kiểm tra bằng class DateHeader
        if (displayList.get(position) instanceof DateHeader) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_TASK;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            // --- ⭐️ Bind Header (LOGIC MỚI) ---
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            DateHeader header = (DateHeader) displayList.get(position);

            headerHolder.tvHeaderTitle.setText(header.title);

            // Đặt icon mũi tên (lên/xuống)
            if (header.isExpanded) {
                headerHolder.ivExpandIcon.setImageResource(R.drawable.ic_priority_high); // Mũi tên xuống
            } else {
                headerHolder.ivExpandIcon.setImageResource(R.drawable.ic_priority_low); // Mũi tên lên
            }

            // Thêm listener cho toàn bộ header
            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHeaderClick(currentPosition);
                }
            });

        } else {
            // --- Bind Task ---
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            Task task = (Task) displayList.get(position);

            taskHolder.taskTitle.setText(task.getTitle());
            taskHolder.taskSubtitle.setText(task.getDescription());

            taskHolder.taskCheckbox.setOnCheckedChangeListener(null);
            taskHolder.taskCheckbox.setChecked(task.isCompleted());

            // ⭐️ YÊU CẦU 1: Mờ đi và gạch ngang
            if (task.isCompleted()) {
                taskHolder.taskTitle.setPaintFlags(taskHolder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskHolder.taskSubtitle.setPaintFlags(taskHolder.taskSubtitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskHolder.cardTask.setAlpha(0.6f); // Mờ đi
            } else {
                taskHolder.taskTitle.setPaintFlags(taskHolder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskHolder.taskSubtitle.setPaintFlags(taskHolder.taskSubtitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskHolder.cardTask.setAlpha(1.0f); // Rõ lại
            }

            taskHolder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int currentPosition = taskHolder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskCheckChanged(currentPosition, isChecked);
                }
            });

            taskHolder.cardTask.setOnClickListener(v -> {
                int pos = taskHolder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null)
                    listener.onTaskClick(pos);
            });

            // Ẩn icon category (cặp)
            taskHolder.iconCategory.setVisibility(View.GONE);

            // Đặt màu cờ theo priority
            String priority = task.getPriority();
            if (priority != null) {
                taskHolder.iconFlag.setVisibility(View.VISIBLE);
                switch (priority) {
                    case "high":
                        taskHolder.iconFlag.setImageResource(R.drawable.ic_flag_red);
                        break;
                    case "medium":
                        taskHolder.iconFlag.setImageResource(R.drawable.ic_flag_yellow);
                        break;
                    case "low":
                        taskHolder.iconFlag.setImageResource(R.drawable.ic_flag_green);
                        break;
                    default:
                        taskHolder.iconFlag.setVisibility(View.GONE);
                        break;
                }
            } else {
                taskHolder.iconFlag.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // ViewHolder cho Task
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardTask;
        TextView taskTitle, taskSubtitle;
        CheckBox taskCheckbox;
        ImageView iconFlag, iconCategory;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskSubtitle = itemView.findViewById(R.id.taskSubtitle);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            iconFlag = itemView.findViewById(R.id.iconFlag);
            iconCategory = itemView.findViewById(R.id.iconCategory);
        }
    }

    // ViewHolder cho Header
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        ImageView ivExpandIcon;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
        }
    }
}