//
//  MLCamera.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import Foundation
import AVFoundation
import os.log
import Vision
import Accelerate
import CoreImage

class MLCamera : NSObject {
    var captureDevice: AVCaptureDevice!
    var captureSession: AVCaptureSession!
    var previewSize: CMVideoDimensions!
    var capturePosition: AVCaptureDevice.Position!
    var deviceOrientation: UIInterfaceOrientation!
    let deviceId: String!
    var handlers: [ImageHandler]!
    var eventSink: FlutterEventSink?
    
    let textureRegistry: FlutterTextureRegistry
    var pixelBuffer : CVPixelBuffer?
    var textureId: Int64!
    
    init(resolution: String, textureRegistry: FlutterTextureRegistry, deviceId: String) {
        self.textureRegistry = textureRegistry
        self.deviceId = deviceId
        self.handlers = []
        
        super.init()
        
        self.captureSession = AVCaptureSession()
        
        self.captureDevice = AVCaptureDevice.init(uniqueID: deviceId)
        
        if self.captureDevice == nil {
            captureDevice = AVCaptureDevice.default(for: AVMediaType.video)!
        }
        
        self.capturePosition = self.captureDevice.position
        
        self.setResolution(resolution: resolution)
        
        let input = try! AVCaptureDeviceInput.init(device: captureDevice)
        
        let output = AVCaptureVideoDataOutput()
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        
        let queue = DispatchQueue.global(qos: DispatchQoS.QoSClass.default)
        output.setSampleBufferDelegate(self, queue: queue)
        
        captureSession.addInput(input)
        captureSession.addOutput(output)
        
        self.deviceOrientation = UIApplication.shared.statusBarOrientation
        captureSession?.outputs.forEach {
                $0.connections.forEach {
                    $0.videoOrientation = self.mapOrientation(orientation: self.deviceOrientation)
                    $0.isVideoMirrored = self.capturePosition == AVCaptureDevice.Position.front
                }
            }
    }
    
    func start() {
        captureSession?.startRunning()
        self.textureId = self.textureRegistry.register(self)
    }
    
    func stop() {
        handlers = []
        captureSession?.stopRunning()
        pixelBuffer = nil
        textureRegistry.unregisterTexture(textureId)
        textureId = nil
    }
    
    func setResolution(resolution: String) {
        var availablePresets = ["ultrahd", "fullhd", "hd", "vga", "potato"]
        
        let bestResolution = self.getBestAvailableResolution(requirement: resolution, available: &availablePresets)
        
        captureSession.sessionPreset = bestResolution
        self.setPreviewSize(resolution: bestResolution)
    }
    
    func setPreviewSize(resolution: AVCaptureSession.Preset) {
        switch resolution {
        case .hd4K3840x2160:
            previewSize = CMVideoDimensions(width: 3840, height: 2160)
            break
        case .hd1920x1080:
            previewSize = CMVideoDimensions(width: 1920, height: 1080)
            break
        case .hd1280x720:
            previewSize = CMVideoDimensions(width: 1280, height: 720)
            break
        case .vga640x480:
            previewSize = CMVideoDimensions(width: 640, height: 480)
            break
        case .cif352x288:
            previewSize = CMVideoDimensions(width: 352, height: 288)
            break
        default:
            break
        }
    }
    
    func getBestAvailableResolution(requirement: String, available: inout [String]) -> AVCaptureSession.Preset {
        switch requirement {
        case "ultrahd":
            if(captureSession.canSetSessionPreset(.hd4K3840x2160) && self.captureDevice.supportsSessionPreset(AVCaptureSession.Preset.hd4K3840x2160))
            {
                return .hd4K3840x2160
            }
            break
        case "fullhd":
            if(captureSession.canSetSessionPreset(.hd1920x1080) && self.captureDevice.supportsSessionPreset(AVCaptureSession.Preset.hd1920x1080))
            {
                return .hd1920x1080
            }
            break
        case "hd":
            if(captureSession.canSetSessionPreset(.hd1280x720) && self.captureDevice.supportsSessionPreset(AVCaptureSession.Preset.hd1280x720))
            {
                return .hd1280x720
            }
            break
        case "vga":
            if(captureSession.canSetSessionPreset(.vga640x480) && self.captureDevice.supportsSessionPreset(AVCaptureSession.Preset.vga640x480))
            {
                return .vga640x480
            }
        case "potato":
            if(captureSession.canSetSessionPreset(.cif352x288) && self.captureDevice.supportsSessionPreset(AVCaptureSession.Preset.cif352x288))
            {
                return .cif352x288
            }
        default:
            return .low
        }
        
        let nextBest: String = available.removeFirst()
        
        return self.getBestAvailableResolution(requirement: nextBest, available: &available)
    }
}

extension MLCamera : FlutterTexture {
    func copyPixelBuffer() -> Unmanaged<CVPixelBuffer>? {
        if(pixelBuffer == nil){
            return nil
        }
        
        return .passRetained(pixelBuffer!)
    }
}

extension MLCamera: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        DispatchQueue.main.async {
            self.setDeviceOrientation()
        }
        
        connection.videoOrientation = self.mapOrientation(orientation: deviceOrientation)
        connection.isVideoMirrored = self.capturePosition == AVCaptureDevice.Position.front
        
        pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)!
        textureRegistry.textureFrameAvailable(self.textureId)
        
        guard self.handlers.count > 0 && self.eventSink != nil else {
            return
        }
        
        for handler in self.handlers! {
            guard !handler.processing.swap(true) else {
                return
            }
            
            DispatchQueue.global(qos: DispatchQoS.QoSClass.utility).async {
                handler.onImage(imageBuffer: sampleBuffer, deviceOrientation: self.deviceOrientation, cameraPosition: self.capturePosition) { (result) -> () in
                    self.eventSink?(result)
                }
            }
        }
    }
    
    func setDeviceOrientation(){
        self.deviceOrientation = UIApplication.shared.statusBarOrientation
    }
    
    func mapOrientation(orientation: UIInterfaceOrientation) -> AVCaptureVideoOrientation {
        switch orientation {
        case .portrait:
            return .portrait
        case .landscapeLeft:
            return .landscapeLeft
        case .portraitUpsideDown:
            return .portraitUpsideDown
        case .landscapeRight:
            return .landscapeRight
        case .unknown:
            return .portrait
        default:
            return .portrait
        }
    }
}
