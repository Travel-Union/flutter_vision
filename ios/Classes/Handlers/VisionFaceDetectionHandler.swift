//
//  VisionFaceDetectionHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-21.
//

import Vision
import AVKit

@available(iOS 11.0, *)
class VisionFaceDetectionHandler : ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        let detectFaceRequest = VNDetectFaceLandmarksRequest { (request, error) in
          self.processing.value = false
            
          if let results = request.results as? [VNFaceObservation] {
            var faceDataList = [[String:Any]]()

            for face in results {
                var data = [String:Any]()
                
                data["boundingBox"] = self.formatBoundingBox(frame: face.boundingBox)
                
                if #available(iOS 12.0, *) {
                    data["rotY"] = face.yaw
                    data["rotZ"] = face.roll
                } else {
                    // Use CIDetector maybe?
                }
                
                guard let landmarks = face.landmarks else {
                    faceDataList.append(data)
                    continue
                }
                
                faceDataList.append(data)
            }
            
            if(faceDataList.count > 0) {
                callback(["eventType": "faceDetection", "data": faceDataList])
            }
          }
        }

        let vnImage = VNImageRequestHandler(cvPixelBuffer: CMSampleBufferGetImageBuffer(imageBuffer)!, options: [:])
        try? vnImage.perform([detectFaceRequest])
    }
    
    var name: String!
    
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
    }
    
    func formatBoundingBox(frame: CGRect) -> [String:Any] {
        var result = [String:Any]()
        result["left"] = frame.origin.x
        result["top"] = frame.origin.y
        result["width"] = frame.width
        result["height"] = frame.height
        return result
    }
}
