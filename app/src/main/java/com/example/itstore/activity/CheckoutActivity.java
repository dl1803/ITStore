package com.example.itstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.R;
import com.example.itstore.adapter.CheckoutAdapter;
import com.example.itstore.adapter.DiscountAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.ActivityCheckoutBinding;
import com.example.itstore.model.Address;
import com.example.itstore.model.CartItem;
import com.example.itstore.model.Coupon;
import com.example.itstore.model.CouponResponse;
import com.example.itstore.model.CreateOrderRequest;
import com.example.itstore.model.Discount;
import com.example.itstore.model.Order;
import com.example.itstore.model.OrderItemRequest;
import com.example.itstore.utils.CartManager;
import com.example.itstore.viewmodel.AddressViewModel;
import com.example.itstore.viewmodel.CheckoutViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.net.InterfaceAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    ActivityCheckoutBinding binding;
    private CheckoutViewModel checkoutViewModel;
    private AddressViewModel addressViewModel;
    private CheckoutAdapter adapter;

    private String appliedVoucherCode = "";
    private int selectedAddressId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        checkoutViewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));


        addressViewModel = new ViewModelProvider(this).get(AddressViewModel.class);

        observeViewModel();


        binding.cardInfo.setOnClickListener(v -> {
            showAddressSelectionDialog();
        });

        binding.layoutDiscount.setOnClickListener(v -> {
            showDiscountBottomSheet();
        });
        
        List<CartItem> receivedItems = CartManager.getInstance().getCheckoutList();

        if (receivedItems != null && !receivedItems.isEmpty()) {
            checkoutViewModel.loadCheckoutData(receivedItems);
            Intent intent = getIntent();

            String passedCode = intent.getStringExtra("VOUCHER_CODE");
            double passedAmount = intent.getDoubleExtra("VOUCHER_AMOUNT", 0.0);

            int passedId = intent.getIntExtra("VOUCHER_ID", -1);

            if (passedCode != null && passedAmount > 0) {
                appliedVoucherCode = passedCode;
                checkoutViewModel.applyDiscount(passedAmount, passedId);
                binding.tvVoucherCode.setText(passedCode);
                binding.tvVoucherCode.setTextColor(getResources().getColor(R.color.orange_primary));
            }
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm nào!", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.rbCod.setChecked(true);

        binding.btnCheckout.setOnClickListener(v -> {
            String paymentMethod = "cod";
            if (binding.rbPayOs.isChecked()) {
                paymentMethod = "bank_transfer";
            }

            List<CartItem> purchasedItems = checkoutViewModel.getCheckoutItems().getValue();
            if (purchasedItems == null || purchasedItems.isEmpty() || selectedAddressId == -1) {
                Toast.makeText(this, "Thiếu thông tin đơn hàng hoặc địa chỉ!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<OrderItemRequest> orderItems = new ArrayList<>();
            for (CartItem item : purchasedItems){
                orderItems.add(new OrderItemRequest(item.getVariantId(), item.getQuantity()));
            }

            Integer couponId = checkoutViewModel.getSelectedCouponId().getValue();
            if (couponId != null && couponId == -1) couponId = null;

            CreateOrderRequest request = new CreateOrderRequest(
                    selectedAddressId,
                    couponId,
                    paymentMethod,
                    "Vui lòng giao trong giờ hành chính",
                    orderItems
            );

            binding.btnCheckout.setEnabled(false);
            binding.btnCheckout.setText("Đang xử lý...");
            checkoutViewModel.placeOrder(request);
        });
        addressViewModel.fetchAddresses(this);
        checkoutViewModel.fetchActiveCoupons();


    }

    // TÍNH NĂNG 1: CHỌN ĐỊA CHỈ TỪ DANH SÁCH
    private void showAddressSelectionDialog() {
        List<Address> addressList = addressViewModel.getAddressList().getValue();

        if (addressList == null || addressList.isEmpty()) {
            Toast.makeText(this, "Bạn chưa có địa chỉ nào! Vui lòng vào Cài đặt để thêm.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] addressStrings = new String[addressList.size()];
        for (int i = 0; i < addressList.size(); i++) {
            Address addr = addressList.get(i);
            addressStrings[i] = addr.getRecipient() + " | " + addr.getPhoneNumber() + "\n"
                    + addr.getStreet() + ", " + addr.getWard() + ", " + addr.getDistrict() + ", " + addr.getProvince();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn địa chỉ nhận hàng");

        builder.setItems(addressStrings, (dialog, which) -> {
            Address selectedAddress = addressList.get(which);

            updateAddressUI(selectedAddress);
        });
        builder.show();
    }

    // TÍNH NĂNG 2: MỞ BOTTOM SHEET CHỌN VOUCHER
    private void showDiscountBottomSheet() {
        List<Coupon> realCoupons = checkoutViewModel.getCouponList().getValue();

        if (realCoupons == null || realCoupons.isEmpty()) {
            Toast.makeText(this, "Hiện tại chưa có mã giảm giá nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_discount, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText edtCode = dialogView.findViewById(R.id.edtVoucherCode);
        Button btnApply = dialogView.findViewById(R.id.btnApplyVoucher);
        RecyclerView rvDiscounts = dialogView.findViewById(R.id.rvDiscountList);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);


        rvDiscounts.setLayoutManager(new LinearLayoutManager(this));
        DiscountAdapter discountAdapter = new DiscountAdapter(realCoupons, coupon -> {
            applyDiscountToCart(coupon);
            bottomSheetDialog.dismiss();
        });


        rvDiscounts.setAdapter(discountAdapter);

        btnApply.setOnClickListener(v -> {
            String inputCode = edtCode.getText().toString().trim();
            if (inputCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã!", Toast.LENGTH_SHORT).show();
                return;
            }

            Coupon foundCoupon = null;
            for (Coupon c : realCoupons) {
                if (c.getCode().equalsIgnoreCase(inputCode)) {
                    foundCoupon = c;
                    break;
                }
            }

            if (foundCoupon != null) {
                applyDiscountToCart(foundCoupon);
                bottomSheetDialog.dismiss();
            } else {
                Toast.makeText(this, "Mã giảm giá không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();


    }

    private void applyDiscountToCart(Coupon coupon) {
        double currentSubtotal = checkoutViewModel.getSubtotalPrice().getValue() != null
                ? checkoutViewModel.getSubtotalPrice().getValue() : 0;

        if (currentSubtotal <= 0) {
            Toast.makeText(this, "Đơn hàng trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSubtotal < coupon.getMinOrderValue()) {
            String requiredAmount = String.format("%,.0f đ", coupon.getMinOrderValue());
            Toast.makeText(this, "Chưa đạt mức tối thiểu " + requiredAmount + " để dùng mã này!", Toast.LENGTH_LONG).show();
            return;
        }

        double discountAmount = 0;
        if ("percent".equals(coupon.getDiscountType())) {
            discountAmount = currentSubtotal * (coupon.getDiscountValue() / 100.0);
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        appliedVoucherCode = coupon.getCode();

        checkoutViewModel.applyDiscount(discountAmount, coupon.getId());

        Toast.makeText(this, "Đã áp dụng mã: " + coupon.getCode(), Toast.LENGTH_SHORT).show();
    }



    private void observeViewModel() {
        checkoutViewModel.getCheckoutItems().observe(this, items -> {
            adapter = new CheckoutAdapter(items);
            binding.rvCheckoutItems.setAdapter(adapter);
            binding.rvCheckoutItems.setNestedScrollingEnabled(false);
        });

        checkoutViewModel.getSubtotalPrice().observe(this, price ->
                binding.tvSubtotalPrice.setText(String.format("%,.0f đ", price)));

        checkoutViewModel.getShippingFee().observe(this, fee ->
                binding.tvShippingPrice.setText(String.format("%,.0f đ", fee)));

        checkoutViewModel.getTotalDiscount().observe(this, discount -> {
            if (discount > 0) {
                binding.tvDiscount.setText(String.format("%,.0f đ", discount));

                binding.tvVoucherDiscount.setText(String.format("-%,.0f đ", discount));

            } else {
                binding.tvDiscount.setText("0đ");
                binding.tvVoucherDiscount.setText("0đ");
            }
        });

        checkoutViewModel.getIsOrderSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                if (binding.rbCod.isChecked()){
                    // Luồng 1: Tiền mặt -> Xóa giỏ hàng và sang trang Success
                    CartManager.getInstance().clearPurchasedItems();
                    CartManager.getInstance().markCartNeedRefresh();
                    Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();

                    int orderId = checkoutViewModel.getCreatedOrderId().getValue();

                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    startActivity(intent);
                    finish();
                } else {
                    // Luồng 2: PayOS -> KHÔNG XÓA GIỎ HÀNG Ở ĐÂY!
                    Toast.makeText(this, "Đang khởi tạo cổng thanh toán...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        checkoutViewModel.getFinalTotalPrice().observe(this, finalTotal -> {
            String formattedTotal = String.format("%,.0f đ", finalTotal);
            binding.tvTotalPrice.setText(formattedTotal);
            binding.tvTotal.setText(formattedTotal);
        });

        checkoutViewModel.getOrderError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            binding.btnCheckout.setEnabled(true);
        });

        addressViewModel.getAddressList().observe(this , addressList -> {
            if (addressList != null && !addressList.isEmpty()) {
                Address defaultAddress = null;
                for (Address address : addressList) {
                    if (address.isDefault()) {
                        defaultAddress = address;
                        break;
                    }
                }
                if (defaultAddress != null) {
                    updateAddressUI(defaultAddress);
                }else {
                    updateAddressUI(addressList.get(0));
                }
            }
        });

        checkoutViewModel.getPayosPaymentUrl().observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                Toast.makeText(this, "Đang chuyển hướng sang cổng thanh toán...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                startActivity(intent);
            }
        });

        checkoutViewModel.getSelectedCouponId().observe(this, couponId -> {
            if (couponId != null && couponId != -1) {
                binding.tvVoucherCode.setText(appliedVoucherCode);
                binding.tvVoucherCode.setTextColor(getResources().getColor(R.color.orange_primary));
            } else {
                binding.tvVoucherCode.setText("Chọn hoặc nhập mã");
                binding.tvVoucherCode.setTextColor(android.graphics.Color.parseColor("#888888"));
            }
        });
    }

    private void updateAddressUI(Address address) {
        selectedAddressId = address.getId();
        binding.tvUserInfo.setText(address.getRecipient() + " | " + address.getPhoneNumber());
        String fullAddress = address.getStreet() + ", " + address.getWard() + ", "
                + address.getDistrict() + ", " + address.getProvince();
        binding.tvAddress.setText(fullAddress);

        if (checkoutViewModel != null) {
            checkoutViewModel.calculateShippingFee(address.getId());        }
    }

    // Auto gọi khi mở lại màn hình
    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.btnCheckout != null) {
            binding.btnCheckout.setEnabled(true);
            binding.btnCheckout.setText("Đặt hàng");
        }
    }
}