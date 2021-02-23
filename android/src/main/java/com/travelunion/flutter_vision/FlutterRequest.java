package com.travelunion.flutter_vision;

import androidx.annotation.Nullable;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

class FlutterRequest {
    boolean hasSubmitted = false;
    MethodCall call;
    MethodChannel.Result submitter;

    FlutterRequest(MethodCall call, MethodChannel.Result submitter) {
        this.call = call;
        this.submitter = submitter;
    }

    public Object getArguments() {
        return call.arguments();
    }

    public String getMethod() {
        return call.method;
    }

    public void submit(Object message) {
        if(!this.hasSubmitted) {
            this.hasSubmitted = true;

            this.submitter.success(message);
        }
    }

    public void reportError(String errorCode, @Nullable String errorMessage, @Nullable Object error) {
        if(!this.hasSubmitted) {
            this.hasSubmitted = true;

            this.submitter.error(errorCode, errorMessage, error);
        }
    }

    public void notImplemented() {
        if(!this.hasSubmitted) {
            this.hasSubmitted = true;

            this.submitter.notImplemented();
        }
    }
}

