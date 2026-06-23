package com.example.itstore.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.R;
import com.example.itstore.model.Address;

import java.util.List;

public class AddressSelectionAdapter extends RecyclerView.Adapter<AddressSelectionAdapter.AddressViewHolder>{
    private List<Address> addressList;
    private OnAddressClickListener listener;


    public interface OnAddressClickListener {
        void onAddressClick(Address address);
    }

    public AddressSelectionAdapter(List<Address> addressList, OnAddressClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    public AddressViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    public void onBindViewHolder(AddressViewHolder holder, int position) {
        Address address = addressList.get(position);
        if (address == null) return;

        holder.tvNameAndPhone.setText(address.getRecipient() + " | " + address.getPhoneNumber());
        String fullAddress = address.getStreet() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getProvince();
        holder.tvAddressDetail.setText(fullAddress);

        holder.tvEdit.setVisibility(View.GONE);
        holder.tvDelete.setVisibility(View.GONE);
        holder.tvDivider.setVisibility(View.GONE);

        if (address.isDefault()) {
            holder.tvDefault.setVisibility(View.VISIBLE);
        } else {
            holder.tvDefault.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAddressClick(address);
        });
    }

    @Override
    public int getItemCount() {
        return addressList == null ? 0 : addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvNameAndPhone, tvAddressDetail, tvDefault, tvEdit, tvDelete, tvDivider;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNameAndPhone = itemView.findViewById(R.id.tvNameAndPhone);
            tvAddressDetail = itemView.findViewById(R.id.tvAddressDetail);
            tvDefault = itemView.findViewById(R.id.tvDefault);
            tvEdit = itemView.findViewById(R.id.tvEdit);
            tvDelete = itemView.findViewById(R.id.tvDelete);
            tvDivider = itemView.findViewById(R.id.tvDivider);
        }
    }
}
