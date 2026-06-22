package com.example.itstore.repository;

import android.content.Context;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.model.AddWishlistRequest;
import com.example.itstore.model.WishlistItem;
import com.example.itstore.model.WishlistMessageResponse;
import com.example.itstore.model.WishlistResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Callback;

public class WishlistRepository {
    private static WishlistRepository instance;
    private final RetrofitClient.ApiService apiService;
    private static boolean needRefresh = true;

    private static List<WishlistItem> cachedItems = null;
    private static Set<Integer> cachedProductIds = new HashSet<>();

    public static void markNeedRefresh() { needRefresh = true; }
    public static boolean isNeedRefresh() { return needRefresh; }
    public static void clearNeedRefresh() { needRefresh = false; }

    public static List<WishlistItem> getCachedItems() { return cachedItems; }
    public static Set<Integer> getCachedProductIds() { return cachedProductIds; }

    public static void updateCache(List<WishlistItem> items) {
        cachedItems = items;
        Set<Integer> ids = new HashSet<>();
        if (items != null) {
            for (WishlistItem item : items) {
                ids.add(item.getProduct().getId());
            }
        }
        cachedProductIds = ids;
    }

    public static void addToCache(int productId) {
        cachedProductIds.add(productId);
    }

    public static void removeFromCache(int productId) {
        cachedProductIds.remove(productId);
        if (cachedItems != null) {
            cachedItems.removeIf(item -> item.getProduct().getId() == productId);
        }
    }

    public static void clearCache() {
        cachedItems = null;
        cachedProductIds = new HashSet<>();
    }

    private WishlistRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public static synchronized WishlistRepository getInstance(Context context) {
        if (instance == null) {
            instance = new WishlistRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getWishlist(Callback<WishlistResponse> callback) {
        apiService.getWishlist().enqueue(callback);
    }

    public void addToWishlist(int productId, Callback<WishlistMessageResponse> callback) {
        apiService.addToWishlist(new AddWishlistRequest(productId)).enqueue(callback);
    }

    public void removeFromWishlist(int productId, Callback<WishlistMessageResponse> callback) {
        apiService.removeFromWishlist(productId).enqueue(callback);
    }
}