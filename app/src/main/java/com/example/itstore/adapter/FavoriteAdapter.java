package com.example.itstore.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.itstore.R;
import com.example.itstore.databinding.FragmentFavoriteBinding;
import com.example.itstore.databinding.ItemProductBinding;
import com.example.itstore.model.Product;
import com.example.itstore.utils.CustomGlideUrl;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private List<Product> favoriteList;
    private Context context;
    private OnFavoriteClickListener listener;
    public interface OnFavoriteClickListener {
        void onRemoveFavorite(Product product);
        void onAddToCart(Product product);
        void onProductClick(Product product);
    }
    public FavoriteAdapter(Context context, List<Product> favoriteList, OnFavoriteClickListener listener) {
        this.context = context;
        this.favoriteList = favoriteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(context), parent, false);
        return new FavoriteViewHolder(binding);
    }

    @Override

    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Product product = favoriteList.get(position);
        holder.binding.tvName.setText(product.getName());
        holder.binding.tvPrice.setText(String.format("%,.0f đ", product.getPrice()));
        Glide.with(context)
                .load(new CustomGlideUrl(product.getImageUrl()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .into(holder.binding.imgProduct);
        holder.binding.imgFavoriteItem.setImageResource(R.drawable.ic_favorite);
        int colorOrange = androidx.core.content.ContextCompat.getColor(context, R.color.orange_primary);
        holder.binding.imgFavoriteItem.setColorFilter(colorOrange);
        holder.binding.imgFavoriteItem.setOnClickListener(v -> {
            product.setFavorite(false);
            if (listener != null) {
                listener.onRemoveFavorite(product);
            }
            favoriteList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favoriteList.size());
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
        holder.binding.ivCartItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(product);
            }
        });
    }

    @Override
    public int getItemCount() { return favoriteList.size(); }
    public void updateList(List<Product> newList) {
        if (this.favoriteList == null) {
            this.favoriteList = newList;
            notifyItemRangeInserted(0, newList.size());
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return favoriteList.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return favoriteList.get(oldPos).getId() == newList.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Product oldP = favoriteList.get(oldPos);
                Product newP = newList.get(newPos);

                String oldImg = oldP.getImageUrl() != null ? oldP.getImageUrl().split("\\?")[0] : "";
                String newImg = newP.getImageUrl() != null ? newP.getImageUrl().split("\\?")[0] : "";

                return oldP.getPrice() == newP.getPrice() && oldImg.equals(newImg);
            }
        });

        this.favoriteList = newList;
        diffResult.dispatchUpdatesTo(this);
    }
    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ItemProductBinding binding;
        public FavoriteViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
