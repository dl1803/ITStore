package com.example.itstore.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itstore.R;
import com.example.itstore.activity.ReturnRequestDetailActivity;
import com.example.itstore.adapter.ReturnRequestAdapter;
import com.example.itstore.viewmodel.ReturnRequestViewModel;

public class ReturnRequestListFragment extends Fragment {
    private ReturnRequestAdapter adapter;
    private ReturnRequestViewModel viewModel;

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    viewModel.fetchReturnRequests();
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_return_request_list, container, false);

        RecyclerView rvReturnRequests = v.findViewById(R.id.rvReturnRequests);
        rvReturnRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ReturnRequestAdapter();
        rvReturnRequests.setAdapter(adapter);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ReturnRequestViewModel.class);

        viewModel.getReturnRequestList().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.setList(list);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnReturnRequestClickListener(item -> {
            Intent intent = new Intent(requireContext(), ReturnRequestDetailActivity.class);
            intent.putExtra("RETURN_REQUEST_ID", item.getId());
            launcher.launch(intent);
        });

        viewModel.fetchReturnRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.fetchReturnRequests();
        }
    }
}