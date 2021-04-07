import 'package:flutter_vision/models/lens_direction.dart';

class AvailableDevice {
  AvailableDevice({this.id, this.lensDirection, this.sensorOrientation});

  final String? id;
  final LensDirection? lensDirection;

  /// Clockwise angle through which the output image needs to be rotated to be upright on the device screen in its native orientation.
  ///
  /// **Range of valid values:**
  /// 0, 90, 180, 270
  ///
  /// On Android, also defines the direction of rolling shutter readout, which
  /// is from top to bottom in the sensor's coordinate system.
  final int? sensorOrientation;

  @override
  bool operator ==(Object o) {
    return o is AvailableDevice && o.id == id && o.lensDirection == lensDirection;
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