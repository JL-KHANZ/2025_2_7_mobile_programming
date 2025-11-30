package com.example.mobile_programming_2025_2.ui.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
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

    // 🔥 딱 하나만 존재하는 온클릭 리스너
    public interface OnDayClickListener { void onDayClick(DayCell cell); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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

        // 🔹 Todo 리스트
        todoAdapter = new TodoAdapter(todoList, (item, isChecked) -> saveTodoState(item.getText(), isChecked));
        recyclerTodo.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTodo.setAdapter(todoAdapter);

        // 🔥 Calendar Adapter 연결
        recyclerCalendar.setLayoutManager(new GridLayoutManager(getActivity(), 7));
        adapter = new CalendarAdapter(cell -> {
            if (!cell.isCurrentMonth) return;

            if (cell.year == selectedYear && cell.month == selectedMonth && cell.day == selectedDay) {
                showDayDialog(cell);
            } else {
                selectedYear = cell.year;
                selectedMonth = cell.month;
                selectedDay = cell.day;
                adapter.notifyDataSetChanged();
                updateTodoListForSelectedDate(cell);
            }
        });
        recyclerCalendar.setAdapter(adapter);

        renderMonth(currentYear, currentMonth);

        btnPrev.setOnClickListener(v -> {
            if (currentMonth == 0) { currentMonth = 11; currentYear--; }
            else currentMonth--;
            renderMonth(currentYear, currentMonth);
        });

        btnNext.setOnClickListener(v -> {
            if (currentMonth == 11) { currentMonth = 0; currentYear++; }
            else currentMonth++;
            renderMonth(currentYear, currentMonth);
        });
    }

    private void renderMonth(int year, int month) {
        textMonth.setText(String.format(Locale.getDefault(), "%04d년 %02d월", year, month + 1));
        loadMonthDailyEntries(year, month);
    }

    private void loadMonthDailyEntries(int year, int month) {
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, 1);
        Calendar endCal = (Calendar) startCal.clone();
        endCal.set(year, month, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        dailyEntryService.getByPeriod(
                sdf.format(startCal.getTime()), sdf.format(endCal.getTime()),
                dailyMap -> {
                    monthDailyMap = dailyMap;
                    adapter.submit(buildMonthCells(year, month));
                    updateTodoList(monthDailyMap.get(getSelectedKey()));
                },
                e -> adapter.submit(buildMonthCells(year, month))
        );
    }

    private String getSelectedKey() {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
    }

    private void saveTodoState(String todoContent, boolean isChecked) {
        SharedPreferences prefs = requireContext().getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(getSelectedKey()+"_"+todoContent, isChecked).apply();
    }

    private boolean loadTodoState(String dateKey, String todoContent) {
        SharedPreferences prefs = requireContext().getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(dateKey+"_"+todoContent, false);
    }

    private void updateTodoListForSelectedDate(DayCell cell) {
        updateTodoList(monthDailyMap.get(
                String.format(Locale.getDefault(), "%04d-%02d-%02d", cell.year, cell.month+1, cell.day)
        ));
    }

    private void updateTodoList(DailyEntry entry) {
        todoList.clear();

        String key = getSelectedKey();
        if (entry != null && entry.feedback != null) {
            if (entry.feedback.get("생활 루틴 1") != null)
                todoList.add(new TodoItem(entry.feedback.get("생활 루틴 1"), loadTodoState(key, entry.feedback.get("생활 루틴 1"))));

            if (entry.feedback.get("생활 루틴 2") != null)
                todoList.add(new TodoItem(entry.feedback.get("생활 루틴 2"), loadTodoState(key, entry.feedback.get("생활 루틴 2"))));
        }
        todoAdapter.notifyDataSetChanged();
    }

    // 🔥 팝업 위젯 띄우기
    private void showDayDialog(DayCell cell) {
        Calendar c = Calendar.getInstance();
        c.set(cell.year, cell.month, cell.day);

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
        String dateDisplay = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(c.getTime());

        WidgetDailyEntry widgetView = new WidgetDailyEntry(requireContext(), null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(widgetView)
                .create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setGravity(Gravity.CENTER);

        dialog.show();
        widgetView.setDailyEntry(monthDailyMap.get(dateStr), dateDisplay);
    }

    private List<DayCell> buildMonthCells(int year, int month) {
        List<DayCell> list = new ArrayList<>(42);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int firstDayWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int prefix = (firstDayWeek + 5) % 7;

        Calendar prev = (Calendar) cal.clone(); prev.add(Calendar.MONTH,-1);
        int prevDays = prev.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = prefix-1;i>=0;i--)
            list.add(new DayCell(prev.get(Calendar.YEAR), prev.get(Calendar.MONTH), prevDays-i,false,null));

        for(int d=1; d<=daysInMonth; d++){
            DailyEntry entry = monthDailyMap.get(String.format("%04d-%02d-%02d",year,month+1,d));
            list.add(new DayCell(year,month,d,true,entry==null?null:entry.topEmotion));
        }

        Calendar next = (Calendar) cal.clone(); next.add(Calendar.MONTH,1);
        while(list.size()<42)
            list.add(new DayCell(next.get(Calendar.YEAR), next.get(Calendar.MONTH), list.size()-daysInMonth-prefix+1,false,null));

        return list;
    }

    // 📌 Calendar Cell 데이터 구조
    static class DayCell{
        int year,month,day; boolean isCurrentMonth; String emotion;
        DayCell(int y,int m,int d,boolean c,String e){year=y;month=m;day=d;isCurrentMonth=c;emotion=e;}
    }

    // 📌 Calendar Adapter 확정 버전
    class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
        private final List<DayCell> items = new ArrayList<>();
        private final OnDayClickListener listener;

        CalendarAdapter(OnDayClickListener l){listener=l;}
        void submit(List<DayCell> list){items.clear();items.addAll(list);notifyDataSetChanged();}
        @Override public int getItemCount(){return items.size();}
        @NonNull @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup p,int v){
            return new DayViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_day_cell,p,false));
        }
        @Override public void onBindViewHolder(@NonNull DayViewHolder h,int pos){h.bind(items.get(pos));}

        class DayViewHolder extends RecyclerView.ViewHolder{
            TextView day; View dot,box;
            DayViewHolder(View v){ super(v);
                day=v.findViewById(R.id.cellDayText);
                dot=v.findViewById(R.id.emotion_dot);
                box=v.findViewById(R.id.day_cell_container);

                v.setOnClickListener(view -> {
                    if(listener!=null) listener.onDayClick((DayCell)view.getTag());
                });
            }

            void bind(DayCell cell){
                itemView.setTag(cell);
                day.setText(String.valueOf(cell.day));

                itemView.setAlpha(cell.isCurrentMonth?1f:0.35f);
                dot.setVisibility(View.GONE);

                if(cell.isCurrentMonth && cell.emotion!=null){
                    dot.setVisibility(View.VISIBLE);
                    Drawable bg=dot.getBackground();
                    if(bg!=null) bg.setTint(ColorMapping.getEmotionColor(itemView.getContext(),cell.emotion));
                }

                Calendar t=Calendar.getInstance();
                boolean today=cell.year==t.get(Calendar.YEAR)&&cell.month==t.get(Calendar.MONTH)&&cell.day==t.get(Calendar.DAY_OF_MONTH);
                boolean sel=cell.year==selectedYear&&cell.month==selectedMonth&&cell.day==selectedDay;

                if(sel) box.setBackgroundResource(R.drawable.bg_date_selected);
                else if(today) box.setBackgroundColor(0x22FF9800);
                else box.setBackgroundResource(R.drawable.calendar_cell_border);
            }
        }
    }

    @Override public void onDestroyView(){super.onDestroyView(); binding=null;}
}
