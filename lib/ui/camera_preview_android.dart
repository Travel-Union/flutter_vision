import 'package:flutter/material.dart';
import 'package:flutter_vision/constants.dart';
import 'package:flutter_vision/flutter_vision.dart';

class CameraPreviewAndroid extends StatefulWidget {
  const CameraPreviewAndroid({Key? key, this.controller, this.onPlatformViewCreated}) : super(key: key);

  final FlutterVision? controller;
  final Function(int id)? onPlatformViewCreated;

  @override
  _CameraPreviewAndroidState createState() => _CameraPreviewAndroidState();
}

class _CameraPreviewAndroidState extends State<CameraPreviewAndroid> {
  @override
  Widget build(BuildContext context) {
    return AndroidView(
        viewType: CameraConstants.viewKey,
        onPlatformViewCreated: widget.onPlatformViewCreated,
      );
  }
}
