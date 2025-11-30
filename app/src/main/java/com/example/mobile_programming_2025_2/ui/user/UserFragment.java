package com.example.mobile_programming_2025_2.ui.user;

import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.mobile_programming_2025_2.LoginActivity;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.ProfileUpdate;
import com.example.mobile_programming_2025_2.databinding.FragmentUserBinding;
import com.example.mobile_programming_2025_2.Service.UserService;

public class UserFragment extends Fragment {

    private FragmentUserBinding binding;
    private UserService userService;
    private ImageView imageUserProfile;
    private ImageView dialogProfileImageView;
    private Uri pendingProfileImageUri;
    private String currentProfileUrl = null;

    private ActivityResultLauncher<String> pickimageLauncher;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        UserViewModel usertViewModel =
                new ViewModelProvider(this).get(UserViewModel.class);

        binding = FragmentUserBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userService = new UserService();

        TextView textUsername = root.findViewById(R.id.text_username);
        TextView textUsernickname = root.findViewById(R.id.text_usernickname);
        TextView fixProfile = root.findViewById(R.id.fix_profile);

        pickimageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        pendingProfileImageUri = uri;
                        if (dialogProfileImageView != null) {
                            Glide.with(this)
                                    .load(uri)
                                    .circleCrop()
                                    .into(dialogProfileImageView);
                        }
                    }
                }
        );

        imageUserProfile = root.findViewById(R.id.image_user);

        fixProfile.setOnClickListener(v -> {
            showEditProfileDialog(textUsername, textUsernickname);
        });

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

    private void showEditProfileDialog(TextView textUsername, TextView textUsernickname) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        EditText etName = dialogView.findViewById(R.id.edit_name);
        EditText etNickname = dialogView.findViewById(R.id.edit_nickname);

        ImageView imagePreview = dialogView.findViewById(R.id.image_profile_preview);
        TextView textChangeImage = dialogView.findViewById(R.id.text_change_image);

        // 미리보기 이미지 변경
        if (pendingProfileImageUri != null) {
            Glide.with(this)
                    .load(pendingProfileImageUri)
                    .circleCrop()
                    .into(imagePreview);
        } else if (imageUserProfile != null && imageUserProfile.getDrawable() != null) {
            Glide.with(this)
                    .load(currentProfileUrl)
                    .circleCrop()
                    .into(imagePreview);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(imagePreview);
        }

        dialogProfileImageView = imagePreview;

        View.OnClickListener changeImageClickListener = v -> {
            pickimageLauncher.launch("image/*");
        };
        imagePreview.setOnClickListener(changeImageClickListener);
        textChangeImage.setOnClickListener(changeImageClickListener);

        // 기존 이름 표시
        CharSequence currentName = textUsername.getText();
        if (currentName != null && currentName.length() > 0) {
            etName.setText(currentName);
            etName.setSelection(currentName.length());
        }

        // 기존 닉네임 표시
        CharSequence currentNickname = textUsernickname.getText();
        if (currentNickname != null && currentNickname.length() > 0) {
            etNickname.setText(currentNickname);
            etNickname.setSelection(currentNickname.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("프로필 수정")
                .setView(dialogView)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSave.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newNickname = etNickname.getText().toString().trim();

                if (newName.isEmpty()) {
                    etName.setError("이름을 입력해주세요");
                    etName.requestFocus();
                    return;
                }
                if (newNickname.isEmpty()) {
                    etNickname.setError("닉네임을 입력해주세요");
                    etNickname.requestFocus();
                    return;
                }

                ProfileUpdate profileUpdate = new ProfileUpdate();
                profileUpdate.name = newName;
                profileUpdate.nickname = newNickname;
                if (pendingProfileImageUri != null) {
                    profileUpdate.photoUri = pendingProfileImageUri;
                }

                userService.saveMyProfile(profileUpdate)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show();

                            textUsername.setText(newName);
                            textUsernickname.setText(newNickname);

                            if (pendingProfileImageUri != null && imageUserProfile != null) {
                                Glide.with(this)
                                        .load(pendingProfileImageUri)
                                        .circleCrop()
                                        .into(imageUserProfile);
                                pendingProfileImageUri = null;
                            }
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "프로필 저장 실패" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        });
        dialog.setOnDismissListener(d -> {
            dialogProfileImageView = null;
            pendingProfileImageUri = null;
        });
        dialog.show();
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
                    if (profile.email != null) binding.textUseremail.setText(profile.email);
                    currentProfileUrl = profile.photoURL;
                    if (profile.photoURL == null)
                        Glide.with(this)
                                .load(R.drawable.default_avatar)
                                .circleCrop()
                                .into(binding.imageUser);
                    else
                        Glide.with(this)
                                .load(profile.photoURL)
                                .circleCrop()
                                .into(binding.imageUser);
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