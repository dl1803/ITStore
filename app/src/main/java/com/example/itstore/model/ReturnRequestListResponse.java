package com.example.itstore.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReturnRequestListResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<ReturnRequestItem> data;

    public boolean isSuccess() { return success; }
    public List<ReturnRequestItem> getData() { return data; }

    public static class ReturnRequestItem {
        @SerializedName("id")
        private int id;

        @SerializedName("reason")
        private String reason;

        @SerializedName("status")
        private String status;

        @SerializedName("admin_note")
        private String adminNote;

        @SerializedName("refund_amount")
        private double refundAmount;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("return_items")
        private List<ReturnItem> returnItems;

        public int getId() { return id; }
        public String getReason() { return reason; }
        public String getStatus() { return status; }
        public String getAdminNote() { return adminNote; }
        public double getRefundAmount() { return refundAmount; }
        public String getCreatedAt() { return createdAt; }
        public String getCreatedAtFormatted() {
            return com.example.itstore.utils.DateUtils.formatDateDMY(createdAt);
        }
        public List<ReturnItem> getReturnItems() { return returnItems; }

        public static class ReturnItem {
            @SerializedName("id")
            private int id;

            @SerializedName("name")
            private String name;

            @SerializedName("slug")
            private String slug;

            @SerializedName("quantity")
            private int quantity;

            @SerializedName("variant")
            private VariantSummary variant;

            public int getId() { return id; }
            public String getName() { return name; }
            public String getSlug() { return slug; }
            public int getQuantity() { return quantity; }
            public VariantSummary getVariant() { return variant; }
        }

        public static class VariantSummary {
            @SerializedName("id")
            private int id;

            @SerializedName("version")
            private String version;

            @SerializedName("color")
            private String color;

            @SerializedName("color_hex")
            private String colorHex;

            @SerializedName("image_url")
            private String imageUrl;

            public int getId() { return id; }
            public String getVersion() { return version; }
            public String getColor() { return color; }
            public String getColorHex() { return colorHex; }
            public String getImageUrl() { return imageUrl; }
        }
        public String getStatusVN() {
            if (status == null) return "Chờ xử lý";
            switch (status.toLowerCase()) {
                case "pending": return "Chờ xử lý";
                case "approved": return "Đã chấp nhận";
                case "rejected": return "Đã từ chối";
                case "received": return "Đã nhận hàng trả";
                case "completed": return "Hoàn tất";
                default: return "Chờ xử lý";
            }
        }

        public int getStatusColor() {
            if (status == null) return android.graphics.Color.parseColor("#F57C00");
            switch (status.toLowerCase()) {
                case "pending": return android.graphics.Color.parseColor("#F57C00");
                case "approved": return android.graphics.Color.parseColor("#2196F3");
                case "rejected": return android.graphics.Color.parseColor("#FF3B30");
                case "received": return android.graphics.Color.parseColor("#8E24AA");
                case "completed": return android.graphics.Color.parseColor("#4CAF50");
                default: return android.graphics.Color.parseColor("#F57C00");
            }
        }
    }
}