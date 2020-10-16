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
  final String trackingId;
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
        leftEye = Position(_data['leftEye']),
        rightEye = Position(_data['rightEye']),
        leftCheek = Position(_data['leftCheek']),
        rightCheek = Position(_data['rightCheek']),
        leftEar = Position(_data['leftEar']),
        rightEar = Position(_data['rightEar']),
        mouthLeft = Position(_data['mouthLeft']),
        mouthBottom = Position(_data['mouthBottom']),
        mouthRight = Position(_data['mouthRight']),
        noseBase = Position(_data['noseBase']);

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
