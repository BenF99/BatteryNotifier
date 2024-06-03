package com.example.batterynotifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BatteryNotifierActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private static final String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private SmsSender smsSender;

    private TextView vContactNumber;
    private EditText vPercentage;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationSender notificationSender = new NotificationSender(this);
        this.smsSender = new SmsSender(this, notificationSender);

        checkAndRequestPermissions();

        vContactNumber = findViewById(R.id.contact_number);
        Button bContactPick = findViewById(R.id.contact_pick);
        vPercentage = findViewById(R.id.percentage);

        setupLauncher();

        bContactPick.setOnClickListener(v -> pickContact());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            IntStream.range(0, permissions.length).forEach(i -> {
                String message = permissions[i] + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? " Granted" : " Denied");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void setupLauncher() {
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        String phoneNumber = retrieveContactNumber(contactUri);
                        if (phoneNumber != null) {
                            vContactNumber.setText(phoneNumber);
                            registerBatteryLevelReceiver(phoneNumber);
                        } else {
                            Toast.makeText(BatteryNotifierActivity.this, "No phone number found for contact", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @SuppressLint("Range")
    private String retrieveContactNumber(Uri contactUri) {
        String contactNumber = null;
        ContentResolver contentResolver = getContentResolver();
        try (Cursor contentCursor = contentResolver.query(contactUri, null, null, null, null)) {
            if (contentCursor != null && contentCursor.moveToFirst()) {
                String id = contentCursor.getString(contentCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String hasPhone = contentCursor.getString(contentCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                if ("1".equals(hasPhone)) {
                    try (Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null,
                            null
                    )) {
                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                            contactNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }
                    }
                }
            }
        }
        return contactNumber;
    }

    private void registerBatteryLevelReceiver(String phoneNumber) {
        smsSender.setPhoneNumber(phoneNumber);
        BatteryLevelReceiver batteryLevelReceiver = new BatteryLevelReceiver(smsSender);
        batteryLevelReceiver.setDesiredPercentage(Integer.parseInt(vPercentage.getText().toString()));
        registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));

    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = Arrays.stream(permissions)
                .filter(permission -> ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED)
                .collect(Collectors.toList());

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }

}