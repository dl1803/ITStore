package com.example.itstore.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.itstore.R;
import com.example.itstore.adapter.OrderDetailAdapter;
import com.example.itstore.databinding.ActivityOrderDetailBinding;
import com.example.itstore.model.Order;
import com.example.itstore.model.OrderItem;
import com.example.itstore.model.OrderTimeline;
import com.example.itstore.model.Product;
import com.example.itstore.model.ProductImage;
import com.example.itstore.model.ProductVariant;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.example.itstore.viewmodel.OrderDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.itstore.adapter.OrderTimelineAdapter;
import com.example.itstore.databinding.DialogTimelineBinding;

public class OrderDetailActivity extends AppCompatActivity {

    private ActivityOrderDetailBinding binding;
    private Order currentOrder;
    private OrderDetailViewModel viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(OrderDetailViewModel.class);

        int orderId = getIntent().getIntExtra("ORDER_ID", -1);

        if (orderId != -1) {
            viewModel.fetchOrderDetail(orderId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy mã đơn hàng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel.getOrderDetail().observe(this, order -> {
            if (order != null) {
                this.currentOrder = order;
                setupOrderInfo();
                updateBottomButtons();
                setupClickListeners();
            } else {
                Toast.makeText(this, "Lỗi: Đơn hàng không tồn tại!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getIsCancelSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Đã hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                viewModel.fetchOrderDetail(Integer.parseInt(currentOrder.getOrderId()));
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
            }
        });

        viewModel.getCancelError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsConfirmSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Cảm ơn bạn! Hãy đánh giá sản phẩm nhé.", Toast.LENGTH_SHORT).show();

                // Lưu trạng thái đơn hàng đã xác nhận vào SharedPreferences (bộ nhớ đt)
                getSharedPreferences("ConfirmedOrders", MODE_PRIVATE)
                        .edit()
                        .putBoolean(currentOrder.getOrderId(), true)
                        .apply();

                binding.btnConfirmReceived.setVisibility(View.GONE);
                binding.btnRefundOrderDetail.setVisibility(View.GONE);
                binding.btnReviewDetail.setVisibility(View.VISIBLE);

                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
            }
        });
    }


    private int getStatusColor(String statusVN) {
        switch (statusVN) {
            case "Đã hủy": return Color.parseColor("#FF3B30"); // Đỏ
            case "Đã giao": return Color.parseColor("#4CAF50"); // Xanh lá
            case "Đang giao": return Color.parseColor("#2196F3"); // Xanh dương
            default: return Color.parseColor("#F57C00"); // Cam
        }
    }

    private void setupOrderInfo() {
        binding.btnBack.setOnClickListener(v -> finish());
        DecimalFormat formatter = new DecimalFormat("###,###,###");

        binding.tvOrderId.setText("Mã đơn hàng: #" + currentOrder.getOrderId());
        binding.tvOrderDate.setText("Ngày đặt: " + currentOrder.getOrderDate());


        String rawDate = currentOrder.getOrderDate();
        if (rawDate != null && rawDate.contains("T")) {
            String formattedDate = rawDate.substring(0, 16).replace("T", " ");
            binding.tvOrderDate.setText("Ngày đặt: " + formattedDate);
        } else {
            binding.tvOrderDate.setText("Ngày đặt: " + rawDate);
        }

        String cusName = currentOrder.getCustomerName() != null ? currentOrder.getCustomerName() : "Tên khách hàng";
        String cusPhone = currentOrder.getPhoneNumber() != null ? currentOrder.getPhoneNumber() : "SĐT";

        binding.tvCustomerName.setText(cusName + " | " + cusPhone);

        String address = currentOrder.getShippingAddress();
        if (address != null && !address.isEmpty()) {
            binding.tvAddress.setText(address);
        } else {
            binding.tvAddress.setText("Chưa cập nhật địa chỉ giao hàng");
        }

        String statusVN = currentOrder.getStatusVN();
        binding.tvOrderStatus.setText("Trạng thái: " + statusVN);
        binding.tvOrderStatus.setTextColor(getStatusColor(statusVN));

        List<OrderItem> itemList = currentOrder.getItems();

        if (itemList == null) {
            itemList = new ArrayList<>();
        }

        OrderDetailAdapter adapter = new OrderDetailAdapter(itemList);
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        // Tính tiền tổng
        long shippingFee = currentOrder.getShippingFee();
        long discount = currentOrder.getDiscount();
        long finalTotal = currentOrder.getTotalPrice();

        long subTotal = finalTotal - shippingFee + discount;


        binding.tvSubTotal.setText(formatter.format(subTotal) + "đ");
        binding.tvShippingFee.setText(formatter.format(shippingFee) + "đ");
        binding.tvTotal.setText(formatter.format(finalTotal) + "đ");

        if (discount > 0) {
            binding.layoutVoucher.setVisibility(View.VISIBLE);
            binding.tvVoucherDiscount.setText("-" + formatter.format(discount) + "đ");
        } else {
            binding.layoutVoucher.setVisibility(View.GONE);
        }

        String method = currentOrder.getPaymentMethod();
        if (method != null && method.equals("bank_transfer")){
            binding.tvPaymentMethodLabel.setText("Banking");
            binding.tvPaymentMethodLabel.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
        } else {
            binding.tvPaymentMethodLabel.setText("COD");
            binding.tvPaymentMethodLabel.setTextColor(Color.parseColor("#FF8C00")); // Cam đậm
        }

        String pStatus = currentOrder.getPaymentStatus();
        if (pStatus != null && pStatus.equals("paid")) {
            binding.tvPaymentStatus.setText("Đã thanh toán");
            binding.tvPaymentStatus.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
        } else if (pStatus != null && pStatus.equals("refunded")) {
            binding.tvPaymentStatus.setText("Đã hoàn tiền");
            binding.tvPaymentStatus.setTextColor(Color.parseColor("#2196F3")); // Xanh dương
        } else {
            binding.tvPaymentStatus.setText("Chưa thanh toán");
            binding.tvPaymentStatus.setTextColor(Color.parseColor("#FF3B30")); // Đỏ
        }
    }

    private void updateBottomButtons() {
        binding.btnCancelOrderDetail.setVisibility(View.GONE);
        binding.btnRefundOrderDetail.setVisibility(View.GONE);
        binding.btnConfirmReceived.setVisibility(View.GONE);
        binding.btnReviewDetail.setVisibility(View.GONE);


        String rawStatus = currentOrder.getStatus();
        if (rawStatus == null) return;

        switch (rawStatus.toLowerCase()) {
            case "pending":
                binding.btnCancelOrderDetail.setVisibility(View.VISIBLE);
                break;
            case "shipping":
                break;
            case "delivered":
                boolean isConfirmed = getSharedPreferences("ConfirmedOrders", MODE_PRIVATE)
                        .getBoolean(currentOrder.getOrderId(), false);
                if (isConfirmed) {
                    binding.btnReviewDetail.setVisibility(View.VISIBLE);
                } else {
                    binding.btnConfirmReceived.setVisibility(View.VISIBLE);
                    binding.btnRefundOrderDetail.setVisibility(View.VISIBLE);
                }
                break;
            case "cancelled":
                break;
        }
    }

    private void setupClickListeners() {
        binding.btnCancelOrderDetail.setOnClickListener(v -> {
            showCancelReasonDialog();
        });
        binding.tvViewTimeline.setOnClickListener(v -> showTimelineDialog());
        binding.btnConfirmReceived.setOnClickListener(v -> {
            int orderId = Integer.parseInt(currentOrder.getOrderId());
            viewModel.confirmReceived(orderId);
        });

        binding.btnRefundOrderDetail.setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, RefundRequestActivity.class);
            intent.putExtra("ORDER_DATA", currentOrder);
            startActivity(intent);
        });

        binding.btnReviewDetail.setOnClickListener(v -> {
            if (currentOrder != null && currentOrder.getItems() != null && !currentOrder.getItems().isEmpty()) {
                com.example.itstore.model.OrderItem firstItem = currentOrder.getItems().get(0);

                Intent intent = new Intent(OrderDetailActivity.this, WriteReviewActivity.class);

                intent.putExtra("ORDER_ITEM_ID", firstItem.getOrderItemId());
                intent.putExtra("PRODUCT_NAME", firstItem.getProductName());
                intent.putExtra("PRODUCT_IMAGE", firstItem.getImageUrl());
                intent.putExtra("PRODUCT_VARIANT", firstItem.getProductType());

                startActivity(intent);
            } else {
                Toast.makeText(OrderDetailActivity.this, "Không tìm thấy sản phẩm để đánh giá!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showCancelReasonDialog(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_cancel_order);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        RadioGroup rgReasons = dialog.findViewById(R.id.rgCancelReasons);
        EditText edtOtherReason = dialog.findViewById(R.id.edtOtherReason);
        AppCompatButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        AppCompatButton btnClose = dialog.findViewById(R.id.btnClose);

        rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbReasonOther) {
                edtOtherReason.setVisibility(View.VISIBLE);
            } else {
                edtOtherReason.setVisibility(View.GONE);
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            int selectedId = rgReasons.getCheckedRadioButtonId();
            if (selectedId ==-1) {
                Toast.makeText(this, "Vui lòng chọn lý do hủy đơn hàng!", Toast.LENGTH_SHORT).show();
                return;
            }

            String cancelReason = "";
            if (selectedId == R.id.rbReasonOther) {
                cancelReason = edtOtherReason.getText().toString().trim();
                if (cancelReason.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập lý do khác!", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                android.widget.RadioButton rbSelected = dialog.findViewById(selectedId);
                cancelReason = rbSelected.getText().toString();
            }

            int orderId = Integer.parseInt(currentOrder.getOrderId());
            viewModel.cancelOrder(orderId, cancelReason);
            dialog.dismiss();
        });
        dialog.show();
      }
    private void showTimelineDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogTimelineBinding dialogBinding = DialogTimelineBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        List<OrderTimeline> realTimelines = new ArrayList<>();

        List<Order.TimelineItem> apiTimelineList = currentOrder.getTimelineList();

        if (apiTimelineList != null && !apiTimelineList.isEmpty()) {
            for (Order.TimelineItem item : apiTimelineList) {

                String statusTitle = getTimelineStatusVN(item.newStatus);

                String timeFormatted = item.changedAt;
                if (timeFormatted != null && timeFormatted.contains("T")) {
                    timeFormatted = timeFormatted.substring(0, 16).replace("T", " ");
                }

                String description = (item.note != null && !item.note.isEmpty())
                        ? item.note
                        : getDefaultTimelineDescription(item.newStatus);

                String content = description + "\n" + timeFormatted;

                // Thêm vào đầu mảng (add 0) để sự kiện mới nhất nổi lên trên cùng
                realTimelines.add(0, new OrderTimeline(statusTitle, content));
            }
        } else {
            realTimelines.add(new OrderTimeline("Đặt hàng", "Hệ thống đã ghi nhận đơn hàng.\n" + currentOrder.getOrderDate()));
        }

        OrderTimelineAdapter adapter = new OrderTimelineAdapter(realTimelines);
        dialogBinding.rvOrderTimeline.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.rvOrderTimeline.setAdapter(adapter);
        dialog.show();
    }

    private String getTimelineStatusVN(String rawStatus) {
        if (rawStatus == null) return "Cập nhật";
        switch (rawStatus.toLowerCase()) {
            case "pending": return "Tạo đơn hàng";
            case "confirmed": return "Đã xác nhận";
            case "preparing": return "Đang chuẩn bị hàng";
            case "packed": return "Đã đóng gói";
            case "shipping": return "Đang giao hàng";
            case "delivered": return "Giao hàng thành công";
            case "cancelled": return "Đơn hàng đã hủy";
            default: return "Cập nhật đơn hàng";
        }
    }

    private String getDefaultTimelineDescription(String rawStatus) {
        if (rawStatus == null) return "Trạng thái đơn hàng đã thay đổi.";
        switch (rawStatus.toLowerCase()) {
            case "pending": return "Hệ thống đã ghi nhận đơn hàng của bạn.";
            case "confirmed": return "Người bán đã xác nhận đơn hàng.";
            case "preparing": return "Kho đang xuất linh kiện và tiến hành đóng gói.";
            case "packed": return "Đơn hàng đã được bàn giao cho đơn vị vận chuyển.";
            case "shipping": return "Shipper đang trên đường giao hàng đến bạn.";
            case "delivered": return "Kiện hàng đã được giao thành công đến người nhận.";
            case "cancelled": return "Đơn hàng đã bị hủy theo yêu cầu.";
            default: return "Cập nhật trạng thái mới.";
        }
    }
}