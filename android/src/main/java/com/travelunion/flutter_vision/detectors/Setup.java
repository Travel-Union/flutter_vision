package com.travelunion.flutter_vision.detectors;

import io.flutter.plugin.common.MethodChannel;

public interface Setup {
    void setup(String modelName, final MethodChannel.Result result);
}