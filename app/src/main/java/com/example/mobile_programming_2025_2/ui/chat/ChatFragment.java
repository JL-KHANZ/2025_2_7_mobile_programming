package com.example.mobile_programming_2025_2.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_programming_2025_2.SearchChatActivity;
import com.example.mobile_programming_2025_2.databinding.FragmentChatBinding;
import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;


public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private FragmentChatBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChatRepository chatRepository = new ChatRepository(new ChatRoomService(), new ChatMessageService());

        ViewModelProvider.Factory factory = new ChatViewModelFactory(chatRepository);

        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);

        chatAdapter = new ChatAdapter(requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());

        layoutManager.setStackFromEnd(true);

        binding.recyclerViewChat.setLayoutManager(layoutManager);
        binding.recyclerViewChat.setAdapter(chatAdapter);


        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {

            chatAdapter.setMessages(messages);

            if (messages == null || messages.isEmpty()) {
                binding.chatNoChatLayout.setVisibility(View.VISIBLE);
                binding.recyclerViewChat.setVisibility(View.GONE);
                binding.inputContainer.setVisibility(View.GONE);
            } else {
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

        binding.chatFindButton.setOnClickListener(v -> {
            // 오늘의 감정이 있는지 확인 필요
            // 있으면 SearchChatActivity로 이동

            android.content.Intent intent = new android.content.Intent(requireActivity(), SearchChatActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}