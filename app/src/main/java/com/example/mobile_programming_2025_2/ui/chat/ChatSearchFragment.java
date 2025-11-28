package com.example.mobile_programming_2025_2.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.MainViewModel;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.databinding.FragmentChatSearchBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ChatSearchFragment extends Fragment {

    private ChatViewModel chatViewModel;
    private FragmentChatSearchBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get shared chatViewModel from this
        chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);

        // make requestMatch and createChatRoom in chatViewModel, then use it here:

        System.out.println("check login status: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        chatViewModel.requestMatch().observe(getViewLifecycleOwner(), candidates -> {
            if (candidates != null) {
                System.out.println("candidates: " + candidates);
                // show candidates
            } else {
                // show error
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
