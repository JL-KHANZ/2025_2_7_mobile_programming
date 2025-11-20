package com.example.mobile_programming_2025_2.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

//import com.bumptech.glide.Glide;
import com.example.mobile_programming_2025_2.R;

import com.example.mobile_programming_2025_2.databinding.FragmentUserBinding;
import com.example.mobile_programming_2025_2.Service.UserService;

public class UserFragment extends Fragment {

    private FragmentUserBinding binding;
    private UserService userService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        UserViewModel usertViewModel =
                new ViewModelProvider(this).get(UserViewModel.class);

        binding = FragmentUserBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userService = new UserService();

        binding.passwordChange.setOnClickListener(v -> {
            View panel = binding.passwordInline;
            if (panel.getVisibility() == View.VISIBLE) {
                panel.setVisibility(View.GONE);
                binding.passwordArrow.setRotation(0f);
            } else {
                panel.setVisibility(View.VISIBLE);
                binding.passwordArrow.setRotation(180f);
            }
        });

        loadUserProfile();
        setupChangePasswordButton();
        // final TextView textView = binding.textUser; // This line is removed
        // usertViewModel.getText().observe(getViewLifecycleOwner(), textView::setText); // This line is removed
        return root;
    }

    private void loadUserProfile() {
        userService.getMyProfile(
                profile->{
                    String uid = profile.uid;
                    String email = profile.email;
                    binding.textUserid.setText(uid);
                    binding.emailInput.setText(email);
                    if (profile.photoURL == null)
                        binding.imageUser.setImageResource(R.drawable.default_avatar);
//                    else
//                        Glide.with(this).load(profile.photoURL).into(binding.imageUser);
                },
                e -> Toast.makeText(requireContext(), "프로필 정보를 불러올 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupChangePasswordButton() {
        binding.btnApply.setOnClickListener(v -> {
            String currentPwd = binding.currentPassword.getText().toString().trim();
            String newPwd = binding.newPassword.getText().toString().trim();
            String confirmPwd = binding.newPasswordConfirm.getText().toString().trim();

            if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(requireContext(), "모든 필드를 채워주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(requireContext(), "새 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPwd.length()<6) {
                Toast.makeText(requireContext(), "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 클릭 방지
            binding.btnApply.setEnabled(false);

            userService.changePassword(currentPwd, newPwd)
                    .addOnSuccessListener(result -> {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
                if (result.success) {
                    binding.currentPassword.setText("");
                    binding.newPassword.setText("");
                    binding.newPasswordConfirm.setText("");
                    binding.passwordInline.setVisibility(View.GONE);
                }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "비밀번호 변경에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(t -> {
                        binding.btnApply.setEnabled(true);
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}