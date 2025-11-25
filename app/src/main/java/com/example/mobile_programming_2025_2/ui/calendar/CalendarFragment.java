package com.example.mobile_programming_2025_2.ui.calendar;

import android.os.Bundle;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_programming_2025_2.R;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

import com.example.mobile_programming_2025_2.databinding.FragmentCalendarBinding;
import com.example.mobile_programming_2025_2.databinding.FragmentHomeBinding;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private TextView textMonth;
    private ImageButton btnPrev, btnNext;
    private RecyclerView recyclerCalendar;
    private CalendarAdapter adapter;

    private int currentYear;
    private int currentMonth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CalendarViewModel calendarViewModel =
                new ViewModelProvider(this).get(CalendarViewModel.class);

        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textCalendar;
//        calendarViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textMonth = view.findViewById(R.id.text_month);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);
        recyclerCalendar = view.findViewById(R.id.recycler_calendar);

        recyclerCalendar.setLayoutManager(new GridLayoutManager(getActivity(), 7));
        adapter = new CalendarAdapter(new CalendarAdapter.OnDayClickListener() {
            @Override
            public void onDayClick(DayCell cell) {
                showDayDialog(cell);
            }
        });
        recyclerCalendar.setAdapter(adapter);

        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

        renderMonth(currentYear, currentMonth);

        btnPrev.setOnClickListener(v -> {
            if (currentMonth == 0) {
                currentMonth = 11;
                currentYear -= 1;
            } else {
                currentMonth -= 1;
            }
            renderMonth(currentYear, currentMonth);
        });
        btnNext.setOnClickListener(v -> {
            if (currentMonth == 11) {
                currentMonth = 0;
                currentYear += 1;
            } else {
                currentMonth += 1;
            }
            renderMonth(currentYear, currentMonth);
        });
    }

    private void renderMonth(int year, int month) {
        String text = String.format("%04d년 %02d월", year, month + 1);
        textMonth.setText(text);

        List<DayCell> cells = buildMonthCells(year, month);
        adapter.submit(cells);
    }

    private List<DayCell> buildMonthCells(int year, int month) {
        List<DayCell> list = new ArrayList<>(42);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayWeek = cal.get(Calendar.DAY_OF_WEEK);  // 1=일, 7=토
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int prefixCount = (firstDayWeek + 5) % 7;

        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = prefixCount - 1; i >= 0; i--) {
            int day = daysInPrevMonth - i;
            Calendar c = (Calendar) prevCal.clone();
            c.set(Calendar.DAY_OF_MONTH, day);
            list.add(new DayCell(
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH),
                    false,
                    null
            ));
        }

        for(int d = 1; d <= daysInMonth; d++) {
            list.add(new DayCell(year, month, d, true, null));
        }

        int totalCells = 42;
        int remain = totalCells - list.size();

        Calendar nextCal = (Calendar) cal.clone();
        nextCal.add(Calendar.MONTH, 1);
        for (int d = 1; d <= remain; d++) {
            Calendar c = (Calendar) nextCal.clone();
            c.set(Calendar.DAY_OF_MONTH, d);
            list.add(new DayCell(
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH),
                    false,
                    null
            ));
        }
        return list;
    }

    static class DayCell {
        int year;
        int month;
        int day;
        boolean isCurrentMonth;
        String emotion;

        DayCell(int year, int month, int day, boolean isCurrentMonth, String emotion) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isCurrentMonth = isCurrentMonth;
            this.emotion = emotion;
        }
    }

    static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
        private final List<DayCell> items = new ArrayList<>();

        public interface OnDayClickListener {
            void onDayClick(DayCell cell);
        }

        private final OnDayClickListener listener;
        public CalendarAdapter(OnDayClickListener listener) {
            this.listener = listener;
        }

        public void submit(List<DayCell> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_day_cell, parent, false);
            return new DayViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class DayViewHolder extends RecyclerView.ViewHolder {
            TextView dayText;

            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                dayText = itemView.findViewById(R.id.text_day);
            }

            public void bind(DayCell cell, OnDayClickListener listener) {
                dayText.setText(String.valueOf(cell.day));

                itemView.setAlpha(cell.isCurrentMonth ? 1f : 0.4f);

                Calendar today = Calendar.getInstance();
                boolean isToday = cell.year == today.get(Calendar.YEAR) &&
                        cell.month == today.get(Calendar.MONTH) &&
                        cell.day == today.get(Calendar.DAY_OF_MONTH);

                if (isToday) {
                    itemView.setBackgroundColor(0x20FF9800);
                } else {
                    itemView.setBackgroundColor(0x00000000);
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null && cell.isCurrentMonth) {
                        listener.onDayClick(cell);
                    }
                });
            }
        }
    }

    private void showDayDialog(DayCell cell) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(cell.year, cell.month, cell.day);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(c.getTime());

        new AlertDialog.Builder(requireContext())
                .setTitle(dateStr).setMessage("감정 분석 내용").setPositiveButton("닫기", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}