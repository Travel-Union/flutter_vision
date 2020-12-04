package com.travelunion.flutter_vision;

import android.content.Context;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class CameraViewFactory extends PlatformViewFactory {

    private final BinaryMessenger messenger;
    FlutterPlugin.FlutterPluginBinding flutterPluginBinding;
    FlutterVisionPlugin plugin;

    public CameraViewFactory(BinaryMessenger messenger, FlutterPlugin.FlutterPluginBinding flutterPluginBinding, FlutterVisionPlugin plugin) {
        super(StandardMessageCodec.INSTANCE);
        this.plugin = plugin;
        this.flutterPluginBinding = flutterPluginBinding;
        this.messenger = messenger;
    }

    @Override
    public PlatformView create(Context context, int id, Object o) {
        return new CameraView(context,messenger, id, flutterPluginBinding,plugin);
    }

}
