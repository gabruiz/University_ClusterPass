package com.example.clusterpass.ui.newsession;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NewSessionViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public NewSessionViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is new session fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}