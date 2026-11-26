package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentHomeBinding;
import com.google.android.material.card.MaterialCardView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    // ---------- MQTT (matches the ESP32 code) ----------
    private static final String MQTT_SERVER = "tcp://broker.hivemq.com:1883";
    private static final String TOPIC_TEMP = "mystore123/fridge1/temperature";
    private static final String TOPIC_HUM = "mystore123/fridge1/humidity";
    private static final String TOPIC_ALARM = "mystore123/fridge1/alarm";
    private static final String TOPIC_STATUS = "mystore123/fridge1/status";

    private MqttAndroidClient mqttClient;
    private boolean mSubscribed = false;

    // UI update handler (ensures MQTT callbacks run on main thread)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Observe LiveData and update UI
        viewModel.getTemperature().observe(getViewLifecycleOwner(), value ->
                binding.textTemperature.setText(value + " °C"));
        viewModel.getHumidity().observe(getViewLifecycleOwner(), value ->
                binding.textHumidity.setText(value + " %"));
        viewModel.getStatus().observe(getViewLifecycleOwner(), value ->
                binding.textStatus.setText(value));
        viewModel.getStatusColor().observe(getViewLifecycleOwner(), colorRes ->
                binding.textStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), colorRes)));
        viewModel.getConnection().observe(getViewLifecycleOwner(), value ->
                binding.textConnection.setText(value));
        viewModel.getConnectionColor().observe(getViewLifecycleOwner(), colorRes ->
                binding.indicatorConnection.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), colorRes))));
        viewModel.getLastUpdated().observe(getViewLifecycleOwner(), value ->
                binding.textLastUpdated.setText(
                        getString(R.string.last_updated) + ": " + value));

        // Connect to MQTT
        connectMqtt();

        return root;
    }

    private void connectMqtt() {
        String clientId = "android-fridge-" + (int) (Math.random() * 100000);
        mqttClient = new MqttAndroidClient(requireContext(), MQTT_SERVER, clientId);

        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                mainHandler.post(() -> {
                    viewModel.setConnection(getString(R.string.status_online));
                    viewModel.setConnectionColor(R.color.green);
                    mSubscribed = false;
                    subscribeTopics();
                });
            }

            @Override
            public void connectionLost(Throwable cause) {
                mainHandler.post(() -> {
                    viewModel.setConnection(getString(R.string.disconnected));
                    viewModel.setConnectionColor(R.color.red);
                    mSubscribed = false;
                });
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                final String payload = new String(message.getPayload());
                mainHandler.post(() -> onMqttMessage(topic, payload));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // not used
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        try {
            viewModel.setConnection(getString(R.string.connecting));
            viewModel.setConnectionColor(R.color.orange);
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // connectComplete callback handles UI update
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    mainHandler.post(() -> {
                        viewModel.setConnection(getString(R.string.disconnected));
                        viewModel.setConnectionColor(R.color.red);
                    });
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            viewModel.setConnection(getString(R.string.disconnected));
            viewModel.setConnectionColor(R.color.red);
        }
    }

    private void subscribeTopics() {
        if (mqttClient == null || mSubscribed) return;
        String[] topics = {TOPIC_TEMP, TOPIC_HUM, TOPIC_ALARM, TOPIC_STATUS};
        int[] qos = {1, 1, 1, 1};
        try {
            mqttClient.subscribe(topics, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mSubscribed = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void onMqttMessage(String topic, String payload) {
        switch (topic) {
            case TOPIC_TEMP:
                viewModel.setTemperature(payload);
                break;
            case TOPIC_HUM:
                viewModel.setHumidity(payload);
                break;
            case TOPIC_ALARM:
                updateAlarm(payload);
                break;
            case TOPIC_STATUS:
                if ("online".equalsIgnoreCase(payload)) {
                    viewModel.setConnection(getString(R.string.status_online));
                    viewModel.setConnectionColor(R.color.green);
                } else {
                    viewModel.setConnection(getString(R.string.status_offline));
                    viewModel.setConnectionColor(R.color.red);
                }
                break;
        }
        // timestamp
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        viewModel.setLastUpdated(sdf.format(new java.util.Date()));
    }

    private void updateAlarm(String alarm) {
        int colorRes;
        String display;
        switch (alarm) {
            case "OK":
                colorRes = R.color.green;
                display = getString(R.string.status_ok);
                break;
            case "TEMP_HIGH":
                colorRes = R.color.orange;
                display = getString(R.string.temp_high);
                break;
            case "HUMIDITY_HIGH":
                colorRes = R.color.orange;
                display = getString(R.string.hum_high);
                break;
            case "TEMP_AND_HUMIDITY_HIGH":
                colorRes = R.color.red;
                display = getString(R.string.temp_hum_high);
                break;
            default:
                colorRes = R.color.secondary_text;
                display = alarm;
                break;
        }
        viewModel.setStatus(display);
        viewModel.setStatusColor(colorRes);

        // Pulse the status card on alarm
        if (colorRes == R.color.red || colorRes == R.color.orange) {
            MaterialCardView card = binding.cardStatus;
            card.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.card));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        binding = null;
    }
}
