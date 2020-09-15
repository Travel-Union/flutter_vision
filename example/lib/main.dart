import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_vision/flutter_vision.dart';
import 'package:flutter_vision/barcode_detector.dart';
import 'package:flutter_vision/text_recognizer.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterVision _controller;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    List<AvailableDevice> devices;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      devices = await FlutterVision.availableCameras;
      _controller = FlutterVision(
          devices.firstWhere((c) => c.lensDirection == LensDirection.back),
          Resolution.ultrahd);
      await _controller.initialize();

      await _controller.addTextRecognizer();

      _controller.subscribe().listen((data) {
        if (data != null) {
          if (data is List<Barcode>) {
            data.forEach((b) => print("barcode: ${b.rawValue}"));
          } else if (data is VisionText) {
            print("text: ${data.text}");
          }
        }
      });
    } on PlatformException {
      print("error");
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {});
  }

  Widget _buildImage() {
    return Container(
      constraints: const BoxConstraints.expand(),
      child: _controller == null
          ? const Center(
              child: Text(
                'Initializing Camera...',
                style: TextStyle(
                  color: Colors.green,
                  fontSize: 30.0,
                ),
              ),
            )
          : Stack(
              fit: StackFit.expand,
              children: <Widget>[
                CameraPreview(_controller),
                FlatButton(
                  onPressed: () async {
                    print(await FlutterVision.capturePhoto);
                  },
                  child: Text("Capture"),
                ),
              ],
            ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: _buildImage(),
      ),
    );
  }
}
