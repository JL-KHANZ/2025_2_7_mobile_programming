package com.example.mobile_programming_2025_2.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.MainViewModel;
import com.example.mobile_programming_2025_2.databinding.FragmentHomeBinding;
import com.example.mobile_programming_2025_2.ui.daily.ActivityDaily;
import com.example.mobile_programming_2025_2.ui.widgets.WidgetDailyEntry;
import com.example.mobile_programming_2025_2.Service.UserService; // ⭐️ 추가됨

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MainViewModel mainViewModel;
    private UserService userService; // ⭐️ 추가됨

    // 감정 이름 배열
    private final String[] emotionNames = {
            "기쁨", "사랑", "신뢰", "순종", "공포", "경외", "놀람", "반감",
            "슬픔", "자책", "혐오", "경멸", "분노", "공격성", "기대", "낙관"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        userService = new UserService(); // ⭐️ 초기화
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ⭐️ [추가됨] 1. 닉네임 불러와서 환영 문구 설정
        userService.getMyProfile(
                profile -> {
                    // profile.name 대신 profile.nickname을 확인합니다!
                    if (profile != null && profile.nickname != null) {
                        binding.textHome.setText("안녕하세요 " + profile.nickname + "님!");
                    } else {
                        binding.textHome.setText("안녕하세요!");
                    }
                },
                e -> {
                    binding.textHome.setText("안녕하세요!");
                }
        );

        // ===============================================================
        // 2. [기록 전] 휠 & 눈동자 인터랙션
        // ===============================================================

        binding.emotionSlider.setOnValueChangeListener(value -> {
            if (binding.textEmotion.getVisibility() != View.VISIBLE) {
                binding.textEmotion.setVisibility(View.VISIBLE);
            }
            int index = (int) (value / 100f * 16) % 16;
            binding.textEmotion.setText(emotionNames[index]);

            int currentColor = binding.emotionSlider.getCurrentEmotionColor();
            binding.textEmotion.setTextColor(currentColor);

            int alpha = 70;
            int transparentColor = androidx.core.graphics.ColorUtils.setAlphaComponent(currentColor, alpha);
            android.graphics.drawable.GradientDrawable faceBackground = new android.graphics.drawable.GradientDrawable();
            faceBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            faceBackground.setColor(transparentColor);
            binding.googlyEyesHome.setBackground(faceBackground);
        });

        binding.emotionSlider.setOnTouchListener((v, event) -> {
            binding.googlyEyesHome.lookAtRaw(event.getRawX(), event.getRawY());
            return false;
        });

        binding.dailyButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ActivityDaily.class);
            String selectedEmotion = binding.textEmotion.getText().toString();
            if (selectedEmotion.isEmpty()) selectedEmotion = "기쁨";
            intent.putExtra("SELECTED_EMOTION", selectedEmotion);
            startActivity(intent);
        });

        // ===============================================================
        // 3. [기록 후] 팝(Pop) & 위젯 설정
        // ===============================================================

        mainViewModel.getTodayEntryData().observe(getViewLifecycleOwner(), dailyEntry -> {
            View beforeGroup = binding.beforeAnalysisGroup;
            View afterGroup = binding.afterAnalysisGroup;
            View emojiContainer = binding.todayEmojiContainer;
            WidgetDailyEntry widgetDailyEntry = binding.dailyEntryWidgetHome;

            if (dailyEntry != null && dailyEntry.feedback != null && dailyEntry.emotions != null) {
                // [기록 있음]
                beforeGroup.setVisibility(View.GONE);
                afterGroup.setVisibility(View.VISIBLE);
                emojiContainer.setVisibility(View.VISIBLE);

                String dateString = dailyEntry.date;
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
                    Date date = inputFormat.parse(dailyEntry.date);
                    if (date != null) dateString = outputFormat.format(date);
                } catch (Exception e) { e.printStackTrace(); }

                widgetDailyEntry.setDailyEntry(dailyEntry, dateString);

                // 감정 이름 & 색상
                String topEmotionName = getTopEmotionName(dailyEntry.emotions);
                int maxColor = com.example.mobile_programming_2025_2.ui.ColorMapping.getEmotionColor(requireContext(), topEmotionName);

                // 팝 배경색 설정
                android.graphics.drawable.GradientDrawable emojiBg = new android.graphics.drawable.GradientDrawable();
                emojiBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                emojiBg.setColor(maxColor);
                binding.googlyEyesResult.setBackground(emojiBg);

                binding.textNoRecordMessage.setVisibility(View.GONE);

                // 팝 클릭 시 버블 발사
                binding.popClickTarget.setOnClickListener(v -> {
                    int[] location = new int[2];
                    binding.popClickTarget.getLocationOnScreen(location);
                    float centerX = location[0] + binding.popClickTarget.getWidth() / 2f;
                    float centerY = location[1] + binding.popClickTarget.getHeight() / 2f;
                    int[] bubbleViewLoc = new int[2];
                    binding.bubbleTriangleView.getLocationOnScreen(bubbleViewLoc);

                    binding.bubbleTriangleView.explodeTriangle(
                            centerX - bubbleViewLoc[0],
                            centerY - bubbleViewLoc[1],
                            maxColor
                    );
                });

            } else {
                // [기록 없음]
                beforeGroup.setVisibility(View.VISIBLE);
                afterGroup.setVisibility(View.GONE);
                emojiContainer.setVisibility(View.GONE);

                binding.textNoRecordMessage.setVisibility(View.VISIBLE);
                binding.textEmotion.setVisibility(View.INVISIBLE);

                widgetDailyEntry.setDailyEntry(null, "");
            }
        });

        return root;
    }

    private String getTopEmotionName(java.util.Map<String, Integer> emotions) {
        String topEmotion = "기쁨";
        int maxScore = -1;
        if (emotions != null) {
            for (java.util.Map.Entry<String, Integer> entry : emotions.entrySet()) {
                Integer score = entry.getValue();
                if (score != null && score > maxScore) {
                    maxScore = score;
                    topEmotion = entry.getKey();
                }
            }
        }
        return topEmotion.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}