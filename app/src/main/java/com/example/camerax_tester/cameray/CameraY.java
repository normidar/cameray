package com.example.camerax_tester.cameray;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;


public class CameraY {
    private PermissionActivity activity;
    private ExecutorService cameraExecutorService;
    private Camera camera;

    public CameraY(PermissionActivity activity) {
        this.activity = activity;
        cameraExecutorService = Executors.newSingleThreadExecutor();
        assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public void run(Runnable callback) {
        applyPermission(() -> startCamera(cameraProvider -> {
            bindVideoCapture(cameraProvider);
            bindImageCapture(cameraProvider);
            callback.run();
            return null;
        }));
    }

    @SuppressLint("NewApi")
    private void applyPermission(Runnable nextStep) {
        assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        List<String> lst = new ArrayList<>(Arrays.asList(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            lst.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        for (String perm : lst) {
            boolean isPermied = ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED;
            if (!isPermied) {
                ActivityCompat.requestPermissions(activity, lst.toArray(new String[lst.size()]), 10);
                activity.permissionsCallback = integer -> {
                    nextStep.run();
                    return null;
                };
            }
        }
        nextStep.run();
    }

    ProcessCameraProvider cameraProvider;
    @SuppressLint("NewApi")
    private void startCamera(Function<ProcessCameraProvider, Void> callback) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                callback.apply(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    // Preview
    Preview preview;
    public void bindPreview(PreviewView previewView) {
        assert cameraProvider != null;
        if (preview != null) {
            cameraProvider.unbind(preview);
        }

        preview =new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview);
    }

    public void unbindPreview() {
        assert cameraProvider != null;
        assert preview != null;
        cameraProvider.unbind(preview);
    }

    // Capture

    ImageCapture imageCapture;
    private void bindImageCapture(ProcessCameraProvider cameraProvider) {
        if (imageCapture !=null) {
            cameraProvider.unbind(imageCapture);
        }

        imageCapture = new ImageCapture.Builder().build();
        cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageCapture);
    }

    public void takePhoto() {
        String name = String.valueOf(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                .Builder(activity.getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                .build();
        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(activity),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("abc", "image capture succeeded");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("abc", "image capture error");
                    }
                }
        );
    }

    // Analyzer

    @SuppressLint("NewApi")
    public void startAnalyzer() {
        assert cameraProvider != null;
        if(imageListeners.size() > 0) {
            bindAnalyzer(cameraProvider, imageProxy -> {
                for (Function<ImageProxy, Void> func : imageListeners) {
                    func.apply(imageProxy);
                }
                imageProxy.close();
                return null;
            });
        }
    }

    public void stopAnalyzer() {
        if (imageAnalysis != null) {
            cameraProvider.unbind(imageAnalysis);
        }
    }

    public List<Function<ImageProxy, Void>> imageListeners = new ArrayList<>();

    ImageAnalysis imageAnalysis;

    @SuppressLint("NewApi")
    private void bindAnalyzer(ProcessCameraProvider cameraProvider, Function<ImageProxy, Void> callback) {
        assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        if (imageAnalysis != null) {
            cameraProvider.unbind(imageAnalysis);
        }
        imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(cameraExecutorService, callback::apply);
        camera = cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis);
    }

    // Video Capture

    VideoCapture videoCapture;
    Recorder recorder;
    PendingRecording pendingRecording;

    private void bindVideoCapture(ProcessCameraProvider cameraProvider) {
        if (videoCapture != null) {
            cameraProvider.unbind(videoCapture);
        }

        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        camera = cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                videoCapture);
    }

    // VideoCapture Recording management

    Recording recording;

    public void startVideoCapture() {
        assert recorder != null;
        if (recording != null) {
            recording.stop();
            recording.close();
        }

        String name = String.valueOf(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        }
        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(
                activity.getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();
        pendingRecording = recorder.prepareRecording(activity, mediaStoreOutputOptions);
        recording = pendingRecording.start(ContextCompat.getMainExecutor(activity), videoRecordEvent -> {
        });
    }


    public void pauseVideoCapture() {
        assert recording != null;
        recording.pause();
    }

    public void resumeVideoCapture() {
        assert recording != null;
        recording.resume();
    }

    public void stopVideoCapture() {
        assert recording != null;
        recording.stop();
        recording.close();
    }
}
