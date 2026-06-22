package com.example.itstore.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.itstore.R;
import com.example.itstore.model.Banner;
import com.example.itstore.utils.CustomGlideUrl;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder>{
    private List<Banner> bannerList;

    public BannerAdapter(List<Banner> bannerList) {
        this.bannerList = bannerList;
    }

    public void updateData(List<Banner> newBanners) {
        if (this.bannerList == null) {
            this.bannerList = newBanners;
            notifyItemRangeInserted(0, newBanners.size());
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return bannerList.size(); }
            @Override public int getNewListSize() { return newBanners.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return bannerList.get(oldPos).getId() == newBanners.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                String oldImg = bannerList.get(oldPos).getImageUrl() != null ? bannerList.get(oldPos).getImageUrl().split("\\?")[0] : "";
                String newImg = newBanners.get(newPos).getImageUrl() != null ? newBanners.get(newPos).getImageUrl().split("\\?")[0] : "";

                return oldImg.equals(newImg);
            }
        });

        this.bannerList = newBanners;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = bannerList.get(position);

        Glide.with(holder.itemView.getContext())
                .load(new CustomGlideUrl(banner.getImageUrl()))
                .diskCacheStrategy(DiskCacheStrategy.ALL) // lưu ảnh vào cache trong máy -> giảm thời gian tải ảnh
                // (.ALL là lưu tối đa các ảnh và những thay đổi của ảnh -> dùng ảnh tối ưu -> tối ưu t)
                .dontAnimate() // tắt hiệu ứng animation của ảnh(do dùng thư viện Glide nên đôi khi nó fade-in) khi xuất hiện
                .placeholder(R.color.gray_background)
                .into(holder.imgBannerItem);
    }

    @Override
    public int getItemCount() {
        if (bannerList != null) {
            return bannerList.size();
        }
        return 0;
    }

    public class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBannerItem;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBannerItem = itemView.findViewById(R.id.imgBannerItem);
        }
    }

}
