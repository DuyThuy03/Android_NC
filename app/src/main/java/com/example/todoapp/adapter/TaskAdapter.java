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
import androidx.core.content.ContextCompat;

import com.example.todoapp.R;
import com.example.todoapp.model.DateHeader;
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
        void onHeaderClick(int position);
    }

    public TaskAdapter(List<Object> displayList, OnTaskListener listener) {
        this.displayList = displayList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
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
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            DateHeader header = (DateHeader) displayList.get(position);

            headerHolder.tvHeaderTitle.setText(header.title);

            if (header.isExpanded) {
                headerHolder.ivExpandIcon.setImageResource(R.drawable.ic_priority_high);
            } else {
                headerHolder.ivExpandIcon.setImageResource(R.drawable.ic_priority_low);
            }

            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHeaderClick(currentPosition);
                }
            });

        } else {
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            Task task = (Task) displayList.get(position);

            taskHolder.taskTitle.setText(task.getTitle());
            taskHolder.taskSubtitle.setText(task.getDescription());

            taskHolder.taskCheckbox.setOnCheckedChangeListener(null);
            taskHolder.taskCheckbox.setChecked(task.isCompleted());

            // Kiểm tra task quá hạn
            boolean isOverdue = !task.isCompleted() &&
                    task.getDueDate() > 0 &&
                    task.getDueDate() < System.currentTimeMillis();

            // Set background màu đỏ nhạt cho task quá hạn
            if (isOverdue) {
                taskHolder.cardTask.setCardBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_light)
                );
            } else {
                taskHolder.cardTask.setCardBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.white)
                );
            }

            // Mờ đi và gạch ngang task đã hoàn thành
            if (task.isCompleted()) {
                taskHolder.taskTitle.setPaintFlags(taskHolder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskHolder.taskSubtitle.setPaintFlags(taskHolder.taskSubtitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskHolder.cardTask.setAlpha(0.6f);
            } else {
                taskHolder.taskTitle.setPaintFlags(taskHolder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskHolder.taskSubtitle.setPaintFlags(taskHolder.taskSubtitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskHolder.cardTask.setAlpha(1.0f);
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

            taskHolder.iconCategory.setVisibility(View.GONE);

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