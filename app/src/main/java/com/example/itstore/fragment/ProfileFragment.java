package com.example.itstore.fragment;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.itstore.activity.AddressActivity;
import com.example.itstore.activity.ChangePasswordActivity;
import com.example.itstore.activity.EditProfileActivity;
import com.example.itstore.activity.LoginActivity;
import com.example.itstore.activity.NotificationActivity;
import com.example.itstore.activity.NotificationSettingsActivity;
import com.example.itstore.activity.OrderHistoryActivity;
import com.example.itstore.activity.UnreviewedListActivity;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.FragmentProfileBinding;
import com.example.itstore.model.NotificationUnreadCountResponse;
import com.example.itstore.utils.CustomGlideUrl;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.NotificationViewModel;
import com.example.itstore.viewmodel.ProfileViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private NotificationViewModel notificationViewModel;
    private String currentAvatarUrl = "";

    // Chọn ảnh từ thư viện
    private final ActivityResultLauncher<String> pickImgLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null){
            binding.imgAvatar.setImageURI(uri);
            File fileToUpload = convertUriToFile(uri);

            if (fileToUpload != null){
                profileViewModel.uploadAvatar(fileToUpload);
            } else {
                Toast.makeText(requireContext(), "Lỗi khi đọc file ảnh!", Toast.LENGTH_SHORT).show();
            }
        }
    });
    private final BroadcastReceiver notiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (notificationViewModel != null) {
                notificationViewModel.fetchUnreadCount();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        setupObservers();

        String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();

        if (token == null || token.isEmpty()) {
            binding.tvNameUser.setText("Xin chào, Khách!");
            binding.tvEmailUser.setText("Vui lòng đăng nhập để xem hồ sơ");
            binding.tvPhoneUser.setText("");
            binding.tvRoleUser.setText("Guest");
            binding.tvLogout.setText("Đăng nhập");
            binding.tvLogout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });

            binding.tvEditProfile.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Bạn cần đăng nhập để sử dụng tính năng này!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });

        } else {
            String cachedName = SharedPrefsManager.getInstance(requireContext()).getUserFullName();
            String cachedEmail = SharedPrefsManager.getInstance(requireContext()).getUserEmail();

            binding.tvNameUser.setText(cachedName);
            binding.tvEmailUser.setText(cachedEmail);

            profileViewModel.fetchProfile();
            notificationViewModel.fetchUnreadCount();
            binding.tvLogout.setOnClickListener(v -> {
                profileViewModel.logout();
            });

            binding.tvEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                intent.putExtra("name", binding.tvNameUser.getText().toString());
                intent.putExtra("phone", binding.tvPhoneUser.getText().toString());
                intent.putExtra("email", binding.tvEmailUser.getText().toString());
                startActivity(intent);
            });

            binding.tvChangePass.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
                startActivity(intent);
            });

            binding.imgAvatar.setOnClickListener(v -> {
                pickImgLauncher.launch("image/*");
            });

            binding.tvAddressInfo.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddressActivity.class);
                startActivity(intent);
            });

            binding.tvNotificationSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NotificationSettingsActivity.class);
                startActivity(intent);
            });

            binding.tvOrders.setOnClickListener( v -> {
                Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
                startActivity(intent);
            });
            binding.tvUnreviewed.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UnreviewedListActivity.class);
                startActivity(intent);
            });
            binding.btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NotificationActivity.class);
                startActivity(intent);
            });
        }


        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Check lại số lượng notifications chưa đọc mỗi khi người dùng bấm vào profile
        String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
        if (token != null && !token.isEmpty()) {
            notificationViewModel.fetchUnreadCount();
            androidx.core.content.ContextCompat.registerReceiver(
                    requireContext(),
                    notiReceiver,
                    new android.content.IntentFilter("ACTION_UPDATE_NOTI_BADGE"),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        try {
            requireActivity().unregisterReceiver(notiReceiver);
        } catch (Exception ignored) {}
    }
    private void setupObservers() {
        notificationViewModel.getUnreadCountLiveData().observe(getViewLifecycleOwner(), count -> {
            Log.d("NOTI_BADGE_DEBUG", "Số lượng tin chưa đọc Server trả về là: " + count);

            if (count != null && count > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                if (count > 9) {
                    binding.tvNotificationBadge.setText("9+");
                } else {
                    binding.tvNotificationBadge.setText(String.valueOf(count));
                }
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });
        notificationViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                Log.e("NOTI_BADGE_DEBUG", "API Đếm số lượng chưa đọc bị lỗi rồi: " + errorMsg);
                Toast.makeText(requireContext(), "Lỗi đếm thông báo: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvNameUser.setText(user.getFull_name());
                binding.tvEmailUser.setText(user.getEmail());
                binding.tvPhoneUser.setText(user.getPhone_number());
                binding.tvRoleUser.setText(user.getRole());

                if (user.getAvatar_url() != null && !user.getAvatar_url().isEmpty()) {

                    String fullUrl = user.getAvatar_url();
                    String cleanUrl = fullUrl.split("\\?")[0];

                    if (!user.getAvatar_url().equals(currentAvatarUrl)) {
                        currentAvatarUrl = cleanUrl;
                        Glide.with(requireContext())
                                .load(new CustomGlideUrl(fullUrl))
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .error(android.R.drawable.ic_menu_camera)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .dontAnimate()
                                .into(binding.imgAvatar);
                    }
                }
            }
        });

        profileViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        profileViewModel.getIsLogout().observe(getViewLifecycleOwner(), isLogout -> {
            if (isLogout != null && isLogout) {
                SharedPrefsManager.getInstance(requireContext()).clear();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        profileViewModel.getAvatarUpdateStatus().observe(getViewLifecycleOwner(), status ->{
            if (status != null) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Chuyển đổi Uri thành File
    private File convertUriToFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);

            File tempFile = File.createTempFile("avatar_tmp", ".jpg", requireContext().getCacheDir());

            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4 * 1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e){
            return null;
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}