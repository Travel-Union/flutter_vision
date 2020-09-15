//
//  BarcodeDetectorHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import FirebaseMLVision
import os.log

class BarcodeDetectorHandler: ImageHandler {
    let barcodeScanner: VisionBarcodeDetector!
    var name: String!
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        let vision = Vision.vision()
        self.barcodeScanner = vision.barcodeDetector(options: VisionBarcodeDetectorOptions(formats: VisionBarcodeFormat.all))
    }
    
    func onImage(image: VisionImage, callback: @escaping (Dictionary<String, Any>) -> Void) {
        self.processing.value = false
        
        self.barcodeScanner.detect(in: image) { features, error in
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
}
