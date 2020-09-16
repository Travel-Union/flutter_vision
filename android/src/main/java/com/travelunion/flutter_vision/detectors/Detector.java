package com.travelunion.flutter_vision.detectors;

import android.media.Image;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.EventChannel;

public interface Detector {
    void handleDetection(final FirebaseVisionImage image, final EventChannel.EventSink eventSink, AtomicBoolean throttle);

    void handleDetection(final Image originalImage, final FirebaseVisionImage image, final EventChannel.EventSink eventSink, AtomicBoolean throttle);

    void close() throws IOException;
}