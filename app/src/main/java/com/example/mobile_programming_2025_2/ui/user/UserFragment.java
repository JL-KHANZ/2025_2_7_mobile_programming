package com.example.mobile_programming_2025_2.ui.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.LoginActivity;
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

        // -------------------------------------------------------
        // 1. 닉네임 변경 패널 (열기/닫기)
        // -------------------------------------------------------
        binding.nicknameChangeHeader.setOnClickListener(v -> {
            View panel = binding.nicknameInlinePanel;

            if (panel.getVisibility() == View.VISIBLE) {
                panel.setVisibility(View.GONE);
                binding.nicknameArrow.setRotation(0f);
            } else {
                panel.setVisibility(View.VISIBLE);
                binding.nicknameArrow.setRotation(180f);
            }
        });

        // -------------------------------------------------------
        // 2. 비밀번호 변경 패널 (열기/닫기)
        // -------------------------------------------------------
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

        // -------------------------------------------------------
        // 3. 로그아웃 버튼
        // -------------------------------------------------------
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> {
                // 내부 저장소(로그인 도장) 지우기
                SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();

                // 로그인 화면으로 이동
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }

        loadUserProfile();
        setupChangeNicknameButton(); // 닉네임 변경 기능 연결
        setupChangePasswordButton(); // 비밀번호 변경 기능 연결

        return root;
    }

    private void loadUserProfile() {
        userService.getMyProfile(
                profile -> {
                    if (profile.name != null) binding.textUsername.setText(profile.name);

                    // 닉네임 설정
                    if (profile.nickname != null) {
                        binding.textUsernickname.setText(profile.nickname);
                    } else {
                        binding.textUsernickname.setText("닉네임 설정");
                    }

                    if (profile.uid != null) binding.textUserid.setText(profile.uid);
                    if (profile.email != null) binding.emailInput.setText(profile.email);
                    if (profile.photoURL == null)
                        binding.imageUser.setImageResource(R.drawable.default_avatar);
                },
                e -> Toast.makeText(requireContext(), "프로필 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show());
    }

    // 닉네임 변경 버튼 로직
    private void setupChangeNicknameButton() {
        binding.btnChangeNickname.setOnClickListener(v -> {
            String newNickname = binding.editNewNickname.getText().toString().trim();

            if (newNickname.isEmpty()) {
                Toast.makeText(requireContext(), "새 닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnChangeNickname.setEnabled(false);

            userService.changeNickname(newNickname)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
                        if (result.success) {
                            // 성공 시 UI 즉시 반영
                            binding.textUsernickname.setText(newNickname);
                            binding.editNewNickname.setText("");
                            binding.nicknameInlinePanel.setVisibility(View.GONE);
                            binding.nicknameArrow.setRotation(0f);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "닉네임 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(t -> {
                        binding.btnChangeNickname.setEnabled(true);
                    });
        });
    }

    // 비밀번호 변경 버튼 로직
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

            if (newPwd.length() < 6) {
                Toast.makeText(requireContext(), "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnApply.setEnabled(false);

            userService.changePassword(currentPwd, newPwd)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
                        if (result.success) {
                            binding.currentPassword.setText("");
                            binding.newPassword.setText("");
                            binding.newPasswordConfirm.setText("");
                            binding.passwordInline.setVisibility(View.GONE);
                            binding.passwordArrow.setRotation(0f);
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