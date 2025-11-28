package com.example.mobile_programming_2025_2.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.FragmentManager;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.CandidateDTO;
import com.example.mobile_programming_2025_2.databinding.FragmentChatSearchBinding;
import com.example.mobile_programming_2025_2.ui.chat.candidate.CandidateItemCallback;
import com.example.mobile_programming_2025_2.ui.chat.candidate.CandidateAdapter;

import java.util.Collections;
import java.util.List;

public class ChatSearchFragment extends Fragment implements CandidateItemCallback {
    private ChatViewModel chatViewModel;
    private FragmentChatSearchBinding binding;
    private CandidateAdapter candidateAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
        candidateAdapter = new CandidateAdapter(Collections.emptyList(), this); // 초기 빈 리스트와 콜백(this) 전달

        binding.recyclerViewCandidates.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCandidates.setAdapter(candidateAdapter);

        System.out.println("check login status: (Auth check removed for safety)");

        chatViewModel.requestMatch().observe(getViewLifecycleOwner(), candidates -> {
            if (candidates != null && !candidates.isEmpty()) {
                System.out.println("candidates received: " + candidates.size());
                displayCandidates(candidates);
            } else {
                System.out.println("No candidates found or error occurred.");
                // 사용자에게 후보자가 없음을 알리는 UI 업데이트 (예: 텍스트 뷰 표시)
                // binding.tvNoCandidates.setVisibility(View.VISIBLE);
            }
        });
    }

    // LiveData에서 받은 후보자 리스트를 Adapter에 전달하여 화면에 표시합니다.
    public void displayCandidates(List<CandidateDTO> candidates) {
        if (candidateAdapter != null) {
            candidateAdapter.setCandidates(candidates);
        }
    }
    private void setBottomNavVisibility(int visibility) {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.nav_view);
            if (bottomNav != null) {
                bottomNav.setVisibility(visibility);
            }
        }
    }

    // 후보자 버튼을 클릭했을 때 호출
    @Override
    public void onCandidateSelected(CandidateDTO candidate) {
        System.out.println("Candidate selected: " + candidate.uid);

        chatViewModel.createChatRoom(candidate.uid);
        setBottomNavVisibility(View.VISIBLE);

        String message = String.format("후보자 [%s]와 채팅방을 생성합니다.", candidate.displayName);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStack("SearchChat", 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setBottomNavVisibility(View.VISIBLE);
        binding = null;
    }
}