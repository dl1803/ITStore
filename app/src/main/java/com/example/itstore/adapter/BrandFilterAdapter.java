package com.example.itstore.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.model.Brand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrandFilterAdapter extends RecyclerView.Adapter<BrandFilterAdapter.BrandViewHolder> {

    private List<Brand> brandList;
    // Lưu nhiều vị trí khách đang tick
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnBrandFilterListener listener;
    public interface OnBrandFilterListener {
        void onFilterChanged(List<Integer> selectedBrandIds);
    }
    public BrandFilterAdapter(List<Brand> brandList, OnBrandFilterListener listener) {
        this.brandList = brandList;
        this.listener = listener;
        selectedPositions.add(0);
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        Brand brand = brandList.get(position);
        holder.tvBrand.setText(brand.getName());

        // Bật màu nếu hãng này đang được khách chọn
        if (selectedPositions.contains(position)) {
            holder.tvBrand.setBackgroundColor(Color.parseColor("#FFB74D"));
            holder.tvBrand.setTextColor(Color.WHITE);
        } else {
            holder.tvBrand.setBackgroundColor(Color.TRANSPARENT);
            holder.tvBrand.setTextColor(Color.BLACK);
        }

        // Logic bấm vào để bật/tắt
        holder.itemView.setOnClickListener(v -> {
            if (position == 0) {
                selectedPositions.clear();
                selectedPositions.add(0);
            } else {
                selectedPositions.remove(0);
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                if (selectedPositions.isEmpty()) {
                    selectedPositions.add(0);
                }
            }
            notifyDataSetChanged();
            if (listener != null) {
                listener.onFilterChanged(getSelectedBrandIds());
            }
        });
    }

    @Override
    public int getItemCount() {
        return brandList.size();
    }

    public List<Integer> getSelectedBrandIds() {
        List<Integer> result = new ArrayList<>();
        if (selectedPositions.contains(0)) {
            return result; // Chọn "Tất cả" thì trả mảng rỗng
        }
        for (int pos : selectedPositions) {
            result.add(brandList.get(pos).getId());
        }
        return result;
    }

    public void resetSelection() {
        selectedPositions.clear();
        selectedPositions.add(0);
        notifyDataSetChanged();
    }
    public void setSelectedBrandIds(List<Integer> brandIds) {
        selectedPositions.clear();
        if (brandIds == null || brandIds.isEmpty()) {
            selectedPositions.add(0);
        } else {
            for (int i = 0; i < brandList.size(); i++) {
                if (brandIds.contains(brandList.get(i).getId())) {
                    selectedPositions.add(i);
                }
            }
            if (selectedPositions.isEmpty()) {
                selectedPositions.add(0);
            }
        }
        notifyDataSetChanged();
    }
    static class BrandViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrand;
        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrand = itemView.findViewById(android.R.id.text1);
        }
    }
}