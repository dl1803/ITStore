package com.example.itstore.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.R;
import com.example.itstore.activity.CheckoutActivity;
import com.example.itstore.activity.LoginActivity;
import com.example.itstore.activity.ProductDetailActivity;
import com.example.itstore.adapter.CartAdapter;
import com.example.itstore.adapter.DiscountAdapter;
import com.example.itstore.databinding.FragmentCartBinding;
import com.example.itstore.model.CartItem;
import com.example.itstore.model.Coupon;
import com.example.itstore.model.Product;
import com.example.itstore.utils.CartManager;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.CartViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {
    private FragmentCartBinding binding;
    private CartViewModel cartViewModel;
    private CartAdapter cartAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
        if (token == null || token.isEmpty()) {
            binding.rvCart.setVisibility(View.GONE);
            binding.layoutCartDiscount.setVisibility(View.GONE);
            binding.layoutCheckoutTotal.setVisibility(View.GONE);
            binding.layoutRequireLogin.setVisibility(View.VISIBLE);
            binding.btnLoginNow.setOnClickListener(v -> startActivity(new Intent(requireContext(), LoginActivity.class)));
        } else {
            binding.layoutRequireLogin.setVisibility(View.GONE);
            binding.rvCart.setVisibility(View.VISIBLE);
            binding.layoutCartDiscount.setVisibility(View.VISIBLE);
            binding.layoutCheckoutTotal.setVisibility(View.VISIBLE);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        setupRecyclerView();
        observeViewModel();

        binding.layoutCartDiscount.setOnClickListener(v -> showDiscountBottomSheet());

        binding.btnCheckout.setOnClickListener(v -> {
            List<CartItem> itemsToBuy = new ArrayList<>();
            List<CartItem> currentCart = cartViewModel.getCartItems().getValue();
            if (currentCart != null) {
                for (CartItem item : currentCart) {
                    if (item.isSelected()) {
                        itemsToBuy.add(item);
                    }
                }
            }
            if (itemsToBuy.isEmpty()) {
                Toast.makeText(requireContext(), "Bạn chưa chọn sản phẩm nào để mua!", Toast.LENGTH_SHORT).show();
                return;
            }
            CartManager.getInstance().setCheckoutList(itemsToBuy);
            Intent intent = new Intent(requireActivity(), CheckoutActivity.class);
            intent.putExtra("VOUCHER_CODE", cartViewModel.getSelectedVoucherCode());
            intent.putExtra("VOUCHER_AMOUNT", cartViewModel.getSelectedVoucherAmount());
            intent.putExtra("VOUCHER_ID", cartViewModel.getSelectedVoucherId());
            startActivity(intent);
        });

        binding.cbBuyAll.setOnClickListener(v -> cartViewModel.toggleSelectAll(binding.cbBuyAll.isChecked()));

        cartViewModel.loadCartIfNeeded();
        cartViewModel.fetchActiveCoupons();
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(new ArrayList<>(), new CartAdapter.CartClickListener() {
            @Override
            public void onIncrease(CartItem item, int position) {
                cartViewModel.increaseQuantity(item, position);
            }

            @Override
            public void onDecrease(CartItem item, int position) {
                cartViewModel.decreaseQuantity(item, position);
            }

            @Override
            public void onDelete(CartItem item, int position) {
                cartViewModel.removeItem(position);
            }

            @Override
            public void onItemSelected(CartItem item, boolean isChecked) {
                item.setSelected(isChecked);
                cartViewModel.checkAllSelectedStatus();
                cartViewModel.calculateTotal();
            }

            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra("PRODUCT_INFO", product);
                startActivity(intent);
            }
        });
        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(cartAdapter);
    }

    private void observeViewModel() {
        cartViewModel.getCartItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                cartAdapter.setCartList(items);
                int selectedCount = 0;
                for (CartItem item : items) {
                    if (item.isSelected()) selectedCount++;
                }
                binding.btnCheckout.setText("Mua hàng (" + selectedCount + ")");
            }
        });

        cartViewModel.getFinalTotal().observe(getViewLifecycleOwner(), finalTotal -> {
            if (finalTotal == null || finalTotal == 0) {
                binding.tvTotalPrice.setText("0 đ");
            } else {
                binding.tvTotalPrice.setText(String.format("%,.0f đ", finalTotal));
            }
        });

        cartViewModel.getDiscountAmount().observe(getViewLifecycleOwner(), discount -> {
            if (discount != null && discount > 0) {
                binding.tvCartDiscount.setVisibility(View.VISIBLE);
                binding.tvCartDiscount.setText(String.format("Tiết kiệm: -%,.0f đ", discount));
            } else {
                binding.tvCartDiscount.setVisibility(View.GONE);
            }
        });

        cartViewModel.getAppliedVoucherCode().observe(getViewLifecycleOwner(), code -> {
            if (code != null && !code.isEmpty()) {
                binding.tvCartVoucherCode.setText(code);
                binding.tvCartVoucherCode.setTextColor(getResources().getColor(R.color.orange_primary));
            } else {
                binding.tvCartVoucherCode.setText("Chọn hoặc nhập mã");
                binding.tvCartVoucherCode.setTextColor(android.graphics.Color.parseColor("#888888"));
            }
        });

        cartViewModel.getIsAllSelectedLiveData().observe(getViewLifecycleOwner(), isAllSelected -> {
            binding.cbBuyAll.setChecked(isAllSelected);
        });

        cartViewModel.getCouponError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });

        cartViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                cartViewModel.clearToastMessage();
            }
        });
    }

    private void showDiscountBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_discount, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText edtCode = dialogView.findViewById(R.id.edtVoucherCode);
        Button btnApply = dialogView.findViewById(R.id.btnApplyVoucher);
        RecyclerView rvDiscounts = dialogView.findViewById(R.id.rvDiscountList);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);

        List<Coupon> coupons = cartViewModel.getCouponList().getValue();

        if (coupons == null || coupons.isEmpty()) {
            Toast.makeText(requireContext(), "Hiện tại chưa có mã giảm giá nào!", Toast.LENGTH_SHORT).show();
            cartViewModel.fetchActiveCoupons();
            return;
        }

        rvDiscounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        DiscountAdapter discountAdapter = new DiscountAdapter(coupons, coupon -> {
            cartViewModel.applyVoucher(coupon);
            bottomSheetDialog.dismiss();
        });
        rvDiscounts.setAdapter(discountAdapter);

        btnApply.setOnClickListener(v -> {
            String inputCode = edtCode.getText().toString().trim();
            if (inputCode.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập mã!", Toast.LENGTH_SHORT).show();
                return;
            }
            Coupon foundCoupon = null;
            for (Coupon c : coupons) {
                if (c.getCode().equalsIgnoreCase(inputCode)) {
                    foundCoupon = c;
                    break;
                }
            }
            if (foundCoupon != null) {
                cartViewModel.applyVoucher(foundCoupon);
                bottomSheetDialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Mã giảm giá không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cartViewModel != null) {
            cartViewModel.loadCartIfNeeded();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}