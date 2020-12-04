class CameraConstants {
  static const String methodChannelId = "flutter_vision";
  static const String viewKey = "flutter_vision_preview_view";
}

class MethodNames {
  static const String initialize = "initialize";
  static const String availableCameras = "availableCameras";
  static const String capture = "capture";
  static const String setAspectRatio = "setAspectRatio";
  static const String addFaceDetector = "FaceDetector#start";
  static const String closeFaceDetector = "FaceDetector#close";
  static const String addTextRegonizer = "TextRecognizer#start";
  static const String closeTextRegonizer = "TextRecognizer#close";
  static const String addBarcodeDetector = "BarcodeDetector#start";
  static const String closeBarcodeDetector = "BarcodeDetector#close";
}