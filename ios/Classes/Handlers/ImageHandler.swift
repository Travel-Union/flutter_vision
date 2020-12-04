//
//  ImageHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import AVKit

protocol ImageHandler {
    var name: String! { get set }
    var processing: Atomic<Bool> { get set }
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (_:Dictionary<String, Any>) -> Void) -> Void
}

