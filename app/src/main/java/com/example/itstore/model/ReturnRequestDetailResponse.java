package com.example.itstore.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReturnRequestDetailResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private DetailData data;

    public boolean isSuccess() { return success; }
    public DetailData getData() { return data; }

    public static class DetailData {
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

        @SerializedName("images")
        private List<ReturnImage> images;

        @SerializedName("address")
        private ReturnAddress address;

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
        public List<ReturnImage> getImages() { return images; }
        public ReturnAddress getAddress() { return address; }

        public static class ReturnItem {
            @SerializedName("id")
            private int id;

            @SerializedName("product_id")
            private int productId;

            @SerializedName("name")
            private String name;

            @SerializedName("slug")
            private String slug;

            @SerializedName("quantity")
            private int quantity;

            @SerializedName("condition")
            private String condition;

            @SerializedName("unit_price")
            private double unitPrice;

            @SerializedName("variant")
            private VariantSummary variant;

            public int getId() { return id; }
            public int getProductId() { return productId; }
            public String getName() { return name; }
            public String getSlug() { return slug; }
            public int getQuantity() { return quantity; }
            public String getCondition() { return condition; }
            public double getUnitPrice() { return unitPrice; }
            public VariantSummary getVariant() { return variant; }
            public String getConditionVN() {
                if (condition == null) return "";
                switch (condition.toLowerCase()) {
                    case "damaged": return "Hàng bị hỏng/lỗi";
                    case "good": return "Hàng còn tốt";
                    case "wrong_item": return "Sai sản phẩm";
                    default: return condition;
                }
            }
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

        public static class ReturnImage {
            @SerializedName("id")
            private int id;

            @SerializedName("image_url")
            private String imageUrl;

            public int getId() { return id; }
            public String getImageUrl() { return imageUrl; }
        }

        public static class ReturnAddress {
            @SerializedName("id")
            private int id;

            @SerializedName("recipient")
            private String recipient;

            @SerializedName("phone_number")
            private String phoneNumber;

            @SerializedName("province")
            private String province;

            @SerializedName("district")
            private String district;

            @SerializedName("ward")
            private String ward;

            @SerializedName("street")
            private String street;

            public int getId() { return id; }
            public String getRecipient() { return recipient; }
            public String getPhoneNumber() { return phoneNumber; }
            public String getProvince() { return province; }
            public String getDistrict() { return district; }
            public String getWard() { return ward; }
            public String getStreet() { return street; }
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