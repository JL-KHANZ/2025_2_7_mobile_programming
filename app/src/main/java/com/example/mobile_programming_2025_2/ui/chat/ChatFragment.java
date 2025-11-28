package com.example.mobile_programming_2025_2.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_programming_2025_2.MainViewModel;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.Service.MatchService;
import com.example.mobile_programming_2025_2.databinding.FragmentChatBinding;
import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private FragmentChatBinding binding;
    private MainViewModel mainViewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        ChatRepository chatRepository = new ChatRepository(new ChatRoomService(), new ChatMessageService(), new MatchService());
        ViewModelProvider.Factory factory = new ChatViewModelFactory(chatRepository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ChatViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getActiveChatRoom().observe(getViewLifecycleOwner(), roomId -> {

            if (roomId == null || roomId.isEmpty()) {
                View bottomNav = requireActivity().findViewById(R.id.nav_view);
                bottomNav.setVisibility(View.VISIBLE);

                binding.chatNoChatLayout.setVisibility(View.VISIBLE);
                binding.recyclerViewChat.setVisibility(View.GONE);
                binding.inputContainer.setVisibility(View.GONE);

                binding.chatFindButton.setOnClickListener(v -> {
                    mainViewModel.getTodayEntryData().observe(getViewLifecycleOwner(), dailyEntry -> {
                        if (dailyEntry != null) {

                            bottomNav.setVisibility(View.GONE);

                            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, new ChatSearchFragment());
                            fragmentTransaction.addToBackStack("SearchChat");
                            fragmentTransaction.commit();

                            mainViewModel.getTodayEntryData().removeObservers(getViewLifecycleOwner());
                        } else {
                            System.out.println("daily entry null");
                            Toast.makeText(this.getContext(), "오늘의 일기를 먼저 작생해 주세요.", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            } else {
                chatAdapter = new ChatAdapter(requireContext());
                LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());

                layoutManager.setStackFromEnd(true);

                binding.recyclerViewChat.setLayoutManager(layoutManager);
                binding.recyclerViewChat.setAdapter(chatAdapter);

                viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
                            if (messages != null) {
                                chatAdapter.setMessages(messages);
                                binding.recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                            }
                        });
                binding.chatNoChatLayout.setVisibility(View.GONE);
                binding.recyclerViewChat.setVisibility(View.VISIBLE);
                binding.inputContainer.setVisibility(View.VISIBLE);
                binding.recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });

        binding.buttonSend.setOnClickListener(v -> {
            String content = binding.editTextMessage.getText().toString();

            if (!content.trim().isEmpty()) {
                viewModel.sendMessage(content);
                binding.editTextMessage.setText("");
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}