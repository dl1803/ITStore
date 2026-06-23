package com.example.itstore.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.itstore.adapter.ProductAdapter;
import com.example.itstore.api.RetrofitClient;
import com.example.itstore.databinding.FragmentCategoryProductBinding;
import com.example.itstore.dialog.FilterProductDialog;
import com.example.itstore.model.Brand;
import com.example.itstore.model.BrandResponse;
import com.example.itstore.model.Product;
import com.example.itstore.model.ProductResponse;
import com.example.itstore.repository.ProductRepository;
import com.example.itstore.viewmodel.HomeViewModel;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryProductFragment extends Fragment {
    private FragmentCategoryProductBinding binding;
    private ProductAdapter productAdapter;
    private ProductRepository productRepository;
    private int currentCategoryId = -1;
    private double currentMinPrice = 0;
    private double currentMaxPrice = Double.MAX_VALUE;
    private List<Integer> currentBrandIds = new ArrayList<>();
    private List<Brand> fetchedBrands = new ArrayList<>();
    private String currentSort = "";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        productRepository = ProductRepository.getInstance(requireContext());
        if (getArguments() != null) {
            String catIdStr = getArguments().getString("CATEGORY_ID", "-1");
            currentCategoryId = Integer.parseInt(catIdStr);
            String categoryName = getArguments().getString("CATEGORY_NAME", "Danh mục");
            binding.tvCategoryTitle.setText(categoryName);
        }
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>());
        binding.rvCategoryProduct.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategoryProduct.setAdapter(productAdapter);

        fetchBrandsForFilter();
        loadProductsByCategory();
        binding.btnFilter.setOnClickListener(v -> {
            openFilterDialog();
        });
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });
        binding.tvSort.setOnClickListener(v -> {
            showSortMenu();
        });
    }
    private void fetchBrandsForFilter() {
        productRepository.getBrands(new Callback<BrandResponse>() {
            @Override
            public void onResponse(Call<BrandResponse> call, Response<BrandResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    fetchedBrands = response.body().getData();
                }
            }
            @Override
            public void onFailure(Call<BrandResponse> call, Throwable t) {
                Log.e("LỖI_API_BRAND", "Không lấy được danh sách hãng ở màn Category: " + t.getMessage());
            }
        });
    }
    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.tvSort);

        popupMenu.getMenu().add(0, 1, 0, "Mặc định");
        popupMenu.getMenu().add(0, 2, 0, "Mới nhất");
        popupMenu.getMenu().add(0, 3, 0, "Cũ nhất");

        popupMenu.setOnMenuItemClickListener(item -> {
            String selectedTitle = item.getTitle().toString();

            binding.tvSort.setText("Sắp xếp: " + selectedTitle);

            switch (item.getItemId()) {
                case 2:
                    currentSort = "newest";
                    break;
                case 3:
                    currentSort = "oldest";
                    break;
                default:
                    currentSort = "";
                    break;
            }
            loadProductsByCategory();
            return true;
        });

        popupMenu.show();
    }
    private void loadProductsByCategory() {
        Integer apiCategoryId = (currentCategoryId == -1) ? null : currentCategoryId;
        Double apiMinPrice = (currentMinPrice <= 0) ? null : currentMinPrice;
        Double apiMaxPrice = (currentMaxPrice == Double.MAX_VALUE) ? null : currentMaxPrice;
        Integer apiBrandId = (currentBrandIds != null && !currentBrandIds.isEmpty()) ? currentBrandIds.get(0) : null;
        String apiSort = currentSort.isEmpty() ? null : currentSort;

        productRepository.getProducts(
                1, 20, null, apiCategoryId, apiBrandId, apiMinPrice, apiMaxPrice, apiSort,
                new Callback<ProductResponse>() {
                    @Override
                    public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            productAdapter.updateList(response.body().getData());
                            binding.tvItemCount.setText("Tìm thấy " + response.body().getPagination().getTotal() + " sản phẩm");
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductResponse> call, Throwable t) {
                        Log.e("LỖI_API", "Không tải được sản phẩm danh mục: " + t.getMessage());
                    }
                });
    }
    private void openFilterDialog() {
        FilterProductDialog dialog = new FilterProductDialog();
        dialog.setBrandList(fetchedBrands);
        dialog.setPreviousSelection(currentMinPrice, currentMaxPrice, currentBrandIds);
        dialog.setOnFilterAppliedListener((min, max, brandIds) -> {
            this.currentMinPrice = min;
            this.currentMaxPrice = max;
            this.currentBrandIds = brandIds;
            loadProductsByCategory();
        });
        dialog.show(getChildFragmentManager(), "FilterProductDialog");
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
