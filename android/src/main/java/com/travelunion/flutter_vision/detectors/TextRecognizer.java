package com.travelunion.flutter_vision.detectors;

import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.EventChannel;

public class TextRecognizer implements Detector {
    private final FirebaseVisionTextRecognizer recognizer;

    public TextRecognizer(FirebaseVision vision) {
        recognizer = vision.getOnDeviceTextRecognizer();
    }

    @Override
    public void handleDetection(final FirebaseVisionImage image, final EventChannel.EventSink result, final AtomicBoolean throttle) {
        recognizer
                .processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                Map<String, Object> visionTextData = new HashMap<>();
                                visionTextData.put("text", firebaseVisionText.getText());

                                List<Map<String, Object>> allBlockData = new ArrayList<>();
                                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                                    Map<String, Object> blockData = new HashMap<>();
                                    addData(
                                            blockData,
                                            block.getBoundingBox(),
                                            block.getConfidence(),
                                            block.getCornerPoints(),
                                            block.getRecognizedLanguages(),
                                            block.getText());

                                    List<Map<String, Object>> allLineData = new ArrayList<>();
                                    for (FirebaseVisionText.Line line : block.getLines()) {
                                        Map<String, Object> lineData = new HashMap<>();
                                        addData(
                                                lineData,
                                                line.getBoundingBox(),
                                                line.getConfidence(),
                                                line.getCornerPoints(),
                                                line.getRecognizedLanguages(),
                                                line.getText());

                                        List<Map<String, Object>> allElementData = new ArrayList<>();
                                        for (FirebaseVisionText.Element element : line.getElements()) {
                                            Map<String, Object> elementData = new HashMap<>();
                                            addData(
                                                    elementData,
                                                    element.getBoundingBox(),
                                                    element.getConfidence(),
                                                    element.getCornerPoints(),
                                                    element.getRecognizedLanguages(),
                                                    element.getText());

                                            allElementData.add(elementData);
                                        }
                                        lineData.put("elements", allElementData);
                                        allLineData.add(lineData);
                                    }
                                    blockData.put("lines", allLineData);
                                    allBlockData.add(blockData);
                                }

                                visionTextData.put("blocks", allBlockData);
                                Map<String, Object> res = new HashMap<>();
                                res.put("eventType", "textRecognition");
                                res.put("data", visionTextData);
                                throttle.set(false);
                                result.success(res);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                throttle.set(false);
                                result.error("textRecognizerError", exception.getLocalizedMessage(), null);
                            }
                        });
    }

    @Override
    public void handleDetection(Image originalImage, FirebaseVisionImage image, EventChannel.EventSink eventSink, AtomicBoolean throttle) {
        this.handleDetection(image, eventSink, throttle);
    }

    private void addData(
            Map<String, Object> addTo,
            Rect boundingBox,
            Float confidence,
            Point[] cornerPoints,
            List<RecognizedLanguage> languages,
            String text) {

        if (boundingBox != null) {
            addTo.put("left", (double) boundingBox.left);
            addTo.put("top", (double) boundingBox.top);
            addTo.put("width", (double) boundingBox.width());
            addTo.put("height", (double) boundingBox.height());
        }

        addTo.put("confidence", confidence == null ? null : (double) confidence);

        List<double[]> points = new ArrayList<>();
        if (cornerPoints != null) {
            for (Point point : cornerPoints) {
                points.add(new double[] {(double) point.x, (double) point.y});
            }
        }
        addTo.put("points", points);

        List<List<String>> allLanguageData = new ArrayList<>();
        for (RecognizedLanguage language : languages) {
            List<String> languageData = new ArrayList<>();
            languageData.add(language.getLanguageCode());
            allLanguageData.add(languageData);
        }
        addTo.put("languages", allLanguageData);

        addTo.put("text", text);
    }

    @Override
    public void close() throws IOException {
        recognizer.close();
    }
}
