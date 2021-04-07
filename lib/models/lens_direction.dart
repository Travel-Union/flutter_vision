enum LensDirection { unknown, front, back, ext }

extension LensDirectionSerializer on LensDirection {
  String serialize() {
    switch (this) {
      case LensDirection.back:
        return "back";
      case LensDirection.front:
        return "front";
      case LensDirection.ext:
        return "external";
      case LensDirection.unknown:
        return "unknown";
      default:
        throw Exception("Could not serialize Resolution value");
    }
  }
}

class LensDirectionHelper {
  static LensDirection parse(String? val) {
    switch (val) {
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
}