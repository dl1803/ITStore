package com.example.itstore.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.Order;
import com.example.itstore.model.OrderHistoryResponse;
import com.example.itstore.repository.OrderRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryViewModel extends AndroidViewModel {
    private final OrderRepository repository;
    private final MutableLiveData<List<Order>> orderList = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public OrderHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = OrderRepository.getInstance(application);
    }

    public LiveData<List<Order>> getOrderList() { return orderList; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void fetchOrderHistory() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }
        isLoading.setValue(true);
        repository.getOrderHistory(new Callback<OrderHistoryResponse>() {
            @Override
            public void onResponse(Call<OrderHistoryResponse> call, Response<OrderHistoryResponse> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        orderList.setValue(response.body().getData());
                        errorMessage.setValue(null); // có data mới rồi thì xóa lỗi cũ, tránh hiểu lầm
                    } else {
                        errorMessage.setValue("Lỗi từ server: Không lấy được danh sách");
                    }
                } else {
                    errorMessage.setValue("Lỗi kết nối hoặc phiên đăng nhập hết hạn!");
                }
            }

            @Override
            public void onFailure(Call<OrderHistoryResponse> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}