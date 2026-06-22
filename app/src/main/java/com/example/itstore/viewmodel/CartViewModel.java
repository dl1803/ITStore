package com.example.itstore.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.itstore.model.CartItem;
import com.example.itstore.model.CartResponse;
import com.example.itstore.model.Coupon;
import com.example.itstore.model.CouponResponse;
import com.example.itstore.model.UpdateCartItemRequest;
import com.example.itstore.repository.CartRepository;
import com.example.itstore.utils.CartManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartViewModel extends AndroidViewModel {
    private final CartRepository repository;
    private final MutableLiveData<List<CartItem>> cartItemsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> totalPriceLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isAllSelectedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<List<Coupon>> couponList = new MutableLiveData<>();
    private final MutableLiveData<String> couponError = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private Coupon appliedCoupon = null;
    private double selectedVoucherAmount = 0;
    private String selectedVoucherCode = "";
    private int selectedVoucherId = -1;

    private final MutableLiveData<Double> finalTotalLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> discountAmountLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> appliedVoucherCodeLiveData = new MutableLiveData<>("");

    public CartViewModel(@NonNull Application application) {
        super(application);
        this.repository = CartRepository.getInstance(application);
    }
    public void loadCartIfNeeded() {
        if (!CartManager.getInstance().isCartNeedRefresh()) {
            return; // dữ liệu vẫn còn mới, không cần gọi API
        }
        CartManager.getInstance().clearCartNeedRefresh();
        fetchCart();
    }
    public LiveData<Boolean> getIsAllSelectedLiveData() { return isAllSelectedLiveData; }
    public LiveData<List<CartItem>> getCartItems() { return cartItemsLiveData; }
    public LiveData<Double> getTotalPrice() { return totalPriceLiveData; }
    public LiveData<List<Coupon>> getCouponList() { return couponList; }
    public LiveData<String> getCouponError() { return couponError; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Double> getFinalTotal() { return finalTotalLiveData; }
    public LiveData<Double> getDiscountAmount() { return discountAmountLiveData; }
    public LiveData<String> getAppliedVoucherCode() { return appliedVoucherCodeLiveData; }

    public String getSelectedVoucherCode() { return selectedVoucherCode; }
    public double getSelectedVoucherAmount() { return selectedVoucherAmount; }
    public int getSelectedVoucherId() { return selectedVoucherId; }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public void calculateTotal() {
        double total = 0;
        List<CartItem> currentList = cartItemsLiveData.getValue();
        if (currentList != null) {
            for (CartItem item : currentList) {
                if (item.isSelected()) {
                    total += item.getPrice() * item.getQuantity();
                }
            }
        }
        totalPriceLiveData.setValue(total);
        recalculateVoucher(total);
    }

    private void recalculateVoucher(double rawTotal) {
        if (appliedCoupon == null) {
            finalTotalLiveData.setValue(rawTotal);
            discountAmountLiveData.setValue(0.0);
            return;
        }
        if (rawTotal <= 0 || rawTotal < appliedCoupon.getMinOrderValue()) {
            appliedCoupon = null;
            selectedVoucherAmount = 0;
            selectedVoucherCode = "";
            selectedVoucherId = -1;
            appliedVoucherCodeLiveData.setValue("");
            discountAmountLiveData.setValue(0.0);
            finalTotalLiveData.setValue(rawTotal);
            toastMessage.setValue("Đã gỡ mã do không đủ điều kiện tối thiểu!");
            return;
        }
        if ("percent".equals(appliedCoupon.getDiscountType())) {
            selectedVoucherAmount = rawTotal * (appliedCoupon.getDiscountValue() / 100.0);
        } else {
            selectedVoucherAmount = appliedCoupon.getDiscountValue();
        }
        double finalTotal = Math.max(0, rawTotal - selectedVoucherAmount);
        discountAmountLiveData.setValue(selectedVoucherAmount);
        finalTotalLiveData.setValue(finalTotal);
    }

    public void applyVoucher(Coupon coupon) {
        double currentTotal = totalPriceLiveData.getValue() != null ? totalPriceLiveData.getValue() : 0;
        if (currentTotal <= 0) {
            toastMessage.setValue("Vui lòng chọn sản phẩm trước khi áp mã!");
            return;
        }
        if (currentTotal < coupon.getMinOrderValue()) {
            String requiredAmount = String.format("%,.0f đ", coupon.getMinOrderValue());
            toastMessage.setValue("Đơn hàng chưa đạt tối thiểu " + requiredAmount + "!");
            return;
        }
        selectedVoucherCode = coupon.getCode();
        selectedVoucherId = coupon.getId();
        appliedCoupon = coupon;
        appliedVoucherCodeLiveData.setValue(coupon.getCode());
        recalculateVoucher(currentTotal);
        toastMessage.setValue("Đã lưu mã: " + selectedVoucherCode);
    }

    public void increaseQuantity(CartItem item, int position) {
        int newQuantity = item.getQuantity() + 1;
        repository.updateCartItem(item.getVariantId(), newQuantity, new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CartItem> currentList = cartItemsLiveData.getValue();
                    if (currentList != null) {
                        item.setQuantity(newQuantity);
                        cartItemsLiveData.setValue(currentList);
                        calculateTotal();
                    }
                } else {
                    toastMessage.setValue("Không thể cập nhật số lượng");
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng");
            }
        });
    }

    public void decreaseQuantity(CartItem item, int position) {
        if (item.getQuantity() > 1) {
            int newQuantity = item.getQuantity() - 1;
            repository.updateCartItem(item.getVariantId(), newQuantity, new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<CartItem> currentList = cartItemsLiveData.getValue();
                        if (currentList != null) {
                            item.setQuantity(newQuantity);
                            cartItemsLiveData.setValue(currentList);
                            calculateTotal();
                        }
                    } else {
                        toastMessage.setValue("Không thể cập nhật số lượng");
                    }
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    toastMessage.setValue("Lỗi mạng");
                }
            });
        } else {
            repository.removeCartItem(item.getVariantId(), new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<CartItem> currentList = cartItemsLiveData.getValue();
                        if (currentList != null) {
                            currentList.removeIf(cartItem -> cartItem.getVariantId() == item.getVariantId());
                            cartItemsLiveData.setValue(currentList);
                            calculateTotal();
                        }
                    } else {
                        toastMessage.setValue("Không thể xóa sản phẩm");
                    }
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    toastMessage.setValue("Lỗi mạng");
                }
            });
        }
    }

    public void removeItem(int position) {
        List<CartItem> currentList = cartItemsLiveData.getValue();
        if (currentList == null || position >= currentList.size()) return;
        CartItem item = currentList.get(position);
        repository.removeCartItem(item.getVariantId(), new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CartItem> list = cartItemsLiveData.getValue();
                    if (list != null) {
                        list.removeIf(cartItem -> cartItem.getVariantId() == item.getVariantId());
                        cartItemsLiveData.setValue(list);
                        calculateTotal();
                    }
                } else {
                    toastMessage.setValue("Không thể xóa sản phẩm");
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng");
            }
        });
    }

    public void toggleSelectAll(boolean isSelectAll) {
        List<CartItem> currentList = cartItemsLiveData.getValue();
        if (currentList != null) {
            for (CartItem item : currentList) {
                item.setSelected(isSelectAll);
            }
            calculateTotal();
            cartItemsLiveData.setValue(currentList);
            isAllSelectedLiveData.setValue(isSelectAll);
        }
    }

    public void checkAllSelectedStatus() {
        List<CartItem> currentList = cartItemsLiveData.getValue();
        if (currentList == null || currentList.isEmpty()) return;
        boolean allChecked = true;
        for (CartItem item : currentList) {
            if (!item.isSelected()) {
                allChecked = false;
                break;
            }
        }
        isAllSelectedLiveData.setValue(allChecked);
    }

    public void loadCartFromManager() {
        List<CartItem> realCart = CartManager.getInstance().getCartList();
        if (realCart != null) {
            cartItemsLiveData.setValue(realCart);
            calculateTotal();
        }
    }

    public void loadCartFromApi(List<CartItem> itemsFromApi) {
        cartItemsLiveData.setValue(itemsFromApi);
        calculateTotal();
    }

    public void fetchActiveCoupons() {
        repository.getCoupons(new Callback<CouponResponse>() {
            @Override
            public void onResponse(Call<CouponResponse> call, Response<CouponResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    couponList.setValue(response.body().getData());
                } else {
                    couponError.setValue("Không thể tải danh sách khuyến mãi");
                }
            }

            @Override
            public void onFailure(Call<CouponResponse> call, Throwable t) {
                couponError.setValue("Lỗi kết nối server");
            }
        });
    }

    public void fetchCart() {
        repository.getCart(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        List<CartItem> cartItems = response.body().getData();
                        if (cartItems != null) {
                            cartItemsLiveData.setValue(cartItems);
                            calculateTotal();
                        } else {
                            toastMessage.setValue("Lỗi từ server");
                        }
                    } else {
                        if (response.code() == 401) {
                            toastMessage.setValue("Phiên đăng nhập đã hết hạn");
                        } else {
                            toastMessage.setValue("Lỗi từ server");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng: Không thể tải giỏ hàng");
            }
        });
    }
}