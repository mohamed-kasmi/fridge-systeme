package com.example.myapplication.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mTemperature = new MutableLiveData<>("--");
    private final MutableLiveData<String> mHumidity = new MutableLiveData<>("--");
    private final MutableLiveData<String> mStatus = new MutableLiveData<>("-");
    private final MutableLiveData<String> mConnection = new MutableLiveData<>("Disconnected");
    private final MutableLiveData<String> mLastUpdated = new MutableLiveData<>("");
    private final MutableLiveData<Integer> mStatusColor = new MutableLiveData<>(R.color.green);
    private final MutableLiveData<Integer> mConnectionColor = new MutableLiveData<>(R.color.red);

    public LiveData<String> getTemperature() { return mTemperature; }
    public LiveData<String> getHumidity() { return mHumidity; }
    public LiveData<String> getStatus() { return mStatus; }
    public LiveData<String> getConnection() { return mConnection; }
    public LiveData<String> getLastUpdated() { return mLastUpdated; }
    public LiveData<Integer> getStatusColor() { return mStatusColor; }
    public LiveData<Integer> getConnectionColor() { return mConnectionColor; }

    public void setTemperature(String value) { mTemperature.setValue(value); }
    public void setHumidity(String value) { mHumidity.setValue(value); }
    public void setStatus(String value) { mStatus.setValue(value); }
    public void setConnection(String value) { mConnection.setValue(value); }
    public void setLastUpdated(String value) { mLastUpdated.setValue(value); }
    public void setStatusColor(int colorRes) { mStatusColor.setValue(colorRes); }
    public void setConnectionColor(int colorRes) { mConnectionColor.setValue(colorRes); }
}
