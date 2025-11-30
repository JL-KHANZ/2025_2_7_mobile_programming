package com.example.mobile_programming_2025_2.ui.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.TodoItem;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<TodoItem> todoList;
    private Context context;
    private OnItemCheckListener checkListener; // ⭐️ 추가됨: 클릭 감지 리스너

    // ⭐️ 인터페이스 정의 (Fragment와 통신하기 위함)
    public interface OnItemCheckListener {
        void onItemCheck(TodoItem item, boolean isChecked);
    }

    // ⭐️ 생성자 수정 (리스너를 매개변수로 받음)
    public TodoAdapter(List<TodoItem> todoList, OnItemCheckListener checkListener) {
        this.todoList = todoList;
        this.checkListener = checkListener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        // item_todo.xml 레이아웃 사용 (체크박스 왼쪽 디자인)
        View view = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        TodoItem item = todoList.get(position);

        holder.textView.setText(item.getText());

        // 리스너 충돌 방지 (재사용 문제 해결)
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked());

        // 현재 상태에 따라 스타일(취소선, 색상) 업데이트
        updateTextStyle(holder.textView, item.isChecked());

        // ⭐️ 클릭 이벤트 처리
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked); // 데이터 상태 변경
            updateTextStyle(holder.textView, isChecked); // UI 스타일 변경

            // Fragment에 "이거 체크됐어/풀렸어!" 하고 알려줌 -> 저장 로직 실행됨
            if (checkListener != null) {
                checkListener.onItemCheck(item, isChecked);
            }
        });
    }

    // 체크 여부에 따라 글자색 변경 (취소선 없음)
    private void updateTextStyle(TextView textView, boolean isChecked) {
        // 기존 취소선 제거 (항상 깨끗한 상태로 시작)
        textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        if (isChecked) {
            // 체크됨: 회색(#999999)
            textView.setTextColor(Color.parseColor("#999999"));
        } else {
            // 체크 안 됨: 검은색(#333333)
            textView.setTextColor(Color.parseColor("#333333"));
        }
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_todo);
            textView = itemView.findViewById(R.id.text_todo_content);
        }
    }
}