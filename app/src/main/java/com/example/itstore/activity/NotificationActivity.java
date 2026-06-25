package com.example.itstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.itstore.R;
import com.example.itstore.adapter.NotificationAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.ActivityNotificationBinding;
import com.example.itstore.model.ItemNotification;
import com.example.itstore.model.NotificationListResponse;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.NotificationViewModel;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {
    private ActivityNotificationBinding binding;
    private NotificationAdapter adapter;
    private NotificationViewModel viewModel;
    private Thread sseThread;
    private okhttp3.Call activeSseCall;
    private volatile boolean isActivityDestroyed = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = SharedPrefsManager.getInstance(this).getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        setupObservers();
        binding.imgBack.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter();
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);
        adapter.setOnItemClickListener(notiItem -> {
            if (!notiItem.isRead()) {
                int notiId = Integer.parseInt(notiItem.getId());
                viewModel.markNotificationAsRead(notiId);
            }
        });
        binding.tvMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllNotificationsAsRead();
            Toast.makeText(NotificationActivity.this, "Đã đánh dấu đọc tất cả thông báo!", Toast.LENGTH_SHORT).show();
        });
        viewModel.fetchNotifications(1, 20, null);
        connectToNotificationStream();
    }
    private void setupObservers() {
        viewModel.getNotificationsLiveData().observe(this, list -> {
            if (list != null) {
                adapter.setNotiList(list);
                updateEmptyStateUI(list.isEmpty());
                binding.rvNotifications.scrollToPosition(0);
            }
        });
        viewModel.getErrorLiveData().observe(this, errorMsg -> {
            if (errorMsg != null) Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }
    private void connectToNotificationStream() {
        if (isActivityDestroyed) return;
        String token = SharedPrefsManager.getInstance(this).getAccessToken();
        if (token == null || token.isEmpty()) return;

        sseThread = new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // Giữ kết nối mãi mãi
                        .build();

                okhttp3.Request request = new Request.Builder()
                        .url("http://10.0.2.2:3000/api/notifications/stream")
                        .header("Accept", "text/event-stream")
                        .header("Authorization", "Bearer " + token)
                        .build();

                activeSseCall = client.newCall(request);
                okhttp3.Response response = activeSseCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    String line;

                    while (!isActivityDestroyed && (line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String jsonString = line.substring(5).trim();

                            if (!jsonString.equals("ping") && !jsonString.isEmpty()) {
                                NotificationListResponse.ServerNotification sn =
                                        new Gson().fromJson(jsonString, NotificationListResponse.ServerNotification.class);

                                boolean isRead = false;
                                if (sn.getUserNotifications() != null && !sn.getUserNotifications().isEmpty()) {
                                    isRead = sn.getUserNotifications().get(0).isRead();
                                }

                                ItemNotification newNoti = new ItemNotification(
                                        String.valueOf(sn.getId()), sn.getTitle(), sn.getBody(), sn.getCreatedAt(), isRead
                                );
                                runOnUiThread(() -> {
                                    if (!isActivityDestroyed) {
                                        viewModel.addRealtimeNotification(newNoti);
                                        Toast.makeText(NotificationActivity.this, "Bạn có thông báo mới!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SSE_ERROR", "Mất kết nối Stream, đang thử kết nối lại...", e);
                if (!isActivityDestroyed) {
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                    connectToNotificationStream(); // Tự động kết nối lại sau 5 giây nếu lỗi
                }
            }
        });
        sseThread.start();
    }
    private void updateEmptyStateUI(boolean isEmpty) {
        if (isEmpty) {
            binding.rvNotifications.setVisibility(View.GONE);
            binding.emptyNotiState.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.rvNotifications.setVisibility(View.VISIBLE);
            binding.emptyNotiState.getRoot().setVisibility(View.GONE);
        }
    }
    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        super.onDestroy();
        if (activeSseCall != null) {
            activeSseCall.cancel();
        }
        if (sseThread != null && sseThread.isAlive()) {
            sseThread.interrupt();
        }
    }
}