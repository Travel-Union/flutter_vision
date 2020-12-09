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
      switch deviceOrientation {
      case .portrait:
        return cameraPosition == .front ? .leftMirrored : .right
      case .landscapeLeft:
        return cameraPosition == .front ? .downMirrored : .up
      case .portraitUpsideDown:
        return cameraPosition == .front ? .rightMirrored : .left
      case .landscapeRight:
        return cameraPosition == .front ? .upMirrored : .down
      case .unknown:
        return .up
      default:
        return .up
      }
    }
}
