package com.travelunion.flutter_vision;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;

public class Utils {

    static int getFlashModeFromString(String mode){
        switch (mode){
            case "auto":
                return ImageCapture.FLASH_MODE_AUTO;
            case "on":
                return ImageCapture.FLASH_MODE_ON;
            case "off":
                return ImageCapture.FLASH_MODE_OFF;
        }
        return 0;
    }
    static int getLensFacingFromString(String mode){
        switch (mode){
            case "front":
                return CameraSelector.LENS_FACING_FRONT;
            case "back":
                return CameraSelector.LENS_FACING_BACK;
        }
        return 0;
    }
}
