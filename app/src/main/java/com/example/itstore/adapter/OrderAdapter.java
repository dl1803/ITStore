package com.example.itstore.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.itstore.R;
import com.example.itstore.activity.OrderDetailActivity;
import com.example.itstore.activity.ProductDetailActivity;
import com.example.itstore.activity.WriteReviewActivity;
import com.example.itstore.model.Order;
import com.example.itstore.model.Product;

import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orderList;

    public void setOrderList(List<Order> orderList){
        this.orderList = orderList;
        notifyDataSetChanged();
    }

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private OnOrderClickListener listener;

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderAdapter.OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdapter.OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        holder.tvOrderId.setText("Mã ĐH: " + order.getOrderId());

        String statusVN = order.getStatusVN();
        holder.tvOrderStatus.setText(statusVN);
        int statusColor = Color.parseColor("#F57C00");
        switch (statusVN) {
            case "Chờ xác nhận": statusColor = Color.parseColor("#F57C00"); break; // Cam
            case "Đang giao": statusColor = Color.parseColor("#2196F3"); break; // Xanh dương
            case "Đã giao": statusColor = Color.parseColor("#4CAF50"); break; // Xanh lá
            case "Đã mua": statusColor = Color.parseColor("#8E24AA"); break; // Tím
            case "Trả hàng": statusColor = Color.parseColor("#E91E63"); break; // Hồng
            case "Đã hủy": statusColor = Color.parseColor("#FF3B30"); break; // Đỏ
        }
        holder.tvOrderStatus.setTextColor(statusColor);

        holder.tvProductName.setText(order.getProductName());

        if (order.getProductType() == null || order.getProductType().isEmpty()) {
            holder.tvProductType.setVisibility(View.GONE);
        } else {
            holder.tvProductType.setVisibility(View.VISIBLE);
            holder.tvProductType.setText(order.getProductType());
        }

        holder.tvQuantity.setText("x" + order.getQuantity());

        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvTotalPrice.setText("Thành tiền: " + formatter.format(order.getTotalPrice() + 30000) + "đ");
        Glide.with(holder.itemView.getContext())
                .load(order.getImageUrl())
                .placeholder(R.drawable.ic_search)
                .error(R.drawable.ic_search)
                .into(holder.imgProductOrder);

        if (statusVN.equalsIgnoreCase("Đã giao")) {
            // Kiểm tra trạng thái đơn hàng trong bộ nhớ SharedPreferences đã xác nhận hay chưa
            SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("ConfirmedOrders", Context.MODE_PRIVATE);
            boolean isConfirmed = prefs.getBoolean(order.getOrderId(), false);

            if (isConfirmed) {
                holder.btnReviewOrder.setVisibility(View.VISIBLE);
            } else {
                holder.btnReviewOrder.setVisibility(View.GONE);
            }
        } else {
            holder.btnReviewOrder.setVisibility(View.GONE);
        }

        if (order.getExtraItemsCount() > 0) {
            holder.tvTotalItems.setVisibility(View.VISIBLE);
            holder.tvTotalItems.setText("Và " + order.getExtraItemsCount() + " sản phẩm khác");
        } else {
            holder.tvTotalItems.setVisibility(View.GONE);
        }

        holder.btnOrderDetail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });

        if (statusVN.equalsIgnoreCase("Đã mua")) {
            holder.btnRebuyOrder.setVisibility(View.VISIBLE);

            boolean isReviewed = false;
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                isReviewed = order.getItems().get(0).isReviewed();
            }

            if (isReviewed) {
                // Nhánh 1: Đã đánh giá -> Ẩn nút Đánh giá, chỉ còn Mua lại
                holder.btnReviewOrder.setVisibility(View.GONE);
            } else {
                // Nhánh 2: Chưa đánh giá -> Hiện cả 2 nút
                holder.btnReviewOrder.setVisibility(View.VISIBLE);
                holder.btnReviewOrder.setText("Đánh giá");
            }
        }
        else if (statusVN.equalsIgnoreCase("Đã hủy")) {
            // Đã hủy thì khách vẫn có thể muốn Mua lại
            holder.btnRebuyOrder.setVisibility(View.VISIBLE);
            holder.btnReviewOrder.setVisibility(View.GONE);
        }
        else {
            // Các trạng thái Đang giao, Chờ xác nhận... thì ẩn cả 2
            holder.btnReviewOrder.setVisibility(View.GONE);
            holder.btnRebuyOrder.setVisibility(View.GONE);
        }


        holder.btnRebuyOrder.setOnClickListener(v -> {
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                int productId = order.getItems().get(0).getProductId();
                if (productId != -1) {
                    Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
                    // Bốc tạm data đưa sang màn chi tiết
                    Product dummyProduct = new Product(productId, 0, order.getProductName(), "", null, null, 0);
                    dummyProduct.setSlug(String.valueOf(productId));
                    intent.putExtra("PRODUCT_INFO", dummyProduct);
                    v.getContext().startActivity(intent);
                }
            }
        });

        View.OnClickListener openProductDetail = v -> {
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                int productId = order.getItems().get(0).getProductId();

                if (productId != -1) {
                    Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
                    Product dummyProduct = new Product(productId, 0, order.getProductName(), "", null, null, 0);
                    dummyProduct.setSlug(String.valueOf(productId));

                    intent.putExtra("PRODUCT_INFO", dummyProduct);
                    v.getContext().startActivity(intent);
                }
            }
        };
        holder.imgProductOrder.setOnClickListener(openProductDetail);
        holder.tvProductName.setOnClickListener(openProductDetail);
        holder.btnReviewOrder.setOnClickListener(v -> {
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Intent intent = new Intent(v.getContext(), WriteReviewActivity.class);
                int orderItemId = order.getItems().get(0).getOrderItemId();

                intent.putExtra("ORDER_ITEM_ID", orderItemId);
                intent.putExtra("PRODUCT_NAME", order.getProductName());
                intent.putExtra("PRODUCT_IMAGE", order.getImageUrl());
                intent.putExtra("PRODUCT_VARIANT", order.getProductType());
                v.getContext().startActivity(intent);
            } else {
                Intent intent = new Intent(v.getContext(), WriteReviewActivity.class);
                intent.putExtra("PRODUCT_NAME", order.getProductName());
                intent.putExtra("PRODUCT_IMAGE", order.getImageUrl());
                intent.putExtra("PRODUCT_VARIANT", order.getProductType());
                v.getContext().startActivity(intent);
            }
        });

    }


    @Override
    public int getItemCount() {
        return orderList == null ? 0 : orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvProductName, tvProductType;
        TextView tvQuantity, tvTotalItems, tvTotalPrice, btnOrderDetail, btnReviewOrder, btnRebuyOrder;
        ImageView imgProductOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvProductName = itemView.findViewById(R.id.tvProductNameOrder);
            tvProductType = itemView.findViewById(R.id.tvProductTypeOrder);
            tvQuantity = itemView.findViewById(R.id.tvProductQuantity);
            tvTotalItems = itemView.findViewById(R.id.tvTotalItems);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPriceOrder);
            btnOrderDetail = itemView.findViewById(R.id.btnOrderDetail);
            btnReviewOrder = itemView.findViewById(R.id.btnReviewOrder);
            imgProductOrder = itemView.findViewById(R.id.imgProductOrder);
            btnRebuyOrder = itemView.findViewById(R.id.btnRebuyOrder);
        }
    }
}
