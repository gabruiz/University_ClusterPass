package com.example.clusterpass.ui.findpath;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FindPathViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public FindPathViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is find path fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}