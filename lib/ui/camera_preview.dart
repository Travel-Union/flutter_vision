import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_vision/flutter_vision.dart';
import 'package:flutter_vision/ui/camera_preview_android.dart';
import 'package:flutter_vision/ui/camera_preview_ios.dart';

class CameraPreview extends StatefulWidget {
  final FlutterVision? controller;
  final Function(int id)? onAndroidPlatformViewCreated;
  final Widget? loadingContainer;

  const CameraPreview({Key? key, this.controller, this.onAndroidPlatformViewCreated, this.loadingContainer}) : super(key: key);

  @override
  State<StatefulWidget> createState() => _CameraPreviewState();
}

class _CameraPreviewState extends State<CameraPreview> {
  @override
  Widget build(BuildContext context) {
    if(Platform.isIOS) {
      return widget.controller?.value?.isInitialized == true ? CameraPreviewIOS(widget.controller) : widget.loadingContainer!;
    } else {
      return CameraPreviewAndroid(controller: widget.controller, onPlatformViewCreated: widget.onAndroidPlatformViewCreated);
    }
  }
}