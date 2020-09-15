//
//  AvailableDevice.swift
//  flutter_vision
//
//  Created by Lukas Plachtinas on 2020-09-14.
//

class AvailableDevice {
    let id: String!
    let lensFacing: String!
    let orientation: Int!
    
    init(id: String, lensFacing: String, orientation: Int) {
        self.id = id
        self.lensFacing = lensFacing
        self.orientation = orientation
    }
}
