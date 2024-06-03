package com.example.batterynotifier;


import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

import lombok.Getter;
import lombok.Setter;

public class SmsSender {

    private final NotificationSender notificationSender;
    private final SmsManager smsManager;

    @Getter @Setter
    private String phoneNumber;

    public SmsSender(Context context, NotificationSender notificationSender) {
        this.smsManager = context.getSystemService(SmsManager.class);
        this.notificationSender = notificationSender;
    }

    public void sendSMS(String message) {
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Log.i("txtTag", "Sent text message!");
        notificationSender.send("Battery Notifier", "Sent SMS to " + phoneNumber);
    }
}