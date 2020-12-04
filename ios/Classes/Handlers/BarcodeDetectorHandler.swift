//
//  BarcodeDetectorHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import MLKitVision
import MLKitBarcodeScanning
import os.log
import AVKit

class BarcodeDetectorHandler: ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        let orientation = ImageHelper.imageOrientation(
            deviceOrientation: deviceOrientation,
            cameraPosition: cameraPosition
        )
        
        let image = VisionImage(buffer: imageBuffer)
        image.orientation = orientation
        
        self.barcodeScanner.process(image) { features, error in
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
    
    
    let barcodeScanner: BarcodeScanner!
    var name: String!
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        
        let barcodeOptions = BarcodeScannerOptions(formats: .all)
        self.barcodeScanner = BarcodeScanner.barcodeScanner(options: barcodeOptions)
    }
}
