package com.example.itstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.itstore.R;
import com.example.itstore.adapter.BannerAdapter;
import com.example.itstore.adapter.CategoryAdapter;
import com.example.itstore.adapter.ProductAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.ActivityMainBinding;
import com.example.itstore.model.NotificationResponse;
import com.example.itstore.model.TokenRegistrationRequest;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.HomeViewModel;
import com.example.itstore.viewmodel.NotificationViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NotificationViewModel notificationViewModel;
    private NavController navController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());


            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            navController = navHostFragment.getNavController();
        NavInflater inflater = navController.getNavInflater();
        NavGraph graph = inflater.inflate(R.navigation.nav_graph);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("navigate_to")) {
            String destination = intent.getStringExtra("navigate_to");
            if ("cart".equals(destination)) {
                graph.setStartDestination(R.id.nav_cart);
            } else if ("favorite".equals(destination)) {
                graph.setStartDestination(R.id.nav_favorite);
            }
            intent.removeExtra("navigate_to");
        }
        navController.setGraph(graph);
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_search) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        FirebaseApp.initializeApp(this);
        registerNotificationToken();

                // Xử lí sự kiện Click và Kéo để di chuyển Floating Button
            binding.fabAiChat.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY;
                private float startX, startY;
                private static final int DRAG_THRESHOLD = 15; // Phân biệt Click và Kéo

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            // Khi vừa chạm vào
                            dX = view.getX() - event.getRawX();
                            dY = view.getY() - event.getRawY();
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;

                        case MotionEvent.ACTION_MOVE:
                            // Khi kéo đi
                            View parentView = (View) view.getParent();

                            // Tính toán tọa độ dự kiến khi kéo
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;

                            // Kẹp tọa độ X (Không cho lọt ra viền Trái / Phải)
                            if (newX < 0) {
                                newX = 0;
                            } else if (newX > parentView.getWidth() - view.getWidth()) {
                                newX = parentView.getWidth() - view.getWidth();
                            }

                            // Kẹp tọa độ Y (Không cho lọt ra viền Trên / Dưới)
                            if (newY < 0) {
                                newY = 0;
                            } else if (newY > parentView.getHeight() - view.getHeight()) {
                                newY = parentView.getHeight() - view.getHeight();
                            }

                            // Set tọa độ an toàn cho Bong bóng chat
                            view.animate()
                                    .x(newX)
                                    .y(newY)
                                    .setDuration(0)
                                    .start();
                            break;

                        case MotionEvent.ACTION_UP:
                            float endX = event.getRawX();
                            float endY = event.getRawY();
                            if (Math.abs(endX - startX) < DRAG_THRESHOLD &&
                                    Math.abs(endY - startY) < DRAG_THRESHOLD) {
                                view.performClick();
                            } else {
                                // Auto trả về mép màn hinh
                                View parent = (View) view.getParent();
                                int parentWidth = parent.getWidth();
                                float targetX;

                                if (view.getX() + (view.getWidth() / 2f) < parentWidth / 2f) {
                                    targetX = 16f;
                                } else {
                                    targetX = parentWidth - view.getWidth() - 16f;
                                }

                                view.animate()
                                        .x(targetX)
                                        .setDuration(200)
                                        .start();
                            }
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });

            binding.fabAiChat.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AiChatBoxActivity.class));
            });
    }
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("navigate_to")) {
            String destination = intent.getStringExtra("navigate_to");
            try {
                if ("cart".equals(destination)) {
                    navController.navigate(R.id.nav_cart);
                } else if ("favorite".equals(destination)) {
                    navController.navigate(R.id.nav_favorite);
                }
            } catch (Exception e) {
                Log.e("Loi_Chuyen_Trang", "Lỗi ở onNewIntent: " + e.getMessage());
            }
            intent.removeExtra("navigate_to");
        }
    }
    private void registerNotificationToken() {
        String accessToken = SharedPrefsManager.getInstance(this).getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            notificationViewModel.registerFCMToken(token);
        });
    }

}
