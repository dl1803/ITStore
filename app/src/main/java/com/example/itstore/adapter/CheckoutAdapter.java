package com.example.itstore.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.itstore.R;
import com.example.itstore.model.CartItem;

import java.util.List;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {
    private List<CartItem> itemList;

    public CheckoutAdapter(List<CartItem> itemList){
        this.itemList = itemList;
    }


    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkout_product, parent, false);
        return new CheckoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutAdapter.CheckoutViewHolder holder, int position) {
        CartItem item = itemList.get(position);
        if (item == null || item.getProduct() == null) return;

        holder.tvName.setText(item.getProduct().getName());
        holder.tvType.setText(item.getVariantName());

        holder.tvCount.setText("x " + item.getQuantity());

        String newPrice = String.format("%,.0f đ", item.getPrice());
        holder.tvSalePrice.setText(newPrice);

        if (item.getProduct().getCompareAtPrice() > item.getPrice()) {
            holder.tvOldPrice.setVisibility(View.VISIBLE);
            holder.tvOldPrice.setText(String.format("%,.0f đ", item.getProduct().getCompareAtPrice()));
            holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvOldPrice.setVisibility(View.GONE);
        }

        String imgUrl = item.getImageUrl();

        if ((imgUrl == null || imgUrl.trim().isEmpty()) && item.getProduct().getImageUrl() != null) {
            imgUrl = item.getProduct().getImageUrl();
        }

        if (imgUrl == null || imgUrl.trim().isEmpty()) {
            holder.imgProduct.setImageResource(R.drawable.ic_search);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_search)
                    .error(R.drawable.ic_search)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .into(holder.imgProduct);
        }
    }

    @Override
    public int getItemCount() {
        return itemList == null ? 0 : itemList.size();
    }

    public static class CheckoutViewHolder extends RecyclerView.ViewHolder{
        ImageView imgProduct;
        TextView tvName, tvType, tvSalePrice, tvOldPrice, tvCount;

        public CheckoutViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgCheckoutProduct);
            tvName = itemView.findViewById(R.id.tvCheckoutProductName);
            tvType = itemView.findViewById(R.id.tvCheckoutProductType);
            tvSalePrice = itemView.findViewById(R.id.tvCheckoutProductSale);
            tvOldPrice = itemView.findViewById(R.id.tvCheckoutProductPrice);
            tvCount = itemView.findViewById(R.id.tvCheckoutProductCount);
        }

    }
}
