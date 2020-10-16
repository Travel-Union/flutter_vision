//
//  FaceRecognitionHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-10-13.
//

import FirebaseMLVision
import FirebaseMLCommon
import os.log

class FaceDetectionHandler : ImageHandler {
    let faceDetector: VisionFaceDetector!
    var name: String!
    
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        let vision = Vision.vision()
        
        let options = VisionFaceDetectorOptions()
        
        self.faceDetector = vision.faceDetector(options: options)
    }
    
    func onImage(image: VisionImage, callback: @escaping (Dictionary<String, Any>) -> Void) {
        self.processing.value = false
        
        self.faceDetector.process(image) { faces, error in
            
            guard error == nil else {
                os_log("Error decoding text %@", error!.localizedDescription)
                return
            }
            
          guard let faces = faces, !faces.isEmpty else {
            return
          }
            
            let faceDataList = [[String:Any]]()

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
                    data["leftEyeOpen"] = face.hasLeftEyeOpenProbability
                }
                
                if face.hasTrackingID {
                    data["trackingId"] = face.trackingID
                }
            }
            
            if(faceDataList.count > 0) {
                callback(["eventType": "faceDetection", "data": faceDataList])
            }
        }
    }
    
    func getPosition(landmark: VisionFaceLandmark) -> [String:Any] {
        var result = [String:Any]()
        result["x"] = landmark.position.x
        result["y"] = landmark.position.y
        result["z"] = landmark.position.z
        return result
    }
}
