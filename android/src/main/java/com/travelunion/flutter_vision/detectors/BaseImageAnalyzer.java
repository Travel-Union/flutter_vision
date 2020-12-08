package com.travelunion.flutter_vision.detectors;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import Catalano.Imaging.Concurrent.Filters.Grayscale;
import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.ContrastCorrection;

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

        byte[] data = null;
        data = NV21toJPEG(
                YUV_420_888toNV21(mediaImage),
                mediaImage.getWidth(), mediaImage.getHeight());
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);

        bitmapImage = bitmapImage.copy(Bitmap.Config.ARGB_8888, true);

        FastBitmap bitmap = new FastBitmap(bitmapImage);
        ContrastCorrection cc = new ContrastCorrection();
        cc.applyInPlace(bitmap);
        Grayscale g = new Grayscale();
        g.applyInPlace(bitmap);

        pendingTask = detectInImage(InputImage.fromBitmap(bitmap.toBitmap(), image.getImageInfo().getRotationDegrees()))
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

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }


    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }

    protected abstract Task<T> detectInImage(@NonNull InputImage image);
    abstract void stop();
    protected abstract void onSuccessResult(@NonNull T results);
    protected abstract void onFailureResult(Exception e);
}
