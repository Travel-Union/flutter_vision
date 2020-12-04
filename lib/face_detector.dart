import 'dart:io';

import 'package:flutter_vision/constants.dart';
import 'package:flutter_vision/flutter_vision.dart';

class FaceDetector {
  bool _hasBeenOpened = false;
  bool _isClosed = false;

  Future<bool> startDetection() async {
    assert(!_isClosed);

    _hasBeenOpened = true;
    return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.addFaceDetector);
  }

  Future<String> startDetectionIOS(double width, double height) async {
    assert(!_isClosed);

    _hasBeenOpened = true;
    return await FlutterVision.channel.invokeMethod<String>(
      MethodNames.addFaceDetector,
      <String, double>{
        'width': width,
        'height': height,
      },
    );
  }

  Future<bool> close() async {
    if (!_hasBeenOpened) _isClosed = true;
    if (_isClosed) return Future<bool>.value(true);

    _isClosed = true;

    if (Platform.isIOS) {
      return await FlutterVision.channel.invokeMethod<bool>(MethodNames.closeFaceDetector);
    } else {
      return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.closeFaceDetector);
    }
  }
}

class Face {
  final double rotY;
  final double rotZ;
  final double smile;
  final double rightEyeOpen;
  final double leftEyeOpen;
  final int trackingId;
  final Position leftEye;
  final Position rightEye;
  final Position leftCheek;
  final Position rightCheek;
  final Position leftEar;
  final Position rightEar;
  final Position mouthLeft;
  final Position mouthBottom;
  final Position mouthRight;
  final Position noseBase;
  final BoundingBox boundingBox;
  final double faceAngle;

  Face(dynamic _data)
      : rotY = _data['rotY'],
        rotZ = _data['rotZ'],
        smile = _data['smile'],
        rightEyeOpen = _data['rightEyeOpen'],
        leftEyeOpen = _data['leftEyeOpen'],
        trackingId = _data['trackingId'],
        leftEye = _data['leftEye'] != null ? Position(_data['leftEye']) : null,
        rightEye = _data['rightEye'] != null ? Position(_data['rightEye']) : null,
        leftCheek = _data['leftCheek'] != null ? Position(_data['leftCheek']) : null,
        rightCheek = _data['rightCheek'] != null ? Position(_data['rightCheek']) : null,
        leftEar = _data['leftEar'] != null ? Position(_data['leftEar']) : null,
        rightEar = _data['rightEar'] != null ? Position(_data['rightEar']) : null,
        mouthLeft = _data['mouthLeft'] != null ? Position(_data['mouthLeft']) : null,
        mouthBottom = _data['mouthBottom'] != null ? Position(_data['mouthBottom']) : null,
        mouthRight = _data['mouthRight'] != null ? Position(_data['mouthRight']) : null,
        noseBase = _data['noseBase'] != null ? Position(_data['noseBase']) : null,
        faceAngle = _data['faceAngle'],
        boundingBox = _data['boundingBox'] != null && _data['boundingBox']['top'] != null
            ? BoundingBox.fromMap(_data['boundingBox'])
            : null;

  static List<Face> fromList(List<dynamic> data) {
    return data.map((m) => Face(m)).toList();
  }
}

class Position {
  final double x;
  final double y;
  final double z;

  Position(dynamic data)
      : x = data["x"],
        y = data["y"],
        z = data["z"];
}

class BoundingBox {
  final num left;
  final num top;
  final num width;
  final num height;

  BoundingBox.fromMap(dynamic data)
      : left = data['left'],
        top = data['top'],
        width = data['width'],
        height = data['height'];

  BoundingBox.fromLTWH(this.left, this.top, this.width, this.height);
}
