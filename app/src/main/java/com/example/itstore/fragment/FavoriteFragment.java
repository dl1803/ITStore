package com.example.itstore.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.itstore.R;
import com.example.itstore.activity.LoginActivity;
import com.example.itstore.activity.ProductDetailActivity;
import com.example.itstore.adapter.FavoriteAdapter;
import com.example.itstore.databinding.FragmentFavoriteBinding;
import com.example.itstore.model.Product;
import com.example.itstore.model.WishlistItem;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.ProductDetailViewModel;
import com.example.itstore.viewmodel.WishlistViewModel;
import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {
    private FragmentFavoriteBinding binding;
    private FavoriteAdapter adapter;
    private WishlistViewModel wishlistViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();

        if (token == null || token.isEmpty()) {
            binding.rvFavorite.setVisibility(View.GONE);
            binding.layoutRequireLogin.setVisibility(View.VISIBLE);
            binding.tvRequireLoginMsg.setText("Đăng nhập để xem danh sách yêu thích của bạn");
            binding.btnLoginNow.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            });
        } else {
            binding.layoutRequireLogin.setVisibility(View.GONE);
            binding.rvFavorite.setVisibility(View.VISIBLE);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wishlistViewModel = new ViewModelProvider(requireActivity()).get(WishlistViewModel.class);

        FavoriteAdapter.OnFavoriteClickListener favoriteListener = new FavoriteAdapter.OnFavoriteClickListener() {
            @Override
            public void onRemoveFavorite(Product product) {
                wishlistViewModel.removeFromWishlist(product.getId());
            }

            @Override
            public void onAddToCart(Product product) {
                if (product.getVariants() != null && product.getVariants().size() > 1) {
                    Toast.makeText(requireContext(), "Vui lòng chọn phiên bản bạn muốn mua", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                    intent.putExtra("PRODUCT_INFO", product);
                    startActivity(intent);
                } else {
                    ProductDetailViewModel detailViewModel = new ViewModelProvider(requireActivity()).get(ProductDetailViewModel.class);
                    int defaultVariantId = 1;
                    String defaultVariantName = "Mặc định";
                    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                        defaultVariantId = product.getVariants().get(0).getId();
                        defaultVariantName = product.getVariants().get(0).getVersion();
                    }
                    detailViewModel.addToCart(product, defaultVariantId, defaultVariantName, 1);
                    Toast.makeText(requireContext(), "Đã thêm " + product.getName() + " vào giỏ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra("PRODUCT_INFO", product);
                startActivity(intent);
            }
        };

        adapter = new FavoriteAdapter(requireContext(), new ArrayList<>(), favoriteListener);
        binding.rvFavorite.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvFavorite.setAdapter(adapter);

        wishlistViewModel.getWishlistItems().observe(getViewLifecycleOwner(), wishlistItems -> {
            List<Product> productList = new ArrayList<>();
            if (wishlistItems != null) {
                for (WishlistItem item : wishlistItems) {
                    productList.add(item.getProduct());
                }
            }
            adapter.updateList(productList);
        });

        wishlistViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                wishlistViewModel.clearToastMessage();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
        if (token != null && !token.isEmpty()) {
            wishlistViewModel.loadWishlistIfNeeded();
        }
    }
}