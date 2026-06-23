package com.example.itstore.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("total")
    private double total;

    @SerializedName("order_status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("items")
    private List<OrderItem> items;

    @SerializedName("address")
    private AddressInfo addressInfo;

    @SerializedName("timeline")
    private List<TimelineItem> timelineList;

    @SerializedName("shipping_fee")
    private double shippingFee;

    @SerializedName("discount_amount")
    private double discount;

    public static class AddressInfo implements Serializable {
        @SerializedName("recipient")
        public String recipient;

        @SerializedName("phone_number")
        public String phoneNumber;

        @SerializedName("street")
        public String street;

        @SerializedName("ward")
        public String ward;

        @SerializedName("district")
        public String district;

        @SerializedName("province")
        public String province;
    }
    public static class TimelineItem implements Serializable {
        @SerializedName("new_status")
        public String newStatus;

        @SerializedName("note")
        public String note;

        @SerializedName("changed_at")
        public String changedAt;
    }

    public String getOrderId() { return String.valueOf(id); }

    public String getStatus() { return status; }

    public long getTotalPrice() { return (long) total; }

    public String getOrderDate() { return createdAt; }

    // Auto lấy tên của sản phẩm đầu trong đơn hàng
    public String getProductName() {
        if (items != null && !items.isEmpty()) {
            return items.get(0).getProductName();
        }
        return "Đơn hàng rỗng";
    }

    // Auto lấy loại sản phẩm đầu trong đơn hàng
    public String getProductType() {
        if (items != null && !items.isEmpty()) {
            return items.get(0).getProductType();
        }
        return "";
    }

    // Auto tính số lượng món phụ (Tổng số món - 1)
    public int getExtraItemsCount() {
        if (items != null && items.size() > 1) {
            return items.size() - 1;
        }
        return 0;
    }

    // Auto đếm tổng số lượng sản phẩm trong đơn hàng
    public int getQuantity() {
        int totalQty = 0;
        if (items != null) {
            for (OrderItem item : items) {
                totalQty += item.getQuantity();
            }
        }
        return totalQty;
    }

    // Auto lấy ảnh của sản phẩm đầu trong đơn hàng
    public String getImageUrl() {
        if (items != null && !items.isEmpty()) {
            return items.get(0).getImageUrl();
        }
        return "";
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setStatus(String newStatus) {
     this.status = newStatus;
    }

    // Lấy Tên người nhận
    public String getCustomerName() {
        return addressInfo != null ? addressInfo.recipient : null;
    }

    // Lấy Số điện thoại
    public String getPhoneNumber() {
        return addressInfo != null ? addressInfo.phoneNumber : null;
    }

    // Ghép nối Địa chỉ đầy đủ
    public String getShippingAddress() {
        if (addressInfo != null) {
            return addressInfo.street + ", " + addressInfo.ward + ", " +
                    addressInfo.district + ", " + addressInfo.province;
        }
        return null;
    }

    public List<TimelineItem> getTimelineList() {
        return timelineList;
    }
    public long getShippingFee() {
        return (long) shippingFee;
    }

    public long getDiscount() {
        return (long) discount;
    }

    public String getStatusVN() {
        if (status == null) return "Chờ xác nhận";
        switch (status.toLowerCase()) {
            case "pending": return "Chờ xác nhận";
            case "confirmed":
            case "preparing":
            case "shipping": return "Đang giao";
            case "delivered": return "Đã giao";
            case "completed":
            case "received": return "Đã mua";
            case "returning":
            case "returned":
            case "refunding":
            case "refunded": return "Trả hàng";
            case "cancelled": return "Đã hủy";
            default: return "Chờ xác nhận";
        }
    }
}