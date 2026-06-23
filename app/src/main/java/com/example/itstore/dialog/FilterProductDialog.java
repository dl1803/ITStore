package com.example.itstore.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.itstore.adapter.BrandFilterAdapter;
import com.example.itstore.databinding.DialogFilterBinding;
import com.example.itstore.model.Brand;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class FilterProductDialog extends BottomSheetDialogFragment {

    private DialogFilterBinding binding;
    private OnFilterAppliedListener listener;
    private BrandFilterAdapter brandAdapter;
    private List<Brand> brandList = new ArrayList<>();
    private double previousMinPrice = 0;
    private double previousMaxPrice = Double.MAX_VALUE;
    private List<Integer> previousBrandIds = new ArrayList<>();
    public interface OnFilterAppliedListener {
        void onFilterApplied(double minPrice, double maxPrice, List<Integer> selectedBrandIds);
    }
    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }
    public void setBrandList(List<Brand> brands) {
        if (brands != null) {
            this.brandList = brands;
        }
    }
    public void setPreviousSelection(double minPrice, double maxPrice, List<Integer> brandIds) {
        this.previousMinPrice = minPrice;
        this.previousMaxPrice = maxPrice;
        if (brandIds != null) {
            this.previousBrandIds = new ArrayList<>(brandIds);
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        brandAdapter = new BrandFilterAdapter(brandList, selectedIds -> {});
        binding.rvFilterBrand.setAdapter(brandAdapter);
        if (previousMinPrice > 0) {
            binding.edtMinPrice.setText(String.valueOf((int) previousMinPrice));
        }
        if (previousMaxPrice < Double.MAX_VALUE) {
            binding.edtMaxPrice.setText(String.valueOf((int) previousMaxPrice));
        }
        if (!previousBrandIds.isEmpty()) {
            brandAdapter.setSelectedBrandIds(previousBrandIds);
        }
        binding.btnApplyFilter.setOnClickListener(v -> {
            String minStr = binding.edtMinPrice.getText().toString().trim();
            String maxStr = binding.edtMaxPrice.getText().toString().trim();
            double min = minStr.isEmpty() ? 0 : Double.parseDouble(minStr);
            double max = maxStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxStr);
            List<Integer> selectedBrandIds = brandAdapter.getSelectedBrandIds();

            if (listener != null) {
                listener.onFilterApplied(min, max, selectedBrandIds);
            }
            dismiss();
        });
        binding.btnResetFilter.setOnClickListener(v -> {
            binding.edtMinPrice.setText("");
            binding.edtMaxPrice.setText("");
            brandAdapter.resetSelection(); // trả về chữ tất cả
        });
        binding.btnCloseFilter.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
