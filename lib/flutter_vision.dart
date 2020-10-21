
import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_vision/barcode_detector.dart';
import 'package:flutter_vision/face_detector.dart';
import 'package:flutter_vision/text_recognizer.dart';

class FlutterVision extends ValueNotifier<CameraValue> {
  static const MethodChannel channel = const MethodChannel('flutter_vision');

  final Resolution resolution;
  final AvailableDevice device;

  TextRecognizer textRecognizer;
  BarcodeDetector barcodeDetector;
  FaceDetector faceDetector;

  int _textureId;
  Completer<void> _completer;
  bool _isDisposed = false;
  StreamSubscription<dynamic> _subscription;

  FlutterVision(this.device, this.resolution)
      : super(const CameraValue.uninitialized());

  Future<void> initialize() async {
    if (_isDisposed) {
      return Future<void>.value();
    }
    try {
      _completer = Completer<void>();

      final Map<String, dynamic> result =
          await channel.invokeMapMethod<String, dynamic>(
        'initialize',
        <String, dynamic>{
          'deviceId': device.id,
          'resolution': resolution.serialize(),
        },
      );

      _textureId = result['textureId'];

      value = value.copyWith(
        isInitialized: true,
        previewSize: Size(
          result['width'].toDouble(),
          result['height'].toDouble(),
        ),
      );
    } on PlatformException catch (e) {
      throw Exception(e.message);
    }

    initializeStream();

    _completer.complete();

    return _completer.future;
  }

  initializeStream() {
    _subscription = EventChannel('flutter_vision/events')
        .receiveBroadcastStream()
        .listen(_onEvent);
  }

  void _onEvent(dynamic event) {
    final Map<dynamic, dynamic> map = event;
    if (_isDisposed) {
      return;
    }

    switch (map['eventType']) {
      case 'error':
        value = value.copyWith(errorDescription: event['errorDescription']);
        break;
      case 'cameraClosing':
        value = value.copyWith(isRecordingVideo: false);
        break;
    }
  }

  static Future<List<AvailableDevice>> get availableCameras async {
    try {
      final List<Map<dynamic, dynamic>> cameras = await channel
          .invokeListMethod<Map<dynamic, dynamic>>('availableCameras');

      return cameras.map((Map<dynamic, dynamic> camera) {
        return AvailableDevice(
          id: camera['id'],
          lensDirection: _parseLensDirection(camera['lensFacing']),
          sensorOrientation: camera['orientation'],
        );
      }).toList();
    } on PlatformException catch (e) {
      print(e.message);
      return null;
    }
  }

  static Future<Uint8List> get capturePhoto async {
    try {
      return await channel.invokeMethod<Uint8List>('retrieveLastFrame');
    } on PlatformException catch (e) {
      print(e.message);
      return null;
    }
  }

  static LensDirection _parseLensDirection(String string) {
    switch (string) {
      case 'front':
        return LensDirection.front;
      case 'back':
        return LensDirection.back;
      case 'external':
        return LensDirection.ext;
      default:
        return LensDirection.unknown;
    }
  }

  @override
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }
    _isDisposed = true;
    super.dispose();
    if (_completer != null) {
      await _completer.future;
      await channel.invokeMethod<bool>('dispose');
      await _subscription?.cancel();
    }
  }

  Future<bool> addTextRecognizer() async {
    if (!value.isInitialized) {
      throw new Exception("MLVision isn't initialized yet.");
    }

    textRecognizer = TextRecognizer();
    return await textRecognizer.startDetection();
  }

  Future<void> removeTextRecognizer() async {
    if (!value.isInitialized) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }
    await textRecognizer.close();
  }

  Future<bool> addBarcodeDetector() async {
    if (!value.isInitialized) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }

    barcodeDetector = BarcodeDetector();
    return await barcodeDetector.startDetection();
  }

  Future<bool> removeBarcodeDetector() async {
    if (!value.isInitialized) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }

    return await barcodeDetector.close();
  }

  Future<bool> addFaceDetector() async {
    if (!value.isInitialized) {
      throw new Exception("MLVision isn't initialized yet.");
    }

    faceDetector = FaceDetector();
    return await faceDetector.startDetection();
  }

  Future<void> removeFaceDetector() async {
    if (!value.isInitialized) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }
    await faceDetector.close();
  }

  Stream<dynamic> subscribe() {
    return EventChannel('flutter_vision/events')
        .receiveBroadcastStream()
        .map((event) {
      try {
        if (event["eventType"] == "barcodeDetection") {
          return Barcode.fromList(event['data']);
        } else if (event["eventType"] == "textRecognition") {
          final data = Map<String, dynamic>.from(event['data']);
          return VisionText(data);
        } else if (event["eventType"] == "faceDetection") {
          return Face.fromList(event['data']);
        }
      } catch (e) {
        print(e);
      }

      return null;
    });
  }
}

enum LensDirection { unknown, front, back, ext }

enum Resolution { potato, vga, hd, fullhd, ultrahd }

extension ResolutionSerializer on Resolution {
  String serialize() {
    switch (this) {
      case Resolution.ultrahd:
        return "ultrahd";
      case Resolution.fullhd:
        return "fullhd";
      case Resolution.hd:
        return "hd";
      case Resolution.vga:
        return "vga";
      case Resolution.potato:
        return "potato";
      default:
        throw Exception("Could not serialize Resolution value");
    }
  }
}

class AvailableDevice {
  AvailableDevice({this.id, this.lensDirection, this.sensorOrientation});

  final String id;
  final LensDirection lensDirection;

  /// Clockwise angle through which the output image needs to be rotated to be upright on the device screen in its native orientation.
  ///
  /// **Range of valid values:**
  /// 0, 90, 180, 270
  ///
  /// On Android, also defines the direction of rolling shutter readout, which
  /// is from top to bottom in the sensor's coordinate system.
  final int sensorOrientation;

  @override
  bool operator ==(Object o) {
    return o is AvailableDevice &&
        o.id == id &&
        o.lensDirection == lensDirection;
  }

  @override
  int get hashCode {
    return id.hashCode;
  }

  @override
  String toString() {
    return '$runtimeType($id, $lensDirection, $sensorOrientation)';
  }
}

class CameraPreview extends StatelessWidget {
  const CameraPreview(this.controller);

  final FlutterVision controller;

  @override
  Widget build(BuildContext context) {
    return controller.value.isInitialized
        ? Texture(textureId: controller._textureId)
        : Container();
  }
}

class CameraValue {
  const CameraValue(
      {this.isInitialized, this.errorDescription, this.previewSize});

  const CameraValue.uninitialized() : this(isInitialized: false);

  /// True after [FirebaseVision.initialize] has completed successfully.
  final bool isInitialized;

  final String errorDescription;

  /// The size of the preview in pixels.
  ///
  /// Is `null` until  [isInitialized] is `true`.
  final Size previewSize;

  /// Convenience getter for `previewSize.height / previewSize.width`.
  ///
  /// Can only be called when [initialize] is done.
  double get aspectRatio => previewSize.height / previewSize.width;

  bool get hasError => errorDescription != null;

  CameraValue copyWith({
    bool isInitialized,
    bool isRecordingVideo,
    bool isTakingPicture,
    bool isStreamingImages,
    String errorDescription,
    Size previewSize,
  }) {
    return CameraValue(
      isInitialized: isInitialized ?? this.isInitialized,
      errorDescription: errorDescription,
      previewSize: previewSize ?? this.previewSize,
    );
  }

  @override
  String toString() {
    return '$runtimeType('
        'isInitialized: $isInitialized, '
        'errorDescription: $errorDescription, '
        'previewSize: $previewSize )';
  }
}
