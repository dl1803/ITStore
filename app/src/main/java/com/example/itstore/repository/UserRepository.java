package com.example.itstore.repository;

import android.content.Context;

import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.ChangePasswordRequest;
import com.example.itstore.model.ChangePasswordResponse;
import com.example.itstore.model.LogoutRequest;
import com.example.itstore.model.LogoutResponse;
import com.example.itstore.model.ProfileResponse;
import com.example.itstore.model.UpdateProfileRequest;

import okhttp3.MultipartBody;
import retrofit2.Callback;

public class UserRepository {
    private static UserRepository instance;
    private final RetrofitClient.ApiService apiService;
    private static boolean needRefresh = true;

    public static void markNeedRefresh() { needRefresh = true; }
    public static boolean isNeedRefresh() { return needRefresh; }
    public static void clearNeedRefresh() { needRefresh = false; }
    private UserRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context.getApplicationContext());
        }
        return instance;
    }
    public void getProfile(Callback<ProfileResponse> callback) {
        apiService.getProfile().enqueue(callback);
    }
    public void logout(LogoutRequest request, Callback<LogoutResponse> callback) {
        apiService.logout(request).enqueue(callback);
    }
    public void updateProfile(UpdateProfileRequest request, Callback<ProfileResponse> callback) {
        apiService.updateProfile(request).enqueue(callback);
    }
    public void changePassword(ChangePasswordRequest request, Callback<ChangePasswordResponse> callback) {
        apiService.changePassword(request).enqueue(callback);
    }
    public void updateAvatar(MultipartBody.Part avatar, Callback<Void> callback) {
        apiService.updateAvatar(avatar).enqueue(callback);
    }
}