package com.example.clusterpass.ui.yoursteps;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class YourStepsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public YourStepsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is your steps fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}