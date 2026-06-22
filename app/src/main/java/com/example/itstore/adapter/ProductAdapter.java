package com.example.itstore.adapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.itstore.R;
import com.example.itstore.databinding.ItemProductBinding;
import com.example.itstore.model.Product;
import java.util.List;
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder>{
    private Context context;
    private List<Product> productList;
    private OnProductInteractionListener listener;
    public interface OnProductInteractionListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product, int position);
        void onAddToCartClick(Product product);
    }
    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductInteractionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }
    public void setOnProductInteractionListener(OnProductInteractionListener listener) {
        this.listener = listener;
    }
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) return;
        holder.binding.tvName.setText(product.getName());
        holder.binding.tvPrice.setText(String.format(java.util.Locale.US, "%,.0f VNĐ", product.getPrice()));
        android.util.Log.d("KIEM_TRA_LINK", "Tên SP: " + product.getName() + " | Link ảnh: " + product.getImageUrl());

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_search)
                .into(holder.binding.imgProduct);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
        if (product.isFavorite()) {
            int colorOrange = androidx.core.content.ContextCompat.getColor(context, R.color.orange_primary);
            holder.binding.imgFavoriteItem.setColorFilter(colorOrange);
        } else {
            int colorGray = androidx.core.content.ContextCompat.getColor(context, R.color.white_gray);
            holder.binding.imgFavoriteItem.setColorFilter(colorGray);
        }
        holder.binding.imgFavoriteItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(product, position);
            }
        });

        holder.binding.ivCartItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCartClick(product);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }
    @Override
    public int getItemCount() {
        if (productList != null) {
            return productList.size();
        }
        return 0;
    }
    public class ProductViewHolder extends RecyclerView.ViewHolder {
        ItemProductBinding binding;
        public ProductViewHolder(@NonNull ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }
    public void appendList(List<Product> newItems) {
        int startPos = this.productList.size();
        this.productList.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }
}
