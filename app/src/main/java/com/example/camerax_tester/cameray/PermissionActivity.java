package com.example.camerax_tester.cameray;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.function.Function;

public abstract class PermissionActivity extends AppCompatActivity {
    Function<Integer, Void> permissionsCallback;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                permissionsCallback.apply(requestCode);
            }
        }
    }
}
