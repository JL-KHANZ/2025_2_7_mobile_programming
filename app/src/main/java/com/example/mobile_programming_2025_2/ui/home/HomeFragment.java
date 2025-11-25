package com.example.mobile_programming_2025_2.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.LocalRepository;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.Service.DailyEntryService;
import com.example.mobile_programming_2025_2.databinding.FragmentHomeBinding;
import com.example.mobile_programming_2025_2.ui.daily.ActivityDaily;
import com.example.mobile_programming_2025_2.ui.widgets.WidgetDailyEntry;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        LocalRepository repository = new LocalRepository(new DailyEntryService());
        ViewModelProvider.Factory factory = new HomeViewModelFactory(repository);

        HomeViewModel homeViewModel =
                new ViewModelProvider(this, factory).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        WidgetDailyEntry widgetDailyEntry = root.findViewById(R.id.daily_entry_widget_home);

        if (widgetDailyEntry != null) {
            homeViewModel.getTodayEntryData().observe(getViewLifecycleOwner(), dailyEntry -> {
                if (dailyEntry != null) {
                    widgetDailyEntry.setDailyEntry(dailyEntry);
                    Button dailyButton = root.findViewById(R.id.daily_button);
                    dailyButton.setVisibility(View.GONE);
                } else {
                    widgetDailyEntry.setDailyEntry(null);
                }
            });
        }

        homeViewModel.getText().observe(getViewLifecycleOwner(), binding.textHome::setText);

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