package com.example.batterynotifier;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import java.util.Objects;

import lombok.Setter;

public class BatteryLevelReceiver extends BroadcastReceiver {

    private final SmsSender smsSender;
    private static boolean isSmsSent = false;
    private @Setter double desiredPercentage = 80.0;

    public BatteryLevelReceiver(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (Objects.requireNonNull(action)) {
            case Intent.ACTION_BATTERY_CHANGED:
                handleBatteryChanged(intent);
                break;
            case Intent.ACTION_POWER_CONNECTED:
                handlePowerConnected();
                break;
        }
    }

    private void handleBatteryChanged(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale * 100;

        if (batteryPct >= desiredPercentage && !isSmsSent) {
            smsSender.sendSMS("Unplug your phone!");
            isSmsSent = true;
        }
    }

    private void handlePowerConnected() {
        isSmsSent = false; // Reset the flag when the phone is plugged in
        Log.i("BatteryLevelReceiver", "Power connected. Resetting..");
    }

}