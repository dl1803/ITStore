package com.example.itstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.itstore.R;
import com.example.itstore.adapter.ReviewAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.ActivityProductDetailBinding;
import com.example.itstore.adapter.ImagePagerAdapter;
import com.example.itstore.fragment.SpecsBottomSheet;
import com.example.itstore.model.CartItem;
import com.example.itstore.model.Product;
import com.example.itstore.model.ProductImage;
import com.example.itstore.model.ProductReviewsResponse;
import com.example.itstore.model.ProductVariant;
import com.example.itstore.model.Specification;
import com.example.itstore.utils.CartManager;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.ProductDetailViewModel;
import com.example.itstore.viewmodel.WishlistViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private Product currentProduct;
    private Product fullProductData;
    private ProductDetailViewModel detailViewModel;
    private int currentVariantId = 0;
    private String currentVariantName = "";
    private double currentFinalPrice;
    private WishlistViewModel wishlistViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        detailViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ProductDetailViewModel.class);
        wishlistViewModel = new ViewModelProvider(this).get(WishlistViewModel.class);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("PRODUCT_INFO")) {
            currentProduct = (Product) intent.getSerializableExtra("PRODUCT_INFO");
        }

        if (currentProduct == null) {
            Toast.makeText(this, "Lỗi: Không tải được thông tin sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        double basePrice = currentProduct.getPrice();
        double baseOldPrice = currentProduct.getCompareAtPrice();
        currentFinalPrice = basePrice;

        binding.tvPriceOld.setPaintFlags(binding.tvPriceOld.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        binding.tvProductName.setText(currentProduct.getName());
        updatePriceDisplay(basePrice, baseOldPrice);

        List<String> listImages = new ArrayList<>();
        if (currentProduct != null && currentProduct.getImageUrl() != null) {
            listImages.add(currentProduct.getImageUrl());
        }
        ImagePagerAdapter imageAdapter = new ImagePagerAdapter(listImages);
        binding.imgProductDetail.setAdapter(imageAdapter);

        if (currentProduct.getSlug() != null && !currentProduct.getSlug().isEmpty()) {
            // Nếu chuỗi slug truyền sang từ order chỉ toàn là số
            if (currentProduct.getSlug().matches("\\d+")) {
                int realId = Integer.parseInt(currentProduct.getSlug());
                // Tìm slug thật bằng tên và id sản phẩm
                detailViewModel.fetchProductDetailByIdFallback(realId, currentProduct.getName());
            } else {
                fetchFullProductDetail(currentProduct.getSlug());
            }

        } else {
            Toast.makeText(this, "Sản phẩm không có dữ liệu chi tiết!", Toast.LENGTH_SHORT).show();
        }

        wishlistViewModel.loadWishlistIfNeeded();

        wishlistViewModel.getWishlistProductIds().observe(this, productIds -> {
            updateFavoriteIcon(productIds.contains(currentProduct.getId()));
        });

        wishlistViewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                wishlistViewModel.clearToastMessage();
            }
        });

        binding.ivBack.setOnClickListener(v -> finish());
        binding.ivCart.setOnClickListener(v ->{
            goToCartScreen();
        });


        binding.btnAddToCart.setOnClickListener(v -> {
            if (fullProductData == null || currentVariantId == 0) {
                Toast.makeText(this, "Đang tải dữ liệu từ Server...", Toast.LENGTH_SHORT).show();
                return;
            }
            String token = SharedPrefsManager.getInstance(this).getAccessToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(this, LoginActivity.class);
                startActivity(intentLogin);
            } else {
                detailViewModel.addToCart(fullProductData, currentVariantId, currentVariantName, 1);
            }
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            if (fullProductData == null || currentVariantId == 0) {
                Toast.makeText(this, "Lỗi mạng! Không thể mua", Toast.LENGTH_SHORT).show();
                return;
            }
            CartItem buyNowItem = new CartItem(
                    0,
                    0,
                    currentVariantId,
                    1,
                    fullProductData,
                    currentVariantName,
                    currentFinalPrice
            );
            buyNowItem.setSelected(true);

            List<CartItem> fastCheckoutList = new ArrayList<>();
            fastCheckoutList.add(buyNowItem);

            CartManager.getInstance().setCheckoutList(fastCheckoutList);

            Intent intentBuyNow = new Intent(ProductDetailActivity.this, CheckoutActivity.class);
            startActivity(intentBuyNow);
        });


        binding.imgFavoriteItem.setOnClickListener(v -> {
            String token = SharedPrefsManager.getInstance(this).getAccessToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để lưu sản phẩm yêu thích!", Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(this, LoginActivity.class);
                startActivity(intentLogin);
            } else {
                if (wishlistViewModel.isInWishlist(currentProduct.getId())) {
                    wishlistViewModel.removeFromWishlist(currentProduct.getId());
                } else {
                    wishlistViewModel.addToWishlist(currentProduct.getId());
                }
            }
        });
        binding.tvXemCauHinhChiTiet.setOnClickListener(v -> {
            if (fullProductData != null) {
                SpecsBottomSheet bottomSheet = new SpecsBottomSheet(fullProductData);
                bottomSheet.show(getSupportFragmentManager(), "SpecsBottomSheet");
            } else {
                Toast.makeText(this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            }
        });
        // Lấy dữ liệu mô tả thực tế từ sản phẩm
        if (currentProduct != null && currentProduct.getDescription() != null && !currentProduct.getDescription().isEmpty()) {
            binding.tvProductContent.setText(currentProduct.getDescription());
        }
        final boolean[] isDescExpanded = {false};

        View.OnClickListener toggleDescriptionListener = v -> {
            if (isDescExpanded[0]) {
                binding.tvProductContent.setMaxLines(3);
                binding.XemThem.setText("Xem thêm");
                isDescExpanded[0] = false;
            } else {
                binding.tvProductContent.setMaxLines(Integer.MAX_VALUE);
                binding.XemThem.setText("Thu gọn");
                isDescExpanded[0] = true;
            }
        };
        binding.XemThem.setOnClickListener(toggleDescriptionListener);
        binding.tvProductContent.setOnClickListener(toggleDescriptionListener);
        setupReview();

        detailViewModel.getProductDetailLiveData().observe(this, product -> {
            if (product != null) {
                fullProductData = product;
                setupDynamicData();
            }
        });

        detailViewModel.getToastMessageLiveData().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchFullProductDetail(String slug) {
        detailViewModel.fetchProductDetail(slug);
    }
    private void setupDynamicData() {
        if (fullProductData.getDescription() != null ) {
            binding.tvProductContent.setText(fullProductData.getDescription());
        }
        List<String> listImagesUrls = new ArrayList<>();
        if (fullProductData.getImages() != null && !fullProductData.getImages().isEmpty()) {
            for (ProductImage img : fullProductData.getImages()) {
                listImagesUrls.add(img.getImageUrl());
            }
        }
        ImagePagerAdapter imageAdapter = new ImagePagerAdapter(listImagesUrls);
        binding.imgProductDetail.setAdapter(imageAdapter);

        int totalImages = listImagesUrls.isEmpty() ? 1 : listImagesUrls.size();
        binding.tvImageIndicator.setText("1/" + totalImages);

        binding.imgProductDetail.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tvImageIndicator.setText((position + 1) + "/" + totalImages);
            }
        });
        List<ProductVariant> variants = fullProductData.getVariants();
        if (variants != null && !variants.isEmpty()) {
            // Mặc định chọn bản đầu tiên
            ProductVariant var1 = variants.get(0);
            currentVariantId = var1.getId();
            currentVariantName = var1.getVersion();
            currentFinalPrice = var1.getPrice();
            updatePriceDisplay(currentFinalPrice, var1.getCompareAtPrice());

            updateStockStatusUI(var1);

            binding.cardChoice1.setVisibility(View.VISIBLE);
            binding.cardChoice1.setText(currentVariantName);

            binding.cardChoice1.setOnClickListener(v -> {
                binding.cardChoice1.setBackgroundResource(R.color.orange_primary);
                binding.cardChoice1.setTextColor(ContextCompat.getColor(this, R.color.white));
                binding.cardChoice2.setBackgroundResource(android.R.color.transparent);
                binding.cardChoice2.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));

                currentVariantId = var1.getId();
                currentVariantName = var1.getVersion();
                currentFinalPrice = var1.getPrice();
                updatePriceDisplay(currentFinalPrice, var1.getCompareAtPrice());

                updateStockStatusUI(var1);
            });

            // Nếu có bản thứ 2
            if (variants.size() > 1) {
                ProductVariant var2 = variants.get(1);
                binding.cardChoice2.setVisibility(View.VISIBLE);

                binding.cardChoice2.setText(var2.getVersion());

                binding.cardChoice2.setOnClickListener(v -> {
                    binding.cardChoice2.setBackgroundResource(R.color.orange_primary);
                    binding.cardChoice2.setTextColor(ContextCompat.getColor(this, R.color.white));
                    binding.cardChoice1.setBackgroundResource(android.R.color.transparent);
                    binding.cardChoice1.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));

                    currentVariantId = var2.getId();
                    currentVariantName = var2.getVersion();
                    currentFinalPrice = var2.getPrice();
                    updatePriceDisplay(currentFinalPrice, var2.getCompareAtPrice());

                    updateStockStatusUI(var2);
                });
            } else {
                binding.cardChoice2.setVisibility(View.GONE);
            }
        }
        List<Specification> specs = fullProductData.getSpecificationList();

        if (specs != null && !specs.isEmpty()) {
            binding.layoutHighlightSpecs.setVisibility(View.VISIBLE);

            binding.tvHighlightKey1.setText(""); binding.tvHighlightValue1.setText("");
            binding.tvHighlightKey2.setText(""); binding.tvHighlightValue2.setText("");
            binding.tvHighlightKey3.setText(""); binding.tvHighlightValue3.setText("");

            if (specs.size() > 0) {
                binding.tvHighlightKey1.setText(specs.get(0).getKey().toUpperCase() + ":");
                binding.tvHighlightValue1.setText(specs.get(0).getValue());
            }

            if (specs.size() > 1) {
                binding.tvHighlightKey2.setText(specs.get(1).getKey().toUpperCase() + ":");
                binding.tvHighlightValue2.setText(specs.get(1).getValue());
            }

            if (specs.size() > 2) {
                binding.tvHighlightKey3.setText(specs.get(2).getKey().toUpperCase() + ":");
                binding.tvHighlightValue3.setText(specs.get(2).getValue());
            }
        } else {

            binding.layoutHighlightSpecs.setVisibility(View.GONE);
        }
        binding.tvSoldCount.setText("Đã bán " + fullProductData.getSoldCount());
    }
    private void updateStockStatusUI(ProductVariant variant) {
        // kiểm tra xem sản phẩm có bị ngừng bán k
        if ("discontinued".equals(fullProductData.getStatus())) {
            binding.tvStockStatus.setText("Ngừng kinh doanh");
            binding.tvStockStatus.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));

            binding.btnAddToCart.setEnabled(false);
            binding.btnAddToCart.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_gray));
            binding.btnBuyNow.setEnabled(false);
            binding.btnBuyNow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_gray));
            return;
        }

        // nếu còn bán
        int currentStock = variant.getStock();
        if (currentStock > 0) {
            binding.tvStockStatus.setText("Còn hàng");
            binding.tvStockStatus.setText("Còn hàng (" + currentStock + ")");

            binding.tvStockStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_primary));

            binding.btnAddToCart.setEnabled(true);
            binding.btnAddToCart.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_primary));
            binding.btnBuyNow.setEnabled(true);
            binding.btnBuyNow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_primary));
        } else {
            binding.tvStockStatus.setText("Hết hàng");
            binding.tvStockStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

            binding.btnAddToCart.setEnabled(false);
            binding.btnAddToCart.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_gray));
            binding.btnBuyNow.setEnabled(false);
            binding.btnBuyNow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_gray));
        }
    }
    private void setupReview() {
        if (currentProduct == null) return;

        int productId = currentProduct.getId();

        RetrofitClient.getApiService(this).getProductReviews(productId, 1, 3).enqueue(new Callback<ProductReviewsResponse>() {
            @Override
            public void onResponse(Call<com.example.itstore.model.ProductReviewsResponse> call, Response<ProductReviewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductReviewsResponse reviewResponse = response.body();
                    List<ProductReviewsResponse.ReviewDetail> serverReviews = reviewResponse.getData();

                    int totalReviews = 0;
                    if (reviewResponse.getSummary() != null) {
                        totalReviews = reviewResponse.getSummary().getTotalReviews();
                    }

                    binding.tvSeeAllReviews.setText("Xem tất cả (" + totalReviews + ") >");
                    binding.tvRatingCount.setText(totalReviews + " bài đánh giá");

                    if (serverReviews != null && !serverReviews.isEmpty()) {
                        ReviewAdapter adapter = new ReviewAdapter(serverReviews);
                        binding.rvProductReviews.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this));
                        binding.rvProductReviews.setAdapter(adapter);
                    }
                } else {
                    binding.tvRatingCount.setText("0 bài đánh giá");
                    binding.tvSeeAllReviews.setText("Xem tất cả (0) >");
                }
            }

            @Override
            public void onFailure(Call<com.example.itstore.model.ProductReviewsResponse> call, Throwable t) {
                Log.e("API_REVIEW_ERROR", "Lỗi tải đánh giá sản phẩm: " + t.getMessage());
                binding.tvRatingCount.setText("Không thể tải đánh giá");
            }
        });

        binding.tvSeeAllReviews.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, ProductReviewsActivity.class);
            intent.putExtra("PRODUCT_ID", productId);

            // đóng gói thêm danh sách variant đưa qua màn hình xem tất cả
            if (fullProductData != null && fullProductData.getVariants() != null) {
                intent.putExtra("PRODUCT_VARIANTS", (Serializable) fullProductData.getVariants());
            }

            startActivity(intent);
        });
    }

    private void updateFavoriteIcon(boolean isFav) {
        int color = isFav ? android.graphics.Color.parseColor("#FF9800") : android.graphics.Color.parseColor("#B3B3B3");
        binding.imgFavoriteItem.setColorFilter(color);
    }

    private void goToCartScreen() {
        Intent intent = new Intent(ProductDetailActivity.this, MainActivity.class);
        intent.putExtra("navigate_to", "cart");
        intent.putExtra("from_detail", true);
        startActivity(intent);
    }

    private void updatePriceDisplay(double newPrice, double oldPrice) {
        binding.tvPriceNew.setText(String.format(Locale.US, "%,.0f VNĐ", newPrice));
        binding.tvPriceOld.setText(String.format(Locale.US, "%,.0f VNĐ", oldPrice));
        int percent = (int)((oldPrice - newPrice) * 100 / oldPrice);
        if (percent > 0) {
            binding.tvDiscountTag.setText("-" + percent + "%");
        } else {
            binding.tvDiscountTag.setText(-percent + "%");
        }
    }
}