package com.example.itstore.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.itstore.R;
import com.example.itstore.activity.LoginActivity;
import com.example.itstore.activity.ProductDetailActivity;
import com.example.itstore.adapter.BannerAdapter;
import com.example.itstore.adapter.BrandFilterAdapter;
import com.example.itstore.adapter.CategoryAdapter;
import com.example.itstore.adapter.ProductAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.FragmentHomeBinding;
import com.example.itstore.model.Category;
import com.example.itstore.model.CategoryResponse;
import com.example.itstore.model.MockDataRepository;
import com.example.itstore.model.Product;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.HomeViewModel;
import com.example.itstore.viewmodel.ProductDetailViewModel;
import com.example.itstore.viewmodel.WishlistViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private WishlistViewModel wishlistViewModel;
    private FragmentHomeBinding binding;

    private RecyclerView rcvProducts;
    private ProductAdapter productAdapter;
    private RecyclerView rcvCategories;
    private CategoryAdapter categoryAdapter;
    private Handler handler;
    private Runnable runBanner;

    private LinearLayout layoutIndicators;
    private View rootView;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                            Product updatedProduct = (Product) result.getData().getSerializableExtra("UPDATED_PRODUCT");
                            if (updatedProduct != null && homeViewModel != null) {
                                homeViewModel.updateProduct(updatedProduct);
                            }
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        wishlistViewModel = new ViewModelProvider(requireActivity()).get(WishlistViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        layoutIndicators = binding.layoutIndicators;

        ViewPager2 viewPager2 = binding.viewPagerBanner;

        BannerAdapter bannerAdapter = new BannerAdapter(new ArrayList<>());
        viewPager2.setAdapter(bannerAdapter);

        handler = new Handler(Looper.getMainLooper());

        homeViewModel.getBannerListLiveData().observe(getViewLifecycleOwner(), banners -> {
            if (banners != null && !banners.isEmpty()) {
                bannerAdapter.updateData(banners);

                // Tự động sinh ra số lượng dấu chấm (dots) tương ứng với số ảnh
                layoutIndicators.removeAllViews();
                for (int i = 0; i < banners.size(); i++) {
                    CardView dot = new CardView(requireContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
                    params.setMargins(8, 0, 8, 0);
                    dot.setLayoutParams(params);
                    dot.setRadius(10f);
                    dot.setCardElevation(0f);

                    if (i == 0) {
                        dot.setCardBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
                        params.width = 60;
                    } else {
                        dot.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
                    }
                    layoutIndicators.addView(dot);
                }

                // Cập nhật lại logic cuộn ảnh tự động
                runBanner = () -> {
                    int curItem = viewPager2.getCurrentItem();
                    int totalItem = banners.size() - 1;
                    if (curItem < totalItem) {
                        viewPager2.setCurrentItem(curItem + 1);
                    } else {
                        viewPager2.setCurrentItem(0);
                    }
                };
                handler.removeCallbacks(runBanner);
                handler.postDelayed(runBanner, 3000);
            }
        });

        // Animation đổi màu dấu chấm khi vuốt tay
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                if (layoutIndicators != null) {
                    for (int i = 0; i < layoutIndicators.getChildCount(); i++) {
                        CardView dot = (CardView) layoutIndicators.getChildAt(i);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();

                        if (i == position) {
                            dot.setCardBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
                            params.width = 60;
                            params.height = 20;
                        } else {
                            dot.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
                            params.width = 20;
                            params.height = 20;
                        }
                        dot.setLayoutParams(params);
                    }
                }

                if (runBanner != null) {
                    handler.removeCallbacks(runBanner);
                    handler.postDelayed(runBanner, 3000);
                }

            }
        });


        rcvProducts = binding.recyclerProduct;
        rcvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), new ProductAdapter.OnProductInteractionListener() {

                // Chuyển sang màn hình chi tiết
                @Override
                public void onProductClick(Product product) {
                    Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                    intent.putExtra("PRODUCT_INFO", product);
                    detailLauncher.launch(intent);
                }

                // Nút tim
                @Override
                public void onFavoriteClick(Product product, int position) {
                    String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
                    if (token == null || token.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu yêu thích!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                    } else {
                        if (wishlistViewModel.isInWishlist(product.getId())) {
                            wishlistViewModel.removeFromWishlist(product.getId());
                        } else {
                            wishlistViewModel.addToWishlist(product.getId());
                        }
                    }
                }

                // Nút thêm vào giỏ hàng
                @Override
                public void onAddToCartClick(Product product) {
                    String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
                    if (token == null || token.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào giỏ!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        return;
                    }

                    if (product.getVariants() == null || product.getVariants().isEmpty() || product.getVariants().size() > 1) {
                        Toast.makeText(requireContext(), "Vui lòng chọn phiên bản bạn muốn mua ở trang chi tiết", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                        intent.putExtra("PRODUCT_INFO", product);
                        startActivity(intent);
                    } else {
                        ProductDetailViewModel detailViewModel = new ViewModelProvider(requireActivity()).get(ProductDetailViewModel.class);

                        int realVariantId = product.getVariants().get(0).getId();
                        String realVariantName = product.getVariants().get(0).getVersion();

                        detailViewModel.addToCart(product, realVariantId, realVariantName, 1);
                        Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng thành công!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        rcvProducts.setAdapter(productAdapter);
        homeViewModel.getHasMoreLiveData().observe(getViewLifecycleOwner(), hasMore -> {
            if (hasMore != null) {
                isLastPage = !hasMore;
            }
        });
        homeViewModel.getProductListLiveData().observe(getViewLifecycleOwner(), products -> {
            isLoading = false;
            if (products != null) {
                Set<Integer> wishlistIds = wishlistViewModel.getWishlistProductIds().getValue();
                if (wishlistIds != null) {
                    for (Product p : products) {
                        p.setFavorite(wishlistIds.contains(p.getId()));
                    }
                }
                productAdapter.updateList(products);
                if (!isLastPage) {
                    binding.nestedScrollView.post(() -> {
                        View child = binding.nestedScrollView.getChildAt(0);
                        if (child != null) {
                            int diff = child.getBottom() - (binding.nestedScrollView.getHeight() + binding.nestedScrollView.getScrollY());
                            if (diff <= 200) {
                                loadMoreHomeProducts();
                            }
                        }
                    });
                }
            }
        });
        binding.nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY) {
                    View child = v.getChildAt(0);
                    if (child != null) {
                        int diff = child.getBottom() - (v.getHeight() + scrollY);
                        if (diff <= 200 && !isLoading && !isLastPage) {
                            loadMoreHomeProducts();
                        }
                    }
                }
            }
        });
        wishlistViewModel.getWishlistProductIds().observe(getViewLifecycleOwner(), productIds -> {
            List<Product> products = homeViewModel.getProductListLiveData().getValue();
            if (products != null) {
                for (Product p : products) {
                    p.setFavorite(productIds.contains(p.getId()));
                }
                productAdapter.notifyDataSetChanged();
            }
        });

        wishlistViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                wishlistViewModel.clearToastMessage();
            }
        });

        categoryAdapter = new CategoryAdapter(requireContext(), new ArrayList<>(), new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                homeViewModel.filterByCategory(category.getId());
            }
        });
        rcvCategories = binding.recyclerCategory;
        rcvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        rcvCategories.setAdapter(categoryAdapter);

        homeViewModel.getCategoryListLiveData().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                categoryAdapter.updateData(categories);
            }
        });

        binding.tvSeeAll.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_search);
        });
        binding.layoutSearchBar.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_search);
        });
        // goi ham lay san pham tu api
        homeViewModel.loadInitialDataIfNeeded();
        return view;
    }
    private void loadMoreHomeProducts() {
        isLoading = true;
        currentPage++;
        homeViewModel.fetchSuggestedProducts(currentPage, true);
    }
    @Override
    public void onResume() {
        super.onResume();
        wishlistViewModel.loadWishlistIfNeeded();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runBanner != null) {
            handler.removeCallbacks(runBanner);
        }
        binding = null;
    }
}