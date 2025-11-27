package com.example.mobile_programming_2025_2.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.LocalRepository;
import com.example.mobile_programming_2025_2.MainViewModel;
import com.example.mobile_programming_2025_2.MainViewModelFactory;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.Service.DailyEntryService;
import com.example.mobile_programming_2025_2.databinding.FragmentHomeBinding;
import com.example.mobile_programming_2025_2.ui.daily.ActivityDaily;
import com.example.mobile_programming_2025_2.ui.widgets.WidgetDailyEntry;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MainViewModel mainViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        WidgetDailyEntry widgetDailyEntry = binding.dailyEntryWidgetHome;

        mainViewModel.getTodayEntryData().observe(getViewLifecycleOwner(), dailyEntry -> {
            System.out.println("got dailyEntry data: " + dailyEntry);
            if (dailyEntry != null && dailyEntry.feedback != null && dailyEntry.emotions != null) {
                widgetDailyEntry.setDailyEntry(dailyEntry);
                binding.dailyButton.setVisibility(View.GONE);
            } else {
                widgetDailyEntry.setDailyEntry(null);
                binding.dailyButton.setVisibility(View.VISIBLE);
            }
        });
        binding.dailyButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ActivityDaily.class);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}