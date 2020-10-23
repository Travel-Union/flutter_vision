//
//  TextRecognitionHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import FirebaseMLVision
import os.log
import AVKit

class TextRecognitionHandler : ImageHandler {
    func onImage(imageBuffer: CMSampleBuffer, deviceOrientation: UIInterfaceOrientation, cameraPosition: AVCaptureDevice.Position, callback: @escaping (Dictionary<String, Any>) -> Void) {
        let orientation = ImageHelper.imageOrientation(
            deviceOrientation: deviceOrientation,
            cameraPosition: cameraPosition
        )
        
        let metadata = VisionImageMetadata()
        metadata.orientation = orientation
        
        let image = VisionImage(buffer: imageBuffer)
        image.metadata = metadata
        
        self.textRecognizer.process(image) { result, error in
            self.processing.value = false
            
            guard error == nil else {
                os_log("Error decoding text %@", error!.localizedDescription)
                return
            }
            
            guard let textResult = result else {
                return
            }
            
            let resultText = textResult.text
            
            var blocks = [[String:Any]]()
            
            for block in textResult.blocks {
                var blockData = [String:Any]()
                
                blockData["text"] = block.text
                blockData["languages"] = block.recognizedLanguages.map { lang in
                    return lang.toString()
                }
                
                blockData["left"] = block.frame.origin.x
                blockData["top"] = block.frame.origin.y
                blockData["width"] = block.frame.size.width
                blockData["height"] = block.frame.size.height
                
                var blockLines = [[String:Any]]()
                
                for line in block.lines {
                    var lineData = [String:Any]()
                    
                    lineData["text"] = line.text
                    lineData["languages"] = line.recognizedLanguages.map { lang in
                        return lang.toString()
                    }
                    lineData["left"] = line.frame.origin.x
                    lineData["top"] = line.frame.origin.y
                    lineData["width"] = line.frame.size.width
                    lineData["height"] = line.frame.size.height
                    
                    var lineElements = [[String:Any]]()
                    
                    for element in line.elements {
                        var elementData = [String:Any]()
                        
                        elementData["text"] = element.text
                        elementData["left"] = element.frame.origin.x
                        elementData["top"] = element.frame.origin.y
                        elementData["width"] = element.frame.size.width
                        elementData["height"] = element.frame.size.height
                        
                        lineElements.append(elementData)
                    }
                    
                    lineData["elements"] = lineElements
                    
                    blockLines.append(lineData)
                }
                
                blockData["lines"] = blockLines
                
                blocks.append(blockData)
            }
            
            if(resultText.count > 0) {
                callback(["eventType": "textRecognition", "data": ["text": resultText, "blocks": blocks]])
            }
        }
    }
    

    
    let textRecognizer: VisionTextRecognizer!
    var name: String!
    var processing: Atomic<Bool>
    
    init(name: String) {
        self.name = name
        self.processing = Atomic<Bool>(false)
        let vision = Vision.vision()
        self.textRecognizer = vision.onDeviceTextRecognizer()
    }
}

extension VisionTextRecognizedLanguage {
    func toString() -> String? {
        self.languageCode;
    }
}
