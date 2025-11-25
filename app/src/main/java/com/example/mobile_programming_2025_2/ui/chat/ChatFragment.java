package com.example.mobile_programming_2025_2.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_programming_2025_2.databinding.FragmentChatBinding;
import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;
import com.example.mobile_programming_2025_2.R;


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

        ChatRoomService roomService = new ChatRoomService();
        ChatMessageService messageService = new ChatMessageService();
        ChatRepository repository = new ChatRepository(roomService, messageService);

        ViewModelProvider.Factory factory = new ChatViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);

        chatAdapter = new ChatAdapter(requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());

        layoutManager.setStackFromEnd(true);

        binding.recyclerViewChat.setLayoutManager(layoutManager);
        binding.recyclerViewChat.setAdapter(chatAdapter);


        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            chatAdapter.setMessages(messages);

            binding.recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
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