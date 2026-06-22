package com.example.itstore.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.itstore.R;
import com.example.itstore.databinding.ActivityOrderSuccessBinding;
import com.example.itstore.model.Order;
import com.example.itstore.utils.CartManager;
import com.example.itstore.repository.WishlistRepository;

public class OrderSuccessActivity extends AppCompatActivity {

    private ActivityOrderSuccessBinding binding;
    private Order order;
    private int currentOrderId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupClickListeners();

        Intent intent = getIntent();
        Uri data = intent.getData();

        // 1. LUỒNG TỪ PAYOS TRẢ VỀ (Web -> App)
        if (data != null && "itstore".equals(data.getScheme()) && "payment".equals(data.getHost())) {
            String path = data.getPath();
            if ("/success".equals(path)) {
                // TODO: XỬ LÝ THANH TOÁN THÀNH CÔNG
                Toast.makeText(this, "Thanh toán PayOS thành công!", Toast.LENGTH_LONG).show();
                CartManager.getInstance().clearPurchasedItems();
                try {
                    String idParam = data.getQueryParameter("orderId");
                    if (idParam != null) {
                        currentOrderId = Integer.parseInt(idParam);
                    }
                } catch (Exception e) {}
            } else if ("/cancel".equals(path)) {
                // TODO: XỬ LÝ KHI USER HỦY HOẶC THANH TOÁN LỖI
                Toast.makeText(this, "Đã hủy giao dịch thanh toán PayOS!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            // 2. LUỒNG TIỀN MẶT COD (Chuyển nội bộ từ CheckoutActivity sang)
        } else {
            currentOrderId = intent.getIntExtra("ORDER_ID", -1);
            CartManager.getInstance().clearPurchasedItems();
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnContinueBuy.setOnClickListener(v -> {
            WishlistRepository.markNeedRefresh();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        binding.tvViewDetail.setOnClickListener(v -> {
            if (currentOrderId != -1) {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", currentOrderId);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, OrderHistoryActivity.class);
                intent.putExtra("TAB_TO_OPEN", "PENDING");
                startActivity(intent);
            }
            finish();
        });
    }
}