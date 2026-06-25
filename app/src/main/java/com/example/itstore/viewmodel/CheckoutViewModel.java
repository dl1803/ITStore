package com.example.itstore.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.itstore.api.GhnApiClient;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.CartItem;
import com.example.itstore.model.Coupon;
import com.example.itstore.model.CouponResponse;
import com.example.itstore.model.CreateOrderRequest;
import com.example.itstore.model.GhnFeeData;
import com.example.itstore.model.GhnFeeRequest;
import com.example.itstore.model.GhnResponse;
import com.example.itstore.model.CreateOrderResponse;
import com.example.itstore.model.PayOsPaymentResponse;
import com.example.itstore.model.ShipmentFeeRequest;
import com.example.itstore.model.ShipmentFeeResponse;
import com.example.itstore.repository.OrderRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutViewModel extends AndroidViewModel {
    private final OrderRepository repository;

    private final String GHN_TOKEN = "5369cdb2-3fd4-11f1-b84f-e215adfdd13e";
    private final int GHN_SHOP_ID = 6403105;
    private final MutableLiveData<List<CartItem>> checkoutItems = new MutableLiveData<>();
    private final MutableLiveData<Double> subtotalPrice = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> shippingFee = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalDiscount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> finalTotalPrice = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isOrderSuccess = new MutableLiveData<>();

    private final MutableLiveData<String> orderError = new MutableLiveData<>();

    private final MutableLiveData<List<Coupon>> couponList = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedCouponId = new MutableLiveData<>(null);
    private final MutableLiveData<String> payosPaymentUrl = new MutableLiveData<>();
    private final MutableLiveData<Integer> createdOrderId = new MutableLiveData<>();


    public CheckoutViewModel(@NonNull Application application) {
        super(application);
        repository = OrderRepository.getInstance(application);
    }

    public LiveData<List<CartItem>> getCheckoutItems() { return checkoutItems; }
    public LiveData<Double> getSubtotalPrice() { return subtotalPrice; }
    public LiveData<Double> getShippingFee() { return shippingFee; }
    public LiveData<Double> getTotalDiscount() { return totalDiscount; }
    public LiveData<Double> getFinalTotalPrice() { return finalTotalPrice; }
    public LiveData<Boolean> getIsOrderSuccess() { return isOrderSuccess; }
    public LiveData<String> getOrderError() { return orderError; }

    public LiveData<List<Coupon>> getCouponList() { return couponList; }
    public LiveData<Integer> getSelectedCouponId() { return selectedCouponId; }

    public LiveData<Integer> getCreatedOrderId() { return createdOrderId; }
    public LiveData<String> getPayosPaymentUrl() { return payosPaymentUrl; }
    public void loadCheckoutData(List<CartItem> items) {
        checkoutItems.setValue(items);
        calculateMoney(items);
    }

    private void calculateMoney(List<CartItem> items) {
        double subtotal = 0;
        double requestedDiscount = totalDiscount.getValue() != null ? totalDiscount.getValue() : 0;
        if (items != null) {
            for (CartItem item : items) {
                subtotal += (item.getPrice() * item.getQuantity());
            }
        }

        double ship = shippingFee.getValue() != null ? shippingFee.getValue() : 0;
        double actualDiscount = Math.min(requestedDiscount, subtotal);

        double finalPrice = (subtotal - actualDiscount) + ship;

        if(finalPrice < 0) finalPrice = 0;

        subtotalPrice.setValue(subtotal);
        totalDiscount.setValue(actualDiscount);
        finalTotalPrice.setValue(finalPrice);
    }

    public void applyDiscount(double discountAmount, int couponId) {
        totalDiscount.setValue(discountAmount);
        selectedCouponId.setValue(couponId);
        calculateMoney(checkoutItems.getValue());
    }

    public void placeOrder(CreateOrderRequest request) {
        repository.createOrder(request, new Callback<CreateOrderResponse>() {
            @Override
            public void onResponse(Call<CreateOrderResponse> call, Response<CreateOrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        int newOrderId = response.body().getData().getOrderId().getId();
                        createdOrderId.setValue(newOrderId);
                        isOrderSuccess.setValue(true);
                        if ("bank_transfer".equals(request.getPaymentMethod())) {
                            createPayOsLink(newOrderId);
                        }
                    } else {
                        orderError.setValue("Lỗi khi tạo đơn hàng!");
                    }
                }

            @Override
            public void onFailure(Call<CreateOrderResponse> call, Throwable t) {
                orderError.setValue("Lỗi kết nối mạng!");
            }
        });
    }

    public void calculateShippingFee(int addressId) {
        ShipmentFeeRequest request = new ShipmentFeeRequest(addressId);

        repository.calculateShippingFee(request, new Callback<ShipmentFeeResponse>() {
                    @Override
                    public void onResponse(Call<ShipmentFeeResponse> call, Response<ShipmentFeeResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            double realFee = response.body().getData().getShippingFee();
                            shippingFee.setValue(realFee);

                            calculateMoney(checkoutItems.getValue());
                        } else {
                            shippingFee.setValue(30000.0);
                            calculateMoney(checkoutItems.getValue());
                        }
                    }

                    @Override
                    public void onFailure(Call<ShipmentFeeResponse> call, Throwable t) {
                        shippingFee.setValue(30000.0);
                        calculateMoney(checkoutItems.getValue());
                    }
                });
    }
    public void fetchActiveCoupons() {
        repository.getCoupons(new Callback<CouponResponse>() {
            @Override
            public void onResponse(Call<CouponResponse> call, Response<CouponResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    couponList.setValue(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<CouponResponse> call, Throwable t) {

            }
        });
    }

    public void createPayOsLink(int orderId) {
        repository.createPayOsPaymentLink(orderId, new Callback<PayOsPaymentResponse>() {
            @Override
            public void onResponse(Call<PayOsPaymentResponse> call, Response<PayOsPaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    payosPaymentUrl.setValue(response.body().getData().getPaymentUrl());
                }
                 else {
                    orderError.setValue("Lỗi! Không thể tạo link thanh toán PayOS");
                }
            }

            @Override
            public void onFailure(Call<PayOsPaymentResponse> call, Throwable t) {
                orderError.setValue("Lỗi mạng! Không thể kết nối đến hệ thống thanh toán");
            }
        });
    }

}