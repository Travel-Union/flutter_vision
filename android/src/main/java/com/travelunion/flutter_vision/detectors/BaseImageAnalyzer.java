package com.travelunion.flutter_vision.detectors;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseImageAnalyzer<T> implements ImageAnalysis.Analyzer {
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Task<T> pendingTask = null;

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (pendingTask != null && !pendingTask.isComplete()) {
            Log.d("BaseImageAnalyzer", "Process still running");
            return;
        }

        isProcessing.set(true);

        Image mediaImage = image.getImage();

        pendingTask = detectInImage(InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees()))
            .addOnSuccessListener(new OnSuccessListener<T>() {
                @Override
                public void onSuccess(T t) {
                    onSuccessResult(t);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    onFailureResult(e);
                }
            })
            .addOnCompleteListener(new OnCompleteListener<T>() {
                @Override
                public void onComplete(@NonNull Task<T> task) {
                    isProcessing.set(false);
                    image.close();
                }
            });
    }

    protected abstract Task<T> detectInImage(@NonNull InputImage image);
    abstract void stop();
    protected abstract void onSuccessResult(@NonNull T results);
    protected abstract void onFailureResult(Exception e);
}
