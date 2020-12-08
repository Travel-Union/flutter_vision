import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_vision/barcode_detector.dart';
import 'package:flutter_vision/face_detector.dart';
import 'package:flutter_vision/flutter_vision.dart';
import 'package:flutter_vision/models/available_device.dart';
import 'package:flutter_vision/models/lens_direction.dart';
import 'package:flutter_vision/models/resolution.dart';
import 'package:flutter_vision/text_recognizer.dart';
import 'package:flutter_vision/ui/camera_preview.dart';

void main() {
  runApp(
    MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: MyApp(),
      ),
    ),
  );
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterVision _controller;
  Uint8List image;

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

      if (Platform.isIOS) {
        _controller = FlutterVision(LensDirection.back, iOSResolution: Resolution.fullhd, devices: devices);
        await _controller.initialize();
        addTextRecognizer();
      } else {
        _controller = FlutterVision(LensDirection.back);
      }
    } on PlatformException {
      print("error");
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {});
  }

  void _onPlatformViewCreated(int id) async {
    if (_controller == null) {
      return;
    }
    await _controller.initialize();
    addTextRecognizer();
  }

  void addTextRecognizer() async {
    //await _controller.addTextRecognizer();
    _controller.subscribe().listen((data) {
      if (data != null) {
        print(data);
        if (data is List<Barcode>) {
          data.forEach((b) => print("barcode: ${b.rawValue}"));
        } else if (data is VisionText) {
          print("text: ${data.text}");
        } else if (data is List<Face>) {
          if (data.length == 1) {
            final face = data.first;

            print(face?.boundingBox);
          }
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
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
                CameraPreview(controller: _controller, onAndroidPlatformViewCreated: _onPlatformViewCreated),
                FlatButton(
                  onPressed: () async {
                    final img = await FlutterVision.capture;

                    if (img == null) return;

                    setState(() {
                      image = img;
                    });
                  },
                  child: Text("Capture"),
                ),
                image != null
                    ? GestureDetector(
                        onTap: () {
                          setState(() {
                            image = null;
                          });
                        },
                        child: Image.memory(
                          image,
                          width: MediaQuery.of(context).size.width,
                          height: MediaQuery.of(context).size.height,
                        ),
                      )
                    : Container(),
              ],
            ),
    );
  }
}
