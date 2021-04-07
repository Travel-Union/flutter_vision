import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_vision/barcode_detector.dart';
import 'package:flutter_vision/constants.dart';
import 'package:flutter_vision/face_detector.dart';
import 'package:flutter_vision/models/available_device.dart';
import 'package:flutter_vision/models/camera_value.dart';
import 'package:flutter_vision/models/lens_direction.dart';
import 'package:flutter_vision/models/resolution.dart';
import 'package:flutter_vision/text_recognizer.dart';

class FlutterVision extends ValueNotifier<CameraValue> {
  static const MethodChannel channel = const MethodChannel(CameraConstants.methodChannelId);
  static const MethodChannel cameraChannel = const MethodChannel('${CameraConstants.methodChannelId}_0');

  final Resolution iOSResolution;
  final List<AvailableDevice>? devices;
  final LensDirection lensDirection;

  late TextRecognizer textRecognizer;
  late BarcodeDetector barcodeDetector;
  late FaceDetector faceDetector;

  int? textureId;
  Completer<void>? _completer;
  bool _isDisposed = false;
  StreamSubscription<dynamic>? _subscription;

  FlutterVision(this.lensDirection, {this.iOSResolution = Resolution.fullhd, this.devices})
      : super(const CameraValue.uninitialized());

  Future<void> initialize() async {
    if (Platform.isIOS) {
      await _initializeiOS();
    } else {
      await _initializeAndroid();
    }
  }

  static Future<List<AvailableDevice>?> get availableCameras async {
    try {
      final List<Map<dynamic, dynamic>> cameras =
          await (channel.invokeListMethod<Map<dynamic, dynamic>>(MethodNames.availableCameras) as FutureOr<List<Map<dynamic, dynamic>>>);

      return cameras.map((Map<dynamic, dynamic> camera) {
        return AvailableDevice(
          id: camera['id'],
          lensDirection: LensDirectionHelper.parse(camera['lensFacing']),
          sensorOrientation: camera['orientation'],
        );
      }).toList();
    } on PlatformException catch (e) {
      print(e.message);
      return null;
    }
  }

  static Future<Uint8List?> get capture async {
    if (Platform.isIOS) {
      return await _captureIOS();
    } else {
      return await _captureAndroid();
    }
  }

  Future<void> _initializeAndroid() async {
    final Map<String, dynamic> result = await (cameraChannel.invokeMapMethod<String, dynamic>(
      MethodNames.initialize,
      <String, dynamic>{'lensFacing': lensDirection.serialize()},
    ) as FutureOr<Map<String, dynamic>>);

    value = value.copyWith(
        isInitialized: true,
        previewSize: Size(
          result['width'].toDouble(),
          result['height'].toDouble(),
        ),
      );
  }

  Future<void> _initializeiOS() async {
    if (_isDisposed) {
      return Future<void>.value();
    }
    try {
      _completer = Completer<void>();

      final Map<String, dynamic> result = await (channel.invokeMapMethod<String, dynamic>(
        MethodNames.initialize,
        <String, dynamic>{
          'deviceId': devices?.firstWhere((element) => element.lensDirection == lensDirection)?.id,
          'resolution': iOSResolution.serialize(),
        },
      ) as FutureOr<Map<String, dynamic>>);

      textureId = result['textureId'];

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

    _initializeStream();

    _completer!.complete();

    return _completer!.future;
  }

  static Future<Uint8List?> _captureAndroid() async {
    try {
      return await cameraChannel.invokeMethod<Uint8List>(MethodNames.capture);
    } on PlatformException catch (e) {
      print(e.message);
      return null;
    }
  }

  static Future<Uint8List?> _captureIOS() async {
    try {
      return await channel.invokeMethod<Uint8List>(MethodNames.capture);
    } on PlatformException catch (e) {
      print(e.message);
      return null;
    }
  }

  _initializeStream() {
    _subscription = EventChannel('${CameraConstants.methodChannelId}/events').receiveBroadcastStream().listen(_onEvent);
  }

  void _onEvent(dynamic event) {
    final Map<dynamic, dynamic>? map = event;
    if (_isDisposed) {
      return;
    }

    switch (map!['eventType']) {
      case 'error':
        value = value.copyWith(errorDescription: event['errorDescription']);
        break;
      case 'cameraClosing':
        value = value.copyWith(isRecordingVideo: false);
        break;
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
      await _completer!.future;
      if(Platform.isIOS) {
        await channel.invokeMethod<bool>(MethodNames.dispose);
      } else {
        await cameraChannel.invokeMethod<bool>(MethodNames.dispose);
      }
      await _subscription?.cancel();
    }
  }

  Future<bool?> addTextRecognizer() async {
    if (!value.isInitialized!) {
      throw new Exception("MLVision isn't initialized yet.");
    }

    textRecognizer = TextRecognizer();
    return await textRecognizer.startDetection();
  }

  Future<void> removeTextRecognizer() async {
    if (!value.isInitialized!) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }
    await textRecognizer.close();
  }

  Future<bool?> addBarcodeDetector() async {
    if (!value.isInitialized!) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }

    barcodeDetector = BarcodeDetector();
    return await barcodeDetector.startDetection();
  }

  Future<bool?> removeBarcodeDetector() async {
    if (!value.isInitialized!) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }

    return await barcodeDetector.close();
  }

  Future<bool?> addFaceDetector() async {
    if (!value.isInitialized!) {
      throw new Exception("MLVision isn't initialized yet.");
    }

    faceDetector = FaceDetector();
    return await faceDetector.startDetection();
  }

  Future<String?> addFaceDetectorIOS() async {
    if (!value.isInitialized!) {
      throw new Exception("MLVision isn't initialized yet.");
    }

    faceDetector = FaceDetector();
    return await faceDetector.startDetectionIOS();
  }

  Future<void> removeFaceDetector() async {
    if (!value.isInitialized!) {
      throw new Exception("FirebaseVision isn't initialized yet.");
    }
    await faceDetector.close();
  }

  Stream<dynamic> subscribe() {
    return EventChannel('flutter_vision/events').receiveBroadcastStream().map((event) {
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
