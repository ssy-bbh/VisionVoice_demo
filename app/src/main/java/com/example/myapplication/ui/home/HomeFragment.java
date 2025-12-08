package com.example.myapplication.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.ui.ar.RealtimeActivity;
import com.example.myapplication.ui.photo.PhotoRecognitionActivity;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View cardRealtime = view.findViewById(R.id.cardRealtimeScan);
        View cardPhoto = view.findViewById(R.id.cardPhotoUpload);

        // 核心跳转：去 RealtimeActivity
        cardRealtime.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RealtimeActivity.class);
            startActivity(intent);
        });

        // Updated: Start PhotoRecognitionActivity
        cardPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PhotoRecognitionActivity.class);
            startActivity(intent);
        });

        return view;
    }
}