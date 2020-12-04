import 'package:flutter/material.dart';
import 'package:flutter_vision/flutter_vision.dart';

class CameraPreviewIOS extends StatelessWidget {
  const CameraPreviewIOS(this.controller);

  final FlutterVision controller;

  @override
  Widget build(BuildContext context) {
    return controller.value.isInitialized
        ? AspectRatio(
            aspectRatio: controller.value.aspectRatio,
            child: Texture(textureId: controller.textureId),
          )
        : Container();
  }
}
