import 'package:flutter_vision/flutter_vision.dart';

class FaceDetector {
  bool _hasBeenOpened = false;
  bool _isClosed = false;

  Future<bool> startDetection() async {
    assert(!_isClosed);

    _hasBeenOpened = true;
    return await FlutterVision.channel.invokeMethod<bool>('FaceDetector#start');
  }

  Future<bool> close() async {
    if (!_hasBeenOpened) _isClosed = true;
    if (_isClosed) return Future<bool>.value(true);

    _isClosed = true;
    return await FlutterVision.channel.invokeMethod<bool>('FaceDetector#close');
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
        noseBase = _data['noseBase'] != null ? Position(_data['noseBase']) : null;

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
