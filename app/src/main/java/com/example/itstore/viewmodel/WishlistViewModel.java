package com.example.itstore.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.itstore.model.WishlistItem;
import com.example.itstore.model.WishlistMessageResponse;
import com.example.itstore.model.WishlistResponse;
import com.example.itstore.repository.WishlistRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishlistViewModel extends AndroidViewModel {
    private final WishlistRepository repository;
    private final MutableLiveData<List<WishlistItem>> wishlistItems;
    private final MutableLiveData<Set<Integer>> wishlistProductIds;
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public WishlistViewModel(@NonNull Application application) {
        super(application);
        this.repository = WishlistRepository.getInstance(application);
        this.wishlistItems = new MutableLiveData<>(WishlistRepository.getCachedItems());
        this.wishlistProductIds = new MutableLiveData<>(new HashSet<>(WishlistRepository.getCachedProductIds()));
    }

    public void loadWishlistIfNeeded() {
        if (!WishlistRepository.isNeedRefresh()) return;
        fetchWishlist();
    }

    public LiveData<List<WishlistItem>> getWishlistItems() { return wishlistItems; }
    public LiveData<Set<Integer>> getWishlistProductIds() { return wishlistProductIds; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void fetchWishlist() {
        repository.getWishlist(new Callback<WishlistResponse>() {
            @Override
            public void onResponse(Call<WishlistResponse> call, Response<WishlistResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<WishlistItem> items = response.body().getData();
                    WishlistRepository.updateCache(items);
                    wishlistItems.setValue(WishlistRepository.getCachedItems());
                    wishlistProductIds.setValue(new HashSet<>(WishlistRepository.getCachedProductIds()));
                    WishlistRepository.clearNeedRefresh();
                }
            }

            @Override
            public void onFailure(Call<WishlistResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng: Không thể tải danh sách yêu thích");
            }
        });
    }

    public void addToWishlist(int productId) {
        repository.addToWishlist(productId, new Callback<WishlistMessageResponse>() {
            @Override
            public void onResponse(Call<WishlistMessageResponse> call, Response<WishlistMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    WishlistRepository.addToCache(productId);
                    wishlistProductIds.setValue(new HashSet<>(WishlistRepository.getCachedProductIds()));
                    toastMessage.setValue("Đã thêm vào yêu thích");
                    WishlistRepository.markNeedRefresh();
                } else {
                    toastMessage.setValue("Không thể thêm vào yêu thích");
                }
            }

            @Override
            public void onFailure(Call<WishlistMessageResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng: Không thể thêm vào yêu thích");
            }
        });
    }

    public void removeFromWishlist(int productId) {
        repository.removeFromWishlist(productId, new Callback<WishlistMessageResponse>() {
            @Override
            public void onResponse(Call<WishlistMessageResponse> call, Response<WishlistMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    WishlistRepository.removeFromCache(productId);
                    wishlistProductIds.setValue(new HashSet<>(WishlistRepository.getCachedProductIds()));
                    wishlistItems.setValue(WishlistRepository.getCachedItems());
                    toastMessage.setValue("Đã xóa khỏi yêu thích");
                    WishlistRepository.markNeedRefresh();
                } else {
                    toastMessage.setValue("Không thể xóa khỏi yêu thích");
                }
            }

            @Override
            public void onFailure(Call<WishlistMessageResponse> call, Throwable t) {
                toastMessage.setValue("Lỗi mạng: Không thể xóa khỏi yêu thích");
            }
        });
    }

    public boolean isInWishlist(int productId) {
        Set<Integer> ids = wishlistProductIds.getValue();
        return ids != null && ids.contains(productId);
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }
}