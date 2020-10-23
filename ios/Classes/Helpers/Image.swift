//
//  Image.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-23.
//

import FirebaseMLVision
import AVKit

class ImageHelper {
    static func imageOrientation(
        deviceOrientation: UIInterfaceOrientation,
        cameraPosition: AVCaptureDevice.Position
        ) -> VisionDetectorImageOrientation {
        return cameraPosition == .front ? .topRight : .topLeft
    }
    
    static func detectorOrientation(in image: UIImage) -> VisionDetectorImageOrientation {
            switch image.imageOrientation {
            case .up:
                return .topLeft
            case .down:
                return .bottomRight
            case .left:
                return .leftBottom
            case .right:
                return .rightTop
            case .upMirrored:
                return .topRight
            case .downMirrored:
                return .bottomLeft
            case .leftMirrored:
                return .leftTop
            case .rightMirrored:
                return .rightBottom
            default:
                return .topLeft;
            }
        }
}
