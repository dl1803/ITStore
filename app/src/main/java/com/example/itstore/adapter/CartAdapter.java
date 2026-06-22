package com.example.itstore.adapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.itstore.R;
import com.example.itstore.databinding.ItemProductCartBinding;
import com.example.itstore.model.CartItem;
import com.example.itstore.model.Product;
import com.example.itstore.utils.CustomGlideUrl;

import java.util.List;
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder>{
    private List<CartItem> cartList;
    private CartClickListener listener;
    public interface CartClickListener {
        void onProductClick(Product product);
        void onIncrease(CartItem item, int position);
        void onDecrease(CartItem item, int position);
        void onDelete(CartItem item, int position);
        void onItemSelected(CartItem item, boolean isChecked);
    }

    public CartAdapter(List<CartItem> cartList, CartClickListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    public void setCartList(List<CartItem> newCartList) {
        if (this.cartList == null) {
            this.cartList = cartList;
            notifyItemRangeInserted(0, newCartList.size());
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return cartList.size(); }
            @Override public int getNewListSize() { return newCartList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Kiểm tra cùng 1 sản phẩm và cùng phân loại
                CartItem oldItem = cartList.get(oldPos);
                CartItem newItem = newCartList.get(newPos);
                return oldItem.getProduct().getId() == newItem.getProduct().getId()
                        && oldItem.getVariantName().equals(newItem.getVariantName());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                // Chỉ vẽ lại view nếu số lượng hoặc nút check bị thay đổi
                CartItem oldItem = cartList.get(oldPos);
                CartItem newItem = newCartList.get(newPos);

                String oldImg = oldItem.getImageUrl() != null ? oldItem.getImageUrl().split("\\?")[0] : "";
                String newImg = newItem.getImageUrl() != null ? newItem.getImageUrl().split("\\?")[0] : "";

                return oldItem.getQuantity() == newItem.getQuantity()
                        && oldItem.isSelected() == newItem.isSelected()
                        && oldImg.equals(newImg);
            }
        });

        this.cartList = newCartList;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductCartBinding binding = ItemProductCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);
        if (item == null) return;
        if (item.getProduct() != null) {
            holder.binding.tvProductName.setText(item.getProduct().getName());
        } else {
            holder.binding.tvProductName.setText("Lỗi! Sản phẩm không hợp lệ");
        }
        holder.binding.tvVariant.setText(item.getVariantName());
        holder.binding.tvPrice.setText(String.format(java.util.Locale.US, "%,.0f đ", item.getPrice()));
        holder.binding.tvQuantity.setText(String.valueOf(item.getQuantity()));
        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(new CustomGlideUrl(item.getImageUrl()))
                .placeholder(R.drawable.ic_search)
                .error(R.drawable.ic_search)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .into(holder.binding.imgProduct);
        holder.binding.ivPlus.setOnClickListener(v -> {
            if (listener != null) listener.onIncrease(item, position);
        });
        holder.binding.ivMinus.setOnClickListener(v -> {
            if (listener != null) listener.onDecrease(item, position);
        });
        holder.binding.ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item, position);
        });
        holder.binding.cbAgreeBuy.setOnCheckedChangeListener(null);
        holder.binding.cbAgreeBuy.setChecked(item.isSelected());
        holder.binding.cbAgreeBuy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            if (listener != null) {
                listener.onItemSelected(item, isChecked);
            }
        });
        holder.binding.imgProduct.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(item.getProduct());
            }
        });
        holder.binding.tvProductName.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(item.getProduct());
            }
        });
    }
    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }
    public class CartViewHolder extends RecyclerView.ViewHolder {
        ItemProductCartBinding binding;
        public CartViewHolder(@NonNull ItemProductCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
