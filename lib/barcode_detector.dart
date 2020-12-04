import 'dart:io';

import 'package:flutter_vision/constants.dart';
import 'package:flutter_vision/flutter_vision.dart';

class BarcodeDetector {
  bool _hasBeenOpened = false;
  bool _isClosed = false;

  Future<bool> startDetection() async {
    assert(!_isClosed);

    _hasBeenOpened = true;

    if (Platform.isIOS) {
      return await FlutterVision.channel.invokeMethod<bool>(MethodNames.addBarcodeDetector);
    } else {
      return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.addBarcodeDetector);
    }
  }

  Future<bool> close() async {
    if (!_hasBeenOpened) _isClosed = true;
    if (_isClosed) return Future<bool>.value(true);

    _isClosed = true;
    if (Platform.isIOS) {
      return await FlutterVision.channel.invokeMethod<bool>(MethodNames.closeBarcodeDetector);
    } else {
      return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.closeBarcodeDetector);
    }
  }
}

class Barcode {
  Barcode(dynamic _data)
      : rawValue = _data['value'],
        displayValue = _data['displayValue'];

  static List<Barcode> fromList(List<dynamic> data) {
    return data.map((m) => Barcode(m)).toList();
  }

  final String rawValue;

  final String displayValue;
}
