import 'dart:io';
import 'dart:ui';

import 'package:flutter_vision/constants.dart';
import 'package:flutter_vision/flutter_vision.dart';

class TextRecognizer {
  bool _hasBeenOpened = false;
  bool _isClosed = false;

  Future<bool> startDetection() async {
    assert(!_isClosed);

    _hasBeenOpened = true;
    if (Platform.isIOS) {
      return await FlutterVision.channel.invokeMethod<bool>(MethodNames.addTextRegonizer);
    } else {
      return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.addTextRegonizer);
    }
  }

  Future<bool> close() async {
    if (!_hasBeenOpened) _isClosed = true;
    if (_isClosed) return Future<bool>.value(true);

    _isClosed = true;
    if (Platform.isIOS) {
      return await FlutterVision.channel.invokeMethod<bool>(MethodNames.closeTextRegonizer);
    } else {
      return await FlutterVision.cameraChannel.invokeMethod<bool>(MethodNames.closeTextRegonizer);
    }
  }
}

class VisionText {
  VisionText(Map<String, dynamic> data)
      : text = data['text'],
        blocks = List<TextBlock>.unmodifiable(data['blocks'].map<TextBlock>((dynamic block) => TextBlock(block)));

  final String text;
  final List<TextBlock> blocks;
}

class RecognizedLanguage {
  RecognizedLanguage(dynamic data) : languageCode = data;

  final String languageCode;
}

abstract class TextContainer {
  TextContainer(Map<dynamic, dynamic> data)
      : boundingBox = data['left'] != null
            ? Rect.fromLTWH(
                data['left'],
                data['top'],
                data['width'],
                data['height'],
              )
            : null,
        recognizedLanguages = data['languages'] != null
            ? List<RecognizedLanguage>.unmodifiable(
                data['languages'].map<RecognizedLanguage>(
                  (dynamic language) => RecognizedLanguage(language),
                ),
              )
            : [],
        text = data['text'];

  final Rect boundingBox;
  final List<RecognizedLanguage> recognizedLanguages;
  final String text;
}

class TextBlock extends TextContainer {
  TextBlock(Map<dynamic, dynamic> block)
      : lines = List<TextLine>.unmodifiable(
          block['lines'].map<TextLine>(
            (dynamic line) => TextLine(line),
          ),
        ),
        super(block);

  final List<TextLine> lines;
}

class TextLine extends TextContainer {
  TextLine(Map<dynamic, dynamic> line)
      : elements = List<TextElement>.unmodifiable(
          line['elements'].map<TextElement>(
            (dynamic element) => TextElement(element),
          ),
        ),
        super(line);

  final List<TextElement> elements;
}

class TextElement extends TextContainer {
  TextElement(Map<dynamic, dynamic> element) : super(element);
}
