package com.example.itstore.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.ChangePasswordRequest;
import com.example.itstore.model.ChangePasswordResponse;
import com.example.itstore.model.LogoutRequest;
import com.example.itstore.model.LogoutResponse;
import com.example.itstore.model.ProfileResponse;
import com.example.itstore.model.UpdateProfileRequest;
import com.example.itstore.model.User;
import com.example.itstore.repository.UserRepository;
import com.example.itstore.repository.WishlistRepository;
import com.example.itstore.utils.CartManager;
import com.example.itstore.utils.SharedPrefsManager;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<User> userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLogout = new MutableLiveData<>();


    private final MutableLiveData<String> updateSuccessMessage = new MutableLiveData<>();
    private final MutableLiveData<String> phoneError = new MutableLiveData<>();
    private final MutableLiveData<String> nameError = new MutableLiveData<>();


    private final MutableLiveData<String> changeSuccessMessage = new MutableLiveData<>();
    private final MutableLiveData<String> changeErrorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> oldPasswordError = new MutableLiveData<>();
    private final MutableLiveData<String> newPasswordError = new MutableLiveData<>();


    private final MutableLiveData<String> avatarUpdateStatus = new MutableLiveData<>();

    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLogout() {
        return isLogout;
    }


    public LiveData<String> getUpdateSuccessMessage() {
        return updateSuccessMessage;
    }

    public LiveData<String> getPhoneError() { return phoneError; }
    public LiveData<String> getNameError() { return nameError; }


    public LiveData<String> getChangeSuccessMessage() {
        return changeSuccessMessage;
    }
    public LiveData<String> getChangeErrorMessage() {
        return changeErrorMessage;
    }
    public LiveData<String> getOldPasswordError() {
        return oldPasswordError;
    }
    public LiveData<String> getNewPasswordError() {
        return newPasswordError;
    }

    public LiveData<String> getAvatarUpdateStatus() {
        return avatarUpdateStatus;
    }
    public void loadProfileIfNeeded() {
        if (!UserRepository.isNeedRefresh()) return;
        fetchProfile();
    }
    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);
    }

    public void fetchProfile() {

        userRepository.getProfile(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        userProfile.setValue(response.body().getData());
                        UserRepository.clearNeedRefresh();
                    } else {
                        errorMessage.setValue("Lỗi từ server!");
                    }
                } else {
                    errorMessage.setValue("Lỗi! Hãy đăng nhập lại.");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                errorMessage.setValue("Lỗi kết nối mạng!");
            }
        });
    }

    public void logout() {
        CartManager.getInstance().markCartNeedRefresh();
        WishlistRepository.markNeedRefresh();
        UserRepository.markNeedRefresh();

        String refreshToken = SharedPrefsManager.getInstance(getApplication()).getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            isLogout.setValue(true);
            return;
        }

        LogoutRequest request = new LogoutRequest(refreshToken);
        userRepository.logout(request, new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String mes = response.body().getMessage();
                    errorMessage.setValue(mes);
                    isLogout.setValue(true);
                } else {
                    errorMessage.setValue("Phiên đăng nhập hết hạn, đang đăng xuất...");
                    isLogout.setValue(true);
                }
            }

            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                isLogout.setValue(true);
            }
        });
    }

    public void updateProfile(String newName, String newPhone){

        boolean isValid = true;

        nameError.setValue(null);
        phoneError.setValue(null);

        if (newName == null || newName.trim().isEmpty()) {
            nameError.setValue("Vui lòng nhập họ và tên");
            isValid = false;
        } else {
            nameError.setValue(null);
        }

        if (newPhone == null || newPhone.trim().isEmpty()) {
            phoneError.setValue("Vui lòng nhập số điện thoại");
            isValid = false;
        } else if (!newPhone.trim().matches("^(03|05|07|08|09)\\d{8}$")) {
            phoneError.setValue("SĐT không hợp lệ!");
            isValid = false;
        } else {
            phoneError.setValue(null);
        }

        if (!isValid) {
            return;
        }


        UpdateProfileRequest request = new UpdateProfileRequest(newName, newPhone);
        userRepository.updateProfile(request, new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        userProfile.setValue(response.body().getData());
                        updateSuccessMessage.setValue("Cập nhật thành công!");
                        UserRepository.markNeedRefresh();
                    } else {
                        errorMessage.setValue("Lỗi cập nhật!");
                    }
                }
                else {
                    errorMessage.setValue("Lỗi xác thực hoặc dữ liệu không hợp lệ!");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                errorMessage.setValue("Lỗi kết nối mạng!");
            }
        });
    }

    public void changePassword(String oldPassword, String newPassword, String confirmPassword) {

        boolean isValid = true;

        oldPasswordError.setValue(null);
        newPasswordError.setValue(null);

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            oldPasswordError.setValue("Vui lòng nhập mật khẩu hiện tại");
            isValid = false;
        }

        boolean isNewPassValid = true;

        if (newPassword == null || newPassword.isEmpty()) {
            newPasswordError.setValue("Vui lòng nhập mật khẩu mới");
            isNewPassValid = false;
        } else if (newPassword.length() < 8) {
            newPasswordError.setValue("Mật khẩu phải có ít nhất 8 ký tự");
            isNewPassValid = false;
        } else if (!newPassword.matches(".*[a-z].*")) {
            newPasswordError.setValue("Mật khẩu phải có ít nhất 1 ký tự thường");
            isNewPassValid = false;
        } else if (!newPassword.matches(".*[A-Z].*")) {
            newPasswordError.setValue("Mật khẩu phải có ít nhất 1 ký tự in hoa");
            isNewPassValid = false;
        } else if (!newPassword.matches(".*[0-9].*")) {
            newPasswordError.setValue("Mật khẩu phải có ít nhất 1 số");
            isNewPassValid = false;
        } else if (!newPassword.matches(".*[^a-zA-Z0-9].*")) {
            newPasswordError.setValue("Mật khẩu phải có ít nhất 1 ký tự đặc biệt");
            isNewPassValid = false;
        }

        if (!isNewPassValid) {
            isValid = false;
        }

        if (isNewPassValid) {
            if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
                newPasswordError.setValue("Mật khẩu xác nhận không khớp!");
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);

        userRepository.changePassword(request, new Callback<ChangePasswordResponse>() {

            @Override
            public void onResponse(Call<ChangePasswordResponse> call, Response<ChangePasswordResponse> response) {
                if (response.isSuccessful() && response.body() != null){
                    if (response.body() != null && response.isSuccessful()){
                        changeSuccessMessage.setValue(response.body().getMessage());
                }
            } else {
                    try {
                        String errorStr = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorStr);
                        String serverError = jsonObject.getString("message");
                        changeErrorMessage.setValue(serverError);
                    } catch (Exception e) {
                        changeErrorMessage.setValue("Lỗi cập nhật mật khẩu!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ChangePasswordResponse> call, Throwable t) {
                changeErrorMessage.setValue("Lỗi kết nối mạng!");
            }
        });
    }


    public void uploadAvatar(File imgFile) {
        if (imgFile == null) {
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imgFile);

        MultipartBody.Part body = MultipartBody.Part.createFormData("avatar", imgFile.getName(), requestFile);

        userRepository.updateAvatar(body, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    avatarUpdateStatus.setValue("Cập nhật thành công!");
                    UserRepository.markNeedRefresh();
                } else {
                    avatarUpdateStatus.setValue("Cập nhật thất bại!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                avatarUpdateStatus.setValue("Lỗi kết nối mạng!");
            }
        });

    }

}
