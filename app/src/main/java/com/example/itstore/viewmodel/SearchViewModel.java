package com.example.itstore.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.itstore.model.Brand;
import com.example.itstore.model.BrandResponse;
import com.example.itstore.model.Category;
import com.example.itstore.model.Pagination;
import com.example.itstore.model.Product;
import com.example.itstore.model.ProductResponse;
import com.example.itstore.model.WishlistMessageResponse;
import com.example.itstore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.itstore.repository.SearchHistoryRepository;
import com.example.itstore.repository.WishlistRepository;

public class SearchViewModel extends AndroidViewModel {
    private final ProductRepository productRepository;
    private MutableLiveData<List<Product>> _searchResults = new MutableLiveData<>();
    public LiveData<List<Product>> searchResults = _searchResults;
    private List<Product> allProducts;
    private List<Category> allCategories;
    private final WishlistRepository wishlistRepository;
    private MutableLiveData<List<Brand>> listBrandsLiveData = new MutableLiveData<>();
    private final SearchHistoryRepository historyRepository;
    private final MutableLiveData<List<String>> searchHistoryLiveData = new MutableLiveData<>();

    public LiveData<List<String>> getSearchHistoryLiveData() {
        return searchHistoryLiveData;
    }
    public MutableLiveData<List<Brand>> getListBrandsLiveData() {
        return listBrandsLiveData;
    }
    private final MutableLiveData<Boolean> _hasMore = new MutableLiveData<>(true);
    public LiveData<Boolean> getHasMore() { return _hasMore; }
    public SearchViewModel(@NonNull Application application) {
        super(application);
        productRepository = ProductRepository.getInstance(application);
        historyRepository = SearchHistoryRepository.getInstance(application);
        wishlistRepository = WishlistRepository.getInstance(application);
        searchHistoryLiveData.setValue(historyRepository.getHistory());
    }
    public void searchProducts(String query, int categoryId, double minPrice, double maxPrice, List<Integer> brandIds, int page, boolean isLoadMore) {

        Integer apiCategoryId = (categoryId == -1) ? null : categoryId;
        Double apiMinPrice = (minPrice <= 0) ? null : minPrice;
        Double apiMaxPrice = (maxPrice == Double.MAX_VALUE) ? null : maxPrice;
        Integer apiBrandId = (brandIds != null && !brandIds.isEmpty()) ? brandIds.get(0) : null;

        productRepository.getProducts(page, 10, query, apiCategoryId, apiBrandId, apiMinPrice, apiMaxPrice, null, new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> newProducts = response.body().getData();
                    if (response.body().getPagination() != null) {
                        Pagination p = response.body().getPagination();
                        boolean calculatedHasMore = p.getPage() < p.getTotalPages();
                        _hasMore.setValue(calculatedHasMore);
                    }

                    if (isLoadMore) {
                        List<Product> currentList = _searchResults.getValue();
                        List<Product> combinedList = new ArrayList<>();
                        if (currentList != null) {
                            combinedList.addAll(currentList);
                        }
                        combinedList.addAll(newProducts);
                        _searchResults.setValue(combinedList);
                    } else {
                        _searchResults.setValue(newProducts);
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Log.e("API_ERR", "Lỗi tìm kiếm sản phẩm: " + t.getMessage());
                _hasMore.setValue(false);
            }
        });
    }
    public void fetchBrands() {
        productRepository.getBrands(new Callback<BrandResponse>() {
            @Override
            public void onResponse(Call<BrandResponse> call, Response<BrandResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        listBrandsLiveData.setValue(response.body().getData());
                    }
                }
            }
            @Override
            public void onFailure(Call<BrandResponse> call, Throwable t) {
                Log.e("API_ERR", "Lỗi lấy Brand bên Search: " + t.getMessage());
            }
        });
    }
    public void saveKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        historyRepository.addKeyword(keyword.trim());
        searchHistoryLiveData.setValue(historyRepository.getHistory());
    }

    public void removeKeyword(String keyword) {
        historyRepository.removeKeyword(keyword);
        searchHistoryLiveData.setValue(historyRepository.getHistory());
    }

    public void clearHistory() {
        historyRepository.clearAll();
        searchHistoryLiveData.setValue(historyRepository.getHistory());
    }
    public interface FavoriteToggleCallback {
        void onSuccess(boolean isNowFavorite);
        void onError(String message);
    }
    public void toggleFavorite(Product product, FavoriteToggleCallback callback) {
        if (product.isFavorite()) {
            wishlistRepository.removeFromWishlist(product.getId(), new Callback<WishlistMessageResponse>() {
                @Override
                public void onResponse(Call<WishlistMessageResponse> call, Response<WishlistMessageResponse> response) {
                    if (response.isSuccessful()) {
                        WishlistRepository.removeFromCache(product.getId());
                        callback.onSuccess(false);
                    } else {
                        callback.onError("Lỗi: Không thể xóa khỏi yêu thích!");
                    }
                }
                @Override
                public void onFailure(Call<WishlistMessageResponse> call, Throwable t) {
                    callback.onError("Lỗi mạng: " + t.getMessage());
                }
            });
        } else {
            wishlistRepository.addToWishlist(product.getId(), new Callback<WishlistMessageResponse>() {
                @Override
                public void onResponse(Call<WishlistMessageResponse> call, Response<WishlistMessageResponse> response) {
                    if (response.isSuccessful()) {
                        WishlistRepository.addToCache(product.getId());
                        callback.onSuccess(true);
                    } else {
                        callback.onError("Lỗi: Không thể thêm vào yêu thích!");
                    }
                }
                @Override
                public void onFailure(Call<WishlistMessageResponse> call, Throwable t) {
                    callback.onError("Lỗi mạng: " + t.getMessage());
                }
            });
        }
    }
}