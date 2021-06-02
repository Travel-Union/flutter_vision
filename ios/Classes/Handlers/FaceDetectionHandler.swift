//
//  FaceRecognitionHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-13.
//

import MLKitVision
import MLKitFaceDetection
import os.log
import AVKit

class FaceDetectionHandler : ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        let orientation = ImageHelper.imageOrientation(
            deviceOrientation: deviceOrientation,
            cameraPosition: cameraPosition
        )
        
        let image = VisionImage(buffer: imageBuffer)
        image.orientation = orientation
        
        /*
         case up = 0

         case down = 1

         case left = 2

         case right = 3

         case upMirrored = 4

         case downMirrored = 5

         case leftMirrored = 6

         case rightMirrored = 7
         */
        
        print(image.orientation.rawValue)
        
        self.faceDetector.process(image) { faces, error in
            self.processing.value = false
            
            guard error == nil else {
                os_log("Error decoding face %@", error!.localizedDescription)
                return
            }
            
          guard let faces = faces, !faces.isEmpty else {
            return
          }
            
            var faceDataList = [[String:Any]]()
            os_log("---- !!!! processing face data")
            for face in faces {
                var data = [String:Any]()
                
                if face.hasHeadEulerAngleY {
                    data["rotY"] = face.headEulerAngleY
                }
                
                if face.hasHeadEulerAngleZ {
                    data["rotZ"] = face.headEulerAngleZ
                }
                
                if let leftEye = face.landmark(ofType: .leftEye) {
                    data["leftEye"] = self.getPosition(landmark: leftEye)
                }
                
                if let rightEye = face.landmark(ofType: .rightEye) {
                    data["rightEye"] = self.getPosition(landmark: rightEye)
                }
                
                if let leftEar = face.landmark(ofType: .leftEar) {
                    data["leftEar"] = self.getPosition(landmark: leftEar)
                }
                
                if let rightEar = face.landmark(ofType: .rightEar) {
                    data["rightEar"] = self.getPosition(landmark: rightEar)
                }
                
                if let leftCheek = face.landmark(ofType: .leftCheek) {
                    data["leftCheek"] = self.getPosition(landmark: leftCheek)
                }
                
                if let rightCheek = face.landmark(ofType: .rightCheek) {
                    data["rightCheek"] = self.getPosition(landmark: rightCheek)
                }
                
                if let mouthLeft = face.landmark(ofType: .mouthLeft) {
                    data["mouthLeft"] = self.getPosition(landmark: mouthLeft)
                }
                
                if let mouthBottom = face.landmark(ofType: .mouthBottom) {
                    data["mouthBottom"] = self.getPosition(landmark: mouthBottom)
                }

                if let mouthRight = face.landmark(ofType: .mouthRight) {
                    data["mouthRight"] = self.getPosition(landmark: mouthRight)
                }

                if let noseBase = face.landmark(ofType: .noseBase) {
                    data["noseBase"] = self.getPosition(landmark: noseBase)
                }
                
                if face.hasSmilingProbability {
                    data["smile"] = face.smilingProbability
                }
                
                if face.hasRightEyeOpenProbability {
                    data["rightEyeOpen"] = face.rightEyeOpenProbability
                }
                
                if face.hasLeftEyeOpenProbability {
                    data["leftEyeOpen"] = face.leftEyeOpenProbability
                }
                
                if face.hasTrackingID {
                    data["trackingId"] = face.trackingID
                }
                
                data["boundingBox"] = self.formatBoundingBox(frame: face.frame)
                
                faceDataList.append(data)
            }
            
            if(faceDataList.count > 0) {
                callback(["eventType": "faceDetection", "data": faceDataList])
            }
        }
    }
    
    let faceDetector: FaceDetector!
    var name: String!
    
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        
        let options = FaceDetectorOptions()
        options.performanceMode = .accurate
        //options.landmarkMode = .all
        options.contourMode = .all
        options.classificationMode = .all
        
        self.faceDetector = FaceDetector.faceDetector(options: options)
    }
    
    func getPosition(landmark: FaceLandmark) -> [String:Any] {
        var result = [String:Any]()
        result["x"] = landmark.position.x
        result["y"] = landmark.position.y
        return result
    }
    
    func formatBoundingBox(frame: CGRect) -> [String:Any] {
        var result = [String:Any]()
        result["left"] = frame.origin.x
        result["top"] = frame.origin.y
        result["width"] = frame.size.width
        result["height"] = frame.size.height
        return result
    }
}
