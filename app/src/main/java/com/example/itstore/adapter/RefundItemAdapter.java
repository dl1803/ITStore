package com.example.itstore.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.databinding.ItemProductCartBinding;
import com.example.itstore.model.OrderItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefundItemAdapter extends RecyclerView.Adapter<RefundItemAdapter.ViewHolder> {
    private List<OrderItem> list;
    private Map<Integer, Integer> quantityMap = new HashMap<>();
    private Map<Integer, Boolean> checkedMap = new HashMap<>();
    private OnRefundAmountChangeListener listener;

    public interface OnRefundAmountChangeListener {
        void onAmountChanged(double totalRefund, int selectedCount);
    }

    public RefundItemAdapter(List<OrderItem> list, OnRefundAmountChangeListener listener) {
        this.list = list;
        this.listener = listener;
        if (list != null) {
            for (OrderItem item : list) {
                quantityMap.put(item.getOrderItemId(), 1);
                checkedMap.put(item.getOrderItemId(), false);
            }
        }
    }

    private void calculateTotal() {
        double total = 0;
        int count = 0;
        for (OrderItem item : list) {
            int key = item.getOrderItemId();
            if (checkedMap.get(key) != null && checkedMap.get(key)) {
                total += item.getPrice() * quantityMap.get(key);
                count++;
            }
        }
        if (listener != null) {
            listener.onAmountChanged(total, count);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductCartBinding binding = ItemProductCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = list.get(position);
        int key = item.getOrderItemId();

        holder.binding.tvProductName.setText(item.getProductName());
        holder.binding.tvVariant.setText(item.getProductType());
        holder.binding.tvPrice.setText(String.format(java.util.Locale.US, "%,.0f đ", item.getPrice()));
        holder.binding.tvQuantity.setText(String.valueOf(quantityMap.get(key)));

        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(com.example.itstore.R.drawable.ic_search)
                .error(com.example.itstore.R.drawable.ic_search)
                .into(holder.binding.imgProduct);

        holder.binding.cbAgreeBuy.setOnCheckedChangeListener(null);
        holder.binding.cbAgreeBuy.setChecked(checkedMap.get(key));

        holder.binding.cbAgreeBuy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkedMap.put(key, isChecked);
            calculateTotal();
        });

        holder.binding.ivPlus.setOnClickListener(v -> {
            int currentQty = quantityMap.get(key);
            if (currentQty < item.getQuantity()) {
                currentQty++;
                quantityMap.put(key, currentQty);
                holder.binding.tvQuantity.setText(String.valueOf(currentQty));
                calculateTotal();
            }
        });

        holder.binding.ivMinus.setOnClickListener(v -> {
            int currentQty = quantityMap.get(key);
            if (currentQty > 1) {
                currentQty--;
                quantityMap.put(key, currentQty);
                holder.binding.tvQuantity.setText(String.valueOf(currentQty));
                calculateTotal();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public Map<Integer, Integer> getSelectedItems() {
        Map<Integer, Integer> result = new HashMap<>();
        for (OrderItem item : list) {
            int key = item.getOrderItemId();
            if (checkedMap.get(key) != null && checkedMap.get(key)) {
                result.put(key, quantityMap.get(key));
            }
        }
        return result;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemProductCartBinding binding;
        public ViewHolder(ItemProductCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}