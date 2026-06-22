package com.example.itstore.utils;

import com.bumptech.glide.load.model.GlideUrl;

public class CustomGlideUrl extends GlideUrl {
    private final String originalUrl;

    public CustomGlideUrl(String url) {
        super(url);
        this.originalUrl = url;
    }

    @Override
    public String getCacheKey() {
        // Cắt bỏ toàn bộ các tham số biến động sau dấu "?" để làm chìa khóa lưu Cache
        if (originalUrl != null && originalUrl.contains("?")) {
            return originalUrl.split("\\?")[0];
        }
        return originalUrl;
    }
}