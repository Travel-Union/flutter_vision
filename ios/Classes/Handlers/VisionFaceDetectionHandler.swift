//
//  VisionFaceDetectionHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-21.
//

import Vision
import AVKit

class VisionFaceDetectionHandler : ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        if #available(iOS 12.0, *) {
            let detectFaceRequest = VNDetectFaceLandmarksRequest { (request, error) in
              self.processing.value = false
                
                guard let results = request.results as? [VNFaceObservation],
                      let result = results.first else {
                    return
                }
                
                var faceDataList = [[String:Any]]()

                var data = [String:Any]()
                
                guard self.width != nil, self.height != nil else {
                    return
                }
                
                let transform = CGAffineTransform(scaleX: 1, y: -1)
                  .translatedBy(x: 0,
                                y: -(CGFloat)(self.height!))
                  let scale = CGAffineTransform.identity
                    .scaledBy(x: (CGFloat)(self.width!),
                              y: (CGFloat)(self.height!))
                  let bounds = result.boundingBox
                  .applying(scale).applying(transform)

                data["boundingBox"] = self.formatBoundingBox(frame: bounds)
                
                data["rotY"] = self.rad2deg(result.yaw)
                data["rotZ"] = self.rad2deg(result.roll)
                
                data["width"] = self.width
                data["height"] = self.height
                
                faceDataList.append(data)
                
                if(faceDataList.count > 0) {
                    callback(["eventType": "faceDetection", "data": faceDataList])
                }
            }
            
            let pixelBuffer = CMSampleBufferGetImageBuffer(imageBuffer)!
            self.width = CVPixelBufferGetWidth(pixelBuffer)
            self.height = CVPixelBufferGetHeight(pixelBuffer)
            let vnImage = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
            try? vnImage.perform([detectFaceRequest])
        } else {
            let pixelBuffer = CMSampleBufferGetImageBuffer(imageBuffer)!
            let ciImage = CIImage.init(cvImageBuffer: pixelBuffer, options: [:])

            let options = [CIDetectorAccuracy: CIDetectorAccuracyHigh]
            let faceDetector = CIDetector(ofType: CIDetectorTypeFace, context: nil, options: options)!
            
            let results = faceDetector.features(in: ciImage)
            
            self.processing.value = false
            
            guard let result = results.first as? CIFaceFeature else {
                return
            }
            
            var faceDataList = [[String:Any]]()

            var data = [String:Any]()
            
            data["boundingBox"] = self.formatBoundingBox(frame: result.bounds)
           
            if result.hasFaceAngle {
                data["faceAngle"] = result.faceAngle
            }
            
            faceDataList.append(data)
            
            if(faceDataList.count > 0) {
                callback(["eventType": "faceDetection", "data": faceDataList])
            }
        }
    }
    
    func rad2deg(_ number: NSNumber?) -> Double? {
        if(number == nil) {
            return nil
        }
        
        return number!.doubleValue * 180 / .pi
    }
    
    func rad2deg(_ number: Double) -> Double? {
        return number * 180 / .pi
    }
    
    var name: String!
    var width: Int?
    var height: Int?
    
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
