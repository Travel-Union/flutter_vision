package com.travelunion.flutter_vision.detectors;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarcodeDetectionProcessor extends BaseImageAnalyzer<List<Barcode>> {
    private final String TAG = "BarcodeProcessor";
    private final BarcodeScanner detector = BarcodeScanning.getClient();
    private final BarcodeDetectionProcessor.Result result;

    public BarcodeDetectionProcessor(final BarcodeDetectionProcessor.Result result) {
        this.result = result;
    }

    @Override
    protected Task<List<Barcode>> detectInImage(@NonNull InputImage image) {
        return detector.process(image);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch(Exception e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected void onSuccessResult(@NonNull List<Barcode> results) {
        if(this.result == null) {
            return;
        }

        List<Map<String, Object>> barcodes = new ArrayList<>();

        for (Barcode barcode : results) {
            Map<String, Object> barcodeMap = new HashMap<>();

            barcodeMap.put("value", barcode.getRawValue());
            barcodeMap.put("displayValue", barcode.getDisplayValue());

            barcodes.add(barcodeMap);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("eventType", "barcodeDetection");
        res.put("data", barcodes);

        if(barcodes.size() > 0) {
            result.onBarcodeResult(res);
        }
    }

    @Override
    protected void onFailureResult(Exception e) {
        Log.w(TAG, "Barcode Detector failed: " + e);

        if(result != null) {
            result.onBarcodeError(e);
        }
    }

    public interface Result {
        void onBarcodeResult(Map<String, Object> result);
        void onBarcodeError(Exception e);
    }
}
