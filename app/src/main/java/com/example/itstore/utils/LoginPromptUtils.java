package com.example.itstore.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.example.itstore.activity.LoginActivity;

/**
 * Hiển thị dialog yêu cầu đăng nhập thay vì tự động chuyển trang.
 * Người dùng chủ động chọn "Đăng nhập" hoặc "Để sau".
 */
public class LoginPromptUtils {

    /**
     * Hiển thị dialog thông báo cần đăng nhập.
     *
     * @param context Context hiện tại (Activity hoặc Fragment context)
     * @param message Nội dung thông báo lý do cần đăng nhập
     */
    public static void showLoginRequiredDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage(message)
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    Intent intent = new Intent(context, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                })
                .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
