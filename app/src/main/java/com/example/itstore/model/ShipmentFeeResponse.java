package com.example.itstore.model;

import com.google.gson.annotations.SerializedName;

public class ShipmentFeeResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private FeeData data;

    public boolean isSuccess() { return success; }
    public FeeData getData() { return data; }

    public static class FeeData {
        @SerializedName("shipping_fee")
        private double shippingFee;

        public long getShippingFee() { return (long) shippingFee; }
    }
}
