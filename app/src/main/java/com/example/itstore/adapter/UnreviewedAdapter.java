package com.example.itstore.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.itstore.R;
import com.example.itstore.activity.WriteReviewActivity;
import com.example.itstore.databinding.ItemUnreviewedProductBinding;
import com.example.itstore.model.UnreviewedResponse.UnreviewedItem;

import java.util.List;

public class UnreviewedAdapter extends RecyclerView.Adapter<UnreviewedAdapter.ViewHolder> {

    private final List<UnreviewedItem> itemList;
    private final Context context;

    public UnreviewedAdapter(Context context, List<UnreviewedItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUnreviewedProductBinding binding = ItemUnreviewedProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnreviewedItem item = itemList.get(position);
        if (item == null) return;
        String orderCode = "Mã ĐH: #DH" + item.getOrderItemId();
        holder.binding.tvOrderId.setText(orderCode);
        holder.binding.tvCountdown.setText("Còn " + item.getRemainingDays() + " ngày");
        holder.binding.tvProductName.setText(item.getProductName());
        holder.binding.tvProductVariant.setText("Phân loại: " + (item.getVersion() != null ? item.getVersion() : "Tiêu chuẩn"));
        Glide.with(context).
                load(item.getProductImage()).
                placeholder(R.drawable.ic_search).
                into(holder.binding.imgProduct);

        holder.binding.btnWriteReview.setOnClickListener(v -> {
            Intent intent = new Intent(context, WriteReviewActivity.class);
            intent.putExtra("ORDER_ITEM_ID", item.getOrderItemId());
            intent.putExtra("PRODUCT_NAME", item.getProductName());
            intent.putExtra("PRODUCT_IMAGE", item.getProductImage());
            intent.putExtra("PRODUCT_VARIANT", item.getVersion());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() { return itemList != null ? itemList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemUnreviewedProductBinding binding;
        public ViewHolder(ItemUnreviewedProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}