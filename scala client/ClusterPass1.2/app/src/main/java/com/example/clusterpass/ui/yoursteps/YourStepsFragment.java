package com.example.clusterpass.ui.yoursteps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.clusterpass.R;

public class YourStepsFragment extends Fragment {

    private YourStepsViewModel yourStepsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        yourStepsViewModel =
                ViewModelProviders.of(this).get(YourStepsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_your_steps, container, false);
        final TextView textView = root.findViewById(R.id.text_gallery);
        yourStepsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}