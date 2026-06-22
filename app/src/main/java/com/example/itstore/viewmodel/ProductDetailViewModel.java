package com.example.itstore.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.util.Log;

import com.example.itstore.model.CartItem;
import com.example.itstore.model.Product;
import com.example.itstore.model.ProductResponse;
import com.example.itstore.model.SingleProductResponse;
import com.example.itstore.repository.ProductRepository;
import com.example.itstore.utils.CartManager;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.CartResponse;
import com.example.itstore.model.AddCartItemRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailViewModel extends AndroidViewModel {
    private final ProductRepository repository;
    private final MutableLiveData<Product> productDetailLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>();
    public ProductDetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = ProductRepository.getInstance(application);
    }
    public LiveData<Product> getProductDetailLiveData() {
        return productDetailLiveData;
    }

    public LiveData<String> getToastMessageLiveData() {
        return toastMessageLiveData;
    }
    public void fetchProductDetail(String slug) {
        repository.getProductBySlug(slug, new Callback<SingleProductResponse>() {
            @Override
            public void onResponse(Call<SingleProductResponse> call, Response<SingleProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    productDetailLiveData.setValue(response.body().getData());
                } else {
                    toastMessageLiveData.setValue("Không thể tải chi tiết sản phẩm");
                }
            }

            @Override
            public void onFailure(Call<SingleProductResponse> call, Throwable t) {
                toastMessageLiveData.setValue("Lỗi kết nối server");
            }
        });
    }
    public void fetchProductDetailByIdFallback(int productId, String productName) {
        repository.getProducts(1, 50, productName, null, null, null, null, null, new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<com.example.itstore.model.ProductResponse> call, retrofit2.Response<com.example.itstore.model.ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> list = response.body().getData();
                    String realSlug = null;

                    if (list != null) {
                        for (Product p : list) {
                            if (p.getId() == productId) {
                                realSlug = p.getSlug();
                                break;
                            }
                        }
                    }

                    if (realSlug != null && !realSlug.isEmpty()) {
                        fetchProductDetail(realSlug);
                    } else {
                        toastMessageLiveData.setValue("Không tìm thấy dữ liệu gốc của sản phẩm!");
                    }
                } else {
                    toastMessageLiveData.setValue("Lỗi đối chiếu dữ liệu sản phẩm!");
                }
            }

            @Override
            public void onFailure(Call<com.example.itstore.model.ProductResponse> call, Throwable t) {
                toastMessageLiveData.setValue("Lỗi kết nối mạng khi tìm sản phẩm!");
            }
        });
    }
    public void addToCart(Product product, int variantId, String variantName, double finalPrice) {
        repository.addCartItem(variantId, 1, new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            android.widget.Toast.makeText(getApplication(), "Đã thêm vào giỏ hàng!", android.widget.Toast.LENGTH_SHORT).show();
                            CartManager.getInstance().markCartNeedRefresh();
                        } else {
                            android.widget.Toast.makeText(getApplication(), "Không thể thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<CartResponse> call, Throwable t) {
                        android.widget.Toast.makeText(getApplication(), "Lỗi mạng", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }
}