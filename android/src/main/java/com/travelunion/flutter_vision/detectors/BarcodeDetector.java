package com.travelunion.flutter_vision.detectors;

import android.media.Image;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.EventChannel;

public class BarcodeDetector implements Detector {
    private final FirebaseVisionBarcodeDetector detector;

    public BarcodeDetector(FirebaseVision vision) {
        detector = vision.getVisionBarcodeDetector();
    }

    @Override
    public void handleDetection(final FirebaseVisionImage image, final EventChannel.EventSink result, final AtomicBoolean throttle) {
        detector
                .detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                                List<Map<String, Object>> barcodes = new ArrayList<>();

                                for (FirebaseVisionBarcode barcode : firebaseVisionBarcodes) {
                                    Map<String, Object> barcodeMap = new HashMap<>();

                                    barcodeMap.put("value", barcode.getRawValue());
                                    barcodeMap.put("displayValue", barcode.getDisplayValue());

                                    barcodes.add(barcodeMap);
                                }
                                Map<String, Object> res = new HashMap<>();
                                res.put("eventType", "barcodeDetection");
                                res.put("data", barcodes);
                                throttle.set(false);
                                result.success(res);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                throttle.set(false);
                                result.error("barcodeDetectorError", exception.getLocalizedMessage(), null);
                            }
                        });
    }

    @Override
    public void handleDetection(Image originalImage, FirebaseVisionImage image, EventChannel.EventSink eventSink, AtomicBoolean throttle) {
        this.handleDetection(image, eventSink, throttle);
    }

    private FirebaseVisionBarcodeDetectorOptions parseOptions(Map<String, Object> optionsData) {
        Integer barcodeFormats = (Integer) optionsData.get("barcodeFormats");
        return new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(barcodeFormats)
                .build();
    }

    @Override
    public void close() throws IOException {
        detector.close();
    }
}

