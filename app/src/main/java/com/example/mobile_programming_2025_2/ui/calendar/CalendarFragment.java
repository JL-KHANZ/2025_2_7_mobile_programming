package com.example.mobile_programming_2025_2.ui.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity; // 추가
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_programming_2025_2.MainViewModel;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.Service.DailyEntryService;
import com.example.mobile_programming_2025_2.data.DailyEntry;
import com.example.mobile_programming_2025_2.data.TodoItem;
import com.example.mobile_programming_2025_2.databinding.FragmentCalendarBinding;
import com.example.mobile_programming_2025_2.ui.ColorMapping;
import com.example.mobile_programming_2025_2.ui.widgets.WidgetDailyEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private TextView textMonth;
    private ImageButton btnPrev, btnNext;

    private RecyclerView recyclerCalendar;
    private CalendarAdapter adapter;
    private int currentYear, currentMonth;

    private RecyclerView recyclerTodo;
    private TodoAdapter todoAdapter;
    private List<TodoItem> todoList = new ArrayList<>();

    private DailyEntryService dailyEntryService = new DailyEntryService();
    private Map<String, DailyEntry> monthDailyMap = new HashMap<>();

    private int selectedYear, selectedMonth, selectedDay;

    public interface OnDayClickListener {
        void onDayClick(DayCell cell);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CalendarViewModel calendarViewModel =
                new ViewModelProvider(this).get(CalendarViewModel.class);

        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textMonth = view.findViewById(R.id.text_month);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);
        recyclerCalendar = view.findViewById(R.id.recycler_calendar);
        recyclerTodo = view.findViewById(R.id.recycler_todo);

        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

        selectedYear = currentYear;
        selectedMonth = currentMonth;
        selectedDay = today.get(Calendar.DAY_OF_MONTH);

        todoAdapter = new TodoAdapter(todoList, (item, isChecked) -> {
            saveTodoState(item.getText(), isChecked);
        });
        recyclerTodo.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTodo.setAdapter(todoAdapter);

        recyclerCalendar.setLayoutManager(new GridLayoutManager(getActivity(), 7));
        adapter = new CalendarAdapter(new OnDayClickListener() {
            @Override
            public void onDayClick(DayCell cell) {
                if (cell.isCurrentMonth) {
                    if (cell.year == selectedYear && cell.month == selectedMonth && cell.day == selectedDay) {
                        showDayDialog(cell);
                    } else {
                        selectedYear = cell.year;
                        selectedMonth = cell.month;
                        selectedDay = cell.day;

                        adapter.notifyDataSetChanged();
                        updateTodoListForSelectedDate(cell);
                    }
                }
            }
        });
        recyclerCalendar.setAdapter(adapter);

        renderMonth(currentYear, currentMonth);

        btnPrev.setOnClickListener(v -> {
            if (currentMonth == 0) { currentMonth = 11; currentYear--; }
            else { currentMonth--; }
            renderMonth(currentYear, currentMonth);
        });
        btnNext.setOnClickListener(v -> {
            if (currentMonth == 11) { currentMonth = 0; currentYear++; }
            else { currentMonth++; }
            renderMonth(currentYear, currentMonth);
        });
    }

    private void renderMonth(int year, int month) {
        String text = String.format(Locale.getDefault(), "%04d년 %02d월", year, month + 1);
        textMonth.setText(text);
        loadMonthDailyEntries(year, month);
    }

    private void loadMonthDailyEntries(int year, int month) {
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, 1);
        Calendar endCal = (Calendar) startCal.clone();
        endCal.set(year, month, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(startCal.getTime());
        String endDate = sdf.format(endCal.getTime());

        dailyEntryService.getByPeriod(startDate, endDate,
                dailyMap -> {
                    monthDailyMap.clear();
                    monthDailyMap.putAll(dailyMap);

                    List<DayCell> cells = buildMonthCells(year, month);
                    adapter.submit(cells);

                    String selectedKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    updateTodoList(monthDailyMap.get(selectedKey));
                },
                e -> {
                    monthDailyMap.clear();
                    List<DayCell> cells = buildMonthCells(year, month);
                    adapter.submit(cells);
                }
        );
    }

    private void saveTodoState(String todoContent, boolean isChecked) {
        String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
        String key = dateKey + "_" + todoContent;
        SharedPreferences prefs = requireContext().getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, isChecked).apply();
    }

    private boolean loadTodoState(String dateKey, String todoContent) {
        String key = dateKey + "_" + todoContent;
        SharedPreferences prefs = requireContext().getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    private void updateTodoListForSelectedDate(DayCell cell) {
        String key = String.format(Locale.getDefault(), "%04d-%02d-%02d", cell.year, cell.month + 1, cell.day);
        updateTodoList(monthDailyMap.get(key));
    }

    // ⭐️ [수정됨] 체크박스 상태 불러오기 로직 수정!
    private void updateTodoList(DailyEntry entry) {
        todoList.clear();

        // 현재 선택된 날짜의 Key 생성
        String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);

        if (entry != null && entry.feedback != null) {
            Map<String, String> feedback = entry.feedback;
            String r1 = feedback.get("생활 루틴 1");
            String r2 = feedback.get("생활 루틴 2");

            // dateKey를 정확히 전달하여 저장된 상태를 불러옴
            if (r1 != null && !r1.isEmpty()) todoList.add(new TodoItem(r1, loadTodoState(dateKey, r1)));
            if (r2 != null && !r2.isEmpty()) todoList.add(new TodoItem(r2, loadTodoState(dateKey, r2)));
        }
        todoAdapter.notifyDataSetChanged();
    }

    private void showDayDialog(DayCell cell) {
        Calendar c = Calendar.getInstance();
        c.set(cell.year, cell.month, cell.day);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(c.getTime());
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        String dateDisplay = sdfDisplay.format(c.getTime());

        WidgetDailyEntry widgetView = new WidgetDailyEntry(requireContext(), R.layout.widget_daily_entry_calender);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(widgetView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // ⭐️ 팝업 중앙 정렬
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        dialog.show();
        DailyEntry entry = monthDailyMap.get(dateStr);
        widgetView.setDailyEntry(entry, dateDisplay);
    }

    private List<DayCell> buildMonthCells(int year, int month) {
        List<DayCell> list = new ArrayList<>(42);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int prefixCount = (firstDayWeek + 5) % 7;
        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = prefixCount - 1; i >= 0; i--) {
            int day = daysInPrevMonth - i;
            Calendar c = (Calendar) prevCal.clone();
            c.set(Calendar.DAY_OF_MONTH, day);
            list.add(new DayCell(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), false, null));
        }
        for(int d = 1; d <= daysInMonth; d++) {
            String key = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, d);
            DailyEntry entry = monthDailyMap.get(key);
            String emotion = null;
            if (entry != null) emotion = entry.topEmotion;
            list.add(new DayCell(year, month, d, true, emotion));
        }
        int remain = 42 - list.size();
        Calendar nextCal = (Calendar) cal.clone();
        nextCal.add(Calendar.MONTH, 1);
        for (int d = 1; d <= remain; d++) {
            Calendar c = (Calendar) nextCal.clone();
            c.set(Calendar.DAY_OF_MONTH, d);
            list.add(new DayCell(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), false, null));
        }
        return list;
    }

    static class DayCell {
        int year, month, day;
        boolean isCurrentMonth;
        String emotion;
        DayCell(int y, int m, int d, boolean c, String e) { year=y; month=m; day=d; isCurrentMonth=c; emotion=e; }
    }

    class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
        private final List<DayCell> items = new ArrayList<>();
        private final OnDayClickListener listener;

        public CalendarAdapter(OnDayClickListener listener) { this.listener = listener; }

        public void submit(List<DayCell> newItems) {
            items.clear(); items.addAll(newItems); notifyDataSetChanged();
        }

        @NonNull @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_cell, parent, false);
            return new DayViewHolder(v, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override public int getItemCount() { return items.size(); }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView dayText;
            View emotionDot;
            View container;

            public DayViewHolder(@NonNull View itemView, OnDayClickListener listener) {
                super(itemView);
                dayText = itemView.findViewById(R.id.cellDayText);
                emotionDot = itemView.findViewById(R.id.emotion_dot);
                container = itemView.findViewById(R.id.day_cell_container);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onDayClick((DayCell) itemView.getTag());
                });
            }

            public void bind(DayCell cell) {
                itemView.setTag(cell);
                dayText.setText(String.valueOf(cell.day));

                if (cell.isCurrentMonth) itemView.setAlpha(1.0f);
                else itemView.setAlpha(0.4f);

                emotionDot.setVisibility(View.INVISIBLE);
                if (cell.isCurrentMonth && cell.emotion != null) {
                    emotionDot.setVisibility(View.VISIBLE);
                    int color = ColorMapping.getEmotionColor(itemView.getContext(), cell.emotion);
                    Drawable bg = emotionDot.getBackground();
                    if (bg != null) bg.setTint(color);
                }

                Calendar today = Calendar.getInstance();
                boolean isToday = (cell.year == today.get(Calendar.YEAR) && cell.month == today.get(Calendar.MONTH) && cell.day == today.get(Calendar.DAY_OF_MONTH));
                boolean isSelected = (cell.year == selectedYear && cell.month == selectedMonth && cell.day == selectedDay);

                if (isSelected) {
                    container.setBackgroundResource(R.drawable.bg_date_selected);
                } else if (isToday) {
                    container.setBackgroundColor(0x20FF9800);
                } else {
                    container.setBackgroundResource(R.drawable.calendar_cell_border);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}