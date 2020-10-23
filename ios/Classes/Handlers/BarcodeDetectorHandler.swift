//
//  BarcodeDetectorHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import FirebaseMLVision
import os.log
import AVKit

class BarcodeDetectorHandler: ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        let orientation = ImageHelper.imageOrientation(
            deviceOrientation: deviceOrientation,
            cameraPosition: cameraPosition
        )
        
        let metadata = VisionImageMetadata()
        metadata.orientation = orientation
        
        let image = VisionImage(buffer: imageBuffer)
        image.metadata = metadata
        
        self.barcodeScanner.detect(in: image) { features, error in
            self.processing.value = false
            
            guard error == nil else {
                os_log("Error decoding barcode %@", error!.localizedDescription)
                return
            }
            
            guard let barcodes = features else {
                return
            }
            
            var barcodeList = [Any]()
            
            for barcode in barcodes {
                let displayValue = barcode.displayValue
                let rawValue = barcode.rawValue
                
                barcodeList.append(["value": rawValue ?? "", "displayValue": displayValue ?? ""])
            }
            
            if(barcodeList.count > 0) {
                callback(["eventType": "barcodeDetection", "data": barcodeList])
            }
        }
    }
    
    
    let barcodeScanner: VisionBarcodeDetector!
    var name: String!
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        let vision = Vision.vision()
        self.barcodeScanner = vision.barcodeDetector(options: VisionBarcodeDetectorOptions(formats: VisionBarcodeFormat.all))
    }
}
