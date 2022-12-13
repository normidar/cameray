package com.example.camerax_tester;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.example.camerax_tester.cameray.CameraY;
import com.example.camerax_tester.cameray.PermissionActivity;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.camera.core.ImageProxy;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.camerax_tester.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.function.Function;

public class MainActivity extends PermissionActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    CameraY cameraY;

    boolean onOff = true;
    boolean onOff2 = true;
    boolean onOff3 = true;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        cameraY = new CameraY(this);

        binding.fab.setOnClickListener(view -> cameraY.run(() -> {
            if (onOff) {
                cameraY.startVideoCapture();
            } else {
                cameraY.stopVideoCapture();
            }
            onOff = !onOff;
        }));

        binding.fab2.setOnClickListener(view -> cameraY.run(() -> {
            if (onOff2) {
                cameraY.imageListeners = new ArrayList<>();
                cameraY.imageListeners.add(new Function<ImageProxy, Void>() {
                    @Override
                    public Void apply(ImageProxy imageProxy) {
                        Log.d("abc", "camera analyzer running");
                        return null;
                    }
                });
                cameraY.startAnalyzer();
            } else {
                cameraY.stopAnalyzer();
            }
            onOff2 = !onOff2;
        }));

        binding.fab3.setOnClickListener(view -> cameraY.run(() -> {
            cameraY.takePhoto();
        }));

        binding.fab4.setOnClickListener(view -> cameraY.run(() -> {
            if (onOff3) {
                cameraY.bindPreview(binding.previewView);
            } else {
                cameraY.unbindPreview();
            }
            onOff3 = !onOff3;
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}