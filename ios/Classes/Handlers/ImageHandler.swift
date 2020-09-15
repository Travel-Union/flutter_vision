//
//  ImageHandler.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

import FirebaseMLVision

protocol ImageHandler {
    var name: String! { get set }
    var processing: Atomic<Bool> { get set }
    func onImage(image: VisionImage, callback: @escaping (_:Dictionary<String, Any>) -> Void) -> Void
}

