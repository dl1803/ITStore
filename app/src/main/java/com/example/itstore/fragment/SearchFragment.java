package com.example.itstore.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.itstore.R;
import com.example.itstore.activity.LoginActivity;
import com.example.itstore.activity.ProductDetailActivity;
import com.example.itstore.adapter.ProductAdapter;
import com.example.itstore.databinding.FragmentSearchBinding;
import com.example.itstore.dialog.FilterProductDialog;
import com.example.itstore.model.Brand;
import com.example.itstore.model.Category;
import com.example.itstore.model.Product;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.HomeViewModel;
import com.example.itstore.viewmodel.ProductDetailViewModel;
import com.example.itstore.viewmodel.SearchViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private ProductAdapter productAdapter;
    private int currentCategoryId = -1;
    private double currentMinPrice = 0;
    private double currentMaxPrice = Double.MAX_VALUE;
    private List<Integer> currentBrandIds = new ArrayList<>();
    private List<Brand> fetchedBrands = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.getListBrandsLiveData().observe(getViewLifecycleOwner(), brands -> {
            if (brands != null) {
                fetchedBrands = brands;
            }
        });
        viewModel.fetchBrands();
        viewModel.getSearchHistoryLiveData().observe(getViewLifecycleOwner(), this::renderHistoryChips);

        binding.tvClearHistory.setOnClickListener(v -> viewModel.clearHistory());
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>());
        productAdapter.setOnProductInteractionListener(new ProductAdapter.OnProductInteractionListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra("PRODUCT_INFO", product);
                startActivity(intent);
            }
            @Override
            public void onFavoriteClick(Product product, int position) {
                String token = SharedPrefsManager.getInstance(requireContext()).getAccessToken();
                if (token == null || token.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu yêu thích!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                } else {
                    viewModel.toggleFavorite(product, new SearchViewModel.FavoriteToggleCallback() {
                        @Override
                        public void onSuccess(boolean isNowFavorite) {
                            product.setFavorite(isNowFavorite);
                            productAdapter.notifyItemChanged(position);
                            Toast.makeText(requireContext(), isNowFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

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
        binding.rvSearchRecommend.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSearchRecommend.setAdapter(productAdapter);

        viewModel.getHasMore().observe(getViewLifecycleOwner(), hasMore -> {
            if (hasMore != null) {
                isLastPage = !hasMore;
            }
        });
        viewModel.searchResults.observe(getViewLifecycleOwner(), products -> {
            isLoading = false;
            if (products != null) {
                productAdapter.updateList(products);
                String query = binding.edtSearch.getText().toString().trim();
                boolean isFiltering = !query.isEmpty() || currentCategoryId != -1 || currentMinPrice > 0 || !currentBrandIds.isEmpty();
                if (isFiltering) {
                    binding.tvResultRecommend.setText("Tìm thấy " + products.size() + " sản phẩm");
                } else {
                    binding.tvResultRecommend.setText("Gợi ý tìm kiếm");
                }
            }
        });

        binding.rvSearchRecommend.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Chỉ tính toán khi người dùng đang vuốt cuộn xuống dưới
                if (dy > 0) {
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && !isLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                    && firstVisibleItemPosition >= 0
                                    && totalItemCount >= 2) {
                                loadMoreSearchResults();
                            }
                        }
                    }
                }
            }
        });
        // Lay data cua chip tu api
        HomeViewModel homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        homeViewModel.getCategoryListLiveData().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                binding.chipGroupFilter.removeAllViews();

                Chip chipAll = (Chip) getLayoutInflater().inflate(R.layout.item_chip_search, binding.chipGroupFilter, false);
                chipAll.setText("Tất cả");
                chipAll.setCheckable(true);
                chipAll.setChecked(true);
                chipAll.setTag("-1");
                binding.chipGroupFilter.addView(chipAll);

                for (Category category : categories) {
                    if (category.getId() != -1) {
                        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_search, binding.chipGroupFilter, false);
                        chip.setText(category.getName());
                        chip.setCheckable(true);
                        chip.setTag(category.getId());
                        binding.chipGroupFilter.addView(chip);
                    }
                }
            }
        });
        // Bật bàn phím khi mới vào
        binding.edtSearch.requestFocus();
        binding.edtSearch.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT);
        }, 200);

        binding.btnBack.setOnClickListener(v -> {
            hideKeyboard(view);
            Navigation.findNavController(v).popBackStack();
        });

        // KHI BẤM NÚT TÌM KIẾM BẰNG TỪ KHÓA
        binding.ivSearch.setOnClickListener(v -> {
            String keyword = binding.edtSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                viewModel.saveKeyword(keyword);
            }
            performCombinedSearch();
            hideKeyboard(v);
        });

        // KHI BẤM VÀO CHIP -> LỌC NHANH DANH MỤC
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int selectedChipId = group.getCheckedChipId();
            if (selectedChipId != View.NO_ID) {
                Chip selectedChip = group.findViewById(selectedChipId);
                String categoryId = selectedChip.getTag().toString();
                String categoryName = selectedChip.getText().toString();

                if (!categoryId.equals("-1")) {
                    Bundle bundle = new Bundle();
                    bundle.putString("CATEGORY_ID", categoryId);
                    bundle.putString("CATEGORY_NAME", categoryName);
                    Navigation.findNavController(requireView()).navigate(R.id.action_nav_search_to_nav_category_product, bundle);
                }
                // Nếu bấm nút tất cả thì lọc tại chỗ
                else {
                    currentCategoryId = -1;
                    performCombinedSearch();
                }
                group.clearCheck();
            }
        });

        // KHI BẤM NÚT MỞ DIALOG LỌC
        binding.btnOpenFilter.setOnClickListener(v -> {
            FilterProductDialog dialog = new FilterProductDialog();
            dialog.setBrandList(fetchedBrands);
            dialog.setPreviousSelection(currentMinPrice, currentMaxPrice, currentBrandIds);
            dialog.setOnFilterAppliedListener((min, max, brandIds) -> {
                currentMinPrice = min;
                currentMaxPrice = max;
                currentBrandIds = brandIds;

                performCombinedSearch(); // Gọi hàm lọc
            });

            dialog.show(getChildFragmentManager(), "FilterProductDialog");
            hideKeyboard(v);
        });
        performCombinedSearch();
    }
    // Hàm lọc
    private void performCombinedSearch() {
        currentPage = 1;
        isLastPage = false;
        isLoading = true;

        String query = binding.edtSearch.getText().toString().trim();
        if (!query.isEmpty() || currentCategoryId != -1 || currentMinPrice > 0 || !currentBrandIds.isEmpty()) {
            binding.tvResultRecommend.setText("Kết quả lọc");
            binding.btnOpenFilter.setVisibility(View.VISIBLE);
        } else {
            binding.tvResultRecommend.setText("Gợi ý tìm kiếm");
            binding.btnOpenFilter.setVisibility(View.GONE);
        }
        viewModel.searchProducts(query, currentCategoryId, currentMinPrice, currentMaxPrice, currentBrandIds, currentPage, false);
    }
    private void loadMoreSearchResults() {
        isLoading = true;
        currentPage++;
        String query = binding.edtSearch.getText().toString().trim();
        viewModel.searchProducts(query, currentCategoryId, currentMinPrice, currentMaxPrice, currentBrandIds, currentPage, true);
    }
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void renderHistoryChips(List<String> historyList) {
        binding.chipGroupHistory.removeAllViews();

        if (historyList == null || historyList.isEmpty()) {
            binding.layoutSearchHistory.setVisibility(View.GONE);
            return;
        }

        binding.layoutSearchHistory.setVisibility(View.VISIBLE);

        for (String keyword : historyList) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_search_history_chip, binding.chipGroupHistory, false);
            chip.setText(keyword);
            chip.setOnClickListener(v -> {
                binding.edtSearch.setText(keyword);
                binding.edtSearch.setSelection(keyword.length());
                viewModel.saveKeyword(keyword);
                performCombinedSearch();
                hideKeyboard(v);
            });
            chip.setOnCloseIconClickListener(v -> viewModel.removeKeyword(keyword));
            binding.chipGroupHistory.addView(chip);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}