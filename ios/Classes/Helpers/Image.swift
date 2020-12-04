//
//  Image.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-23.
//

import AVKit

class ImageHelper {
    static func imageOrientation(
        deviceOrientation: UIInterfaceOrientation,
        cameraPosition: AVCaptureDevice.Position
    ) -> UIImage.Orientation {
        return cameraPosition == .front ? .upMirrored : .up
    }
}
