package com.travelunion.flutter_vision.detectors;

import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceContourDetectionProcessor extends BaseImageAnalyzer<List<Face>> {
    private final String TAG = "FaceDetectionProcessor";
    private final FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
    private final FaceDetector detector = FaceDetection.getClient(options);
    private final FaceContourDetectionProcessor.Result result;

    private int imageHeight;
    private int imageWidth;

    public FaceContourDetectionProcessor(final FaceContourDetectionProcessor.Result result) {
        this.result = result;
    }

    @Override
    protected Task<List<Face>> detectInImage(@NonNull InputImage image) {
        this.imageHeight = image.getHeight();
        this.imageWidth = image.getWidth();

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
    protected void onSuccessResult(@NonNull List<Face> results) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (Face face : results) {
            Map<String, Object> faceMap = new HashMap<>();

            faceMap.put("rotY", face.getHeadEulerAngleY());
            faceMap.put("rotZ", face.getHeadEulerAngleZ());

            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            if (leftEye != null) {
                faceMap.put("leftEye", getPosition(leftEye.getPosition()));
            }

            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            if (rightEye != null) {
                faceMap.put("rightEye", getPosition(rightEye.getPosition()));
            }

            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
            if (leftEar != null) {
                faceMap.put("leftEar", getPosition(leftEar.getPosition()));
            }

            FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
            if (rightEar != null) {
                faceMap.put("rightEar", getPosition(rightEar.getPosition()));
            }

            FaceLandmark rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK);
            if (rightCheek != null) {
                faceMap.put("rightCheek", getPosition(rightCheek.getPosition()));
            }

            FaceLandmark leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK);
            if (leftCheek != null) {
                faceMap.put("leftCheek", getPosition(leftCheek.getPosition()));
            }

            FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
            if (mouthLeft != null) {
                faceMap.put("mouthLeft", getPosition(mouthLeft.getPosition()));
            }

            FaceLandmark mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
            if (mouthBottom != null) {
                faceMap.put("mouthBottom", getPosition(mouthBottom.getPosition()));
            }

            FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
            if (mouthRight != null) {
                faceMap.put("mouthRight", getPosition(mouthRight.getPosition()));
            }

            FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
            if (noseBase != null) {
                faceMap.put("noseBase", getPosition(noseBase.getPosition()));
            }

            faceMap.put("boundingBox", formatBoundingBox(face.getBoundingBox()));

            faceMap.put("width", this.imageWidth);
            faceMap.put("height", this.imageHeight);

            faceMap.put("smile", face.getSmilingProbability());
            faceMap.put("rightEyeOpen", face.getRightEyeOpenProbability());
            faceMap.put("leftEyeOpen", face.getLeftEyeOpenProbability());
            faceMap.put("trackingId", face.getTrackingId());

            data.add(faceMap);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("eventType", "faceDetection");
        res.put("data", data);

        if(!data.isEmpty() && result != null) {
            result.onFaceResult(res);
        }
    }

    @Override
    protected void onFailureResult(Exception e) {
        Log.w(TAG, "Face Detector failed: " + e);

        if(result != null) {
            result.onFaceError(e);
        }
    }

    /*private Float translateX(Float x) {
        return x * _scaleX;
    }

    private Float translateY(Float y)  {
        return y * _scaleY;
    }*/

    private Map<String, Object> getPosition(PointF point) {
        Map<String, Object> result = new HashMap<>();

        result.put("x", point.x);
        result.put("y", point.y);

        return result;
    }

    private Map<String, Object> formatBoundingBox(Rect boundingBox) {
        Map<String, Object> result = new HashMap<>();

        result.put("left", boundingBox.left);
        result.put("top", boundingBox.top);
        result.put("width", boundingBox.width());
        result.put("height", boundingBox.height());

        return result;
    }

    public interface Result {
        void onFaceResult(Map<String, Object> result);
        void onFaceError(Exception e);
    }
}
