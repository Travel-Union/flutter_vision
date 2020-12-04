package com.travelunion.flutter_vision.detectors;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextDetectionProcessor extends BaseImageAnalyzer<Text> {
    private final String TAG = "TextDetectionProcessor";
    private final TextRecognizer recognizer = TextRecognition.getClient();
    private final TextDetectionProcessor.Result result;

    public TextDetectionProcessor(TextDetectionProcessor.Result result) {
        this.result = result;
    }

    @Override
    protected Task<Text> detectInImage(@NonNull InputImage image) {
        return recognizer.process(image);
    }

    @Override
    public void stop() {
        try {
            recognizer.close();
        } catch(Exception e) {
            Log.e(TAG, "Exception thrown while trying to close Text Recognizer: " + e);
        }
    }

    @Override
    protected void onSuccessResult(@NonNull Text results) {
        if(this.result == null) {
            return;
        }

        String text = results.getText();

        if(text == null || text.length() < 1) {
            return;
        }

        Map<String, Object> visionTextData = new HashMap<>();
        visionTextData.put("text", text);

        List<Map<String, Object>> allBlockData = new ArrayList<>();
        for (Text.TextBlock block : results.getTextBlocks()) {
            Map<String, Object> blockData = new HashMap<>();
            addData(
                    blockData,
                    block.getBoundingBox(),
                    block.getCornerPoints(),
                    block.getRecognizedLanguage(),
                    block.getText());

            List<Map<String, Object>> allLineData = new ArrayList<>();
            for (Text.Line line : block.getLines()) {
                Map<String, Object> lineData = new HashMap<>();
                addData(
                        lineData,
                        line.getBoundingBox(),
                        line.getCornerPoints(),
                        line.getRecognizedLanguage(),
                        line.getText());

                List<Map<String, Object>> allElementData = new ArrayList<>();
                for (Text.Element element : line.getElements()) {
                    Map<String, Object> elementData = new HashMap<>();
                    addData(
                            elementData,
                            element.getBoundingBox(),
                            element.getCornerPoints(),
                            element.getRecognizedLanguage(),
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

        this.result.onTextResult(res);
    }

    @Override
    protected void onFailureResult(Exception e) {
        Log.w(TAG, "Text Recognizer failed: " + e);

        if(result != null) {
            this.result.onTextError(e);
        }
    }

    private void addData(
            Map<String, Object> addTo,
            Rect boundingBox,
            Point[] cornerPoints,
            String language,
            String text) {

        if (boundingBox != null) {
            addTo.put("left", (double) boundingBox.left);
            addTo.put("top", (double) boundingBox.top);
            addTo.put("width", (double) boundingBox.width());
            addTo.put("height", (double) boundingBox.height());
        }

        List<double[]> points = new ArrayList<>();
        if (cornerPoints != null) {
            for (Point point : cornerPoints) {
                points.add(new double[] {(double) point.x, (double) point.y});
            }
        }
        addTo.put("points", points);

        addTo.put("language", language);

        addTo.put("text", text);
    }

    public interface Result {
        void onTextResult(Map<String, Object> result);
        void onTextError(Exception e);
    }
}
