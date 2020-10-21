package com.travelunion.flutter_vision.detectors;

import android.media.Image;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.EventChannel;

public class FaceDetector implements Detector {
    private final FirebaseVisionFaceDetector detector;

    public FaceDetector(FirebaseVision vision) {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        detector = vision.getVisionFaceDetector(options);
    }

    @Override
    public void handleDetection(final FirebaseVisionImage image, final EventChannel.EventSink result, final AtomicBoolean throttle) {
        detector
                .detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                List<Map<String, Object>> data = new ArrayList<>();

                                for (FirebaseVisionFace face : faces) {
                                    Map<String, Object> faceMap = new HashMap<>();

                                    faceMap.put("rotY", face.getHeadEulerAngleY());
                                    faceMap.put("rotZ", face.getHeadEulerAngleZ());

                                    FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                                    if (leftEye != null) {
                                        faceMap.put("leftEye", getPosition(leftEye.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                                    if (rightEye != null) {
                                        faceMap.put("rightEye", getPosition(rightEye.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                                    if (leftEar != null) {
                                        faceMap.put("leftEar", getPosition(leftEar.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                                    if (rightEar != null) {
                                        faceMap.put("rightEar", getPosition(rightEar.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark rightCheek = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK);
                                    if (rightCheek != null) {
                                        faceMap.put("rightCheek", getPosition(rightCheek.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark leftCheek = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK);
                                    if (leftCheek != null) {
                                        faceMap.put("leftCheek", getPosition(leftCheek.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark mouthLeft = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                                    if (mouthLeft != null) {
                                        faceMap.put("mouthLeft", getPosition(mouthLeft.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark mouthBottom = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                                    if (mouthBottom != null) {
                                        faceMap.put("mouthBottom", getPosition(mouthBottom.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark mouthRight = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
                                    if (mouthRight != null) {
                                        faceMap.put("mouthRight", getPosition(mouthRight.getPosition()));
                                    }

                                    FirebaseVisionFaceLandmark noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                                    if (noseBase != null) {
                                        faceMap.put("noseBase", getPosition(noseBase.getPosition()));
                                    }

                                    faceMap.put("smile", face.getSmilingProbability());
                                    faceMap.put("rightEyeOpen", face.getRightEyeOpenProbability());
                                    faceMap.put("leftEyeOpen", face.getLeftEyeOpenProbability());
                                    faceMap.put("trackingId", face.getTrackingId());

                                    data.add(faceMap);
                                }

                                Map<String, Object> res = new HashMap<>();
                                res.put("eventType", "faceDetection");
                                res.put("data", data);

                                throttle.set(false);

                                if(!data.isEmpty()) {
                                    result.success(res);
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                throttle.set(false);
                                result.error("faceDetectionError", exception.getLocalizedMessage(), null);
                            }
                        });
    }

    @Override
    public void handleDetection(Image originalImage, FirebaseVisionImage image, EventChannel.EventSink eventSink, AtomicBoolean throttle) {
        this.handleDetection(image, eventSink, throttle);
    }

    @Override
    public void close() throws IOException {
        detector.close();
    }

    private Map<String, Object> getPosition(FirebaseVisionPoint position) {
        Map<String, Object> result = new HashMap<>();

        result.put("x", position.getX());
        result.put("y", position.getY());
        result.put("z", position.getZ());

        return result;
    }
}

