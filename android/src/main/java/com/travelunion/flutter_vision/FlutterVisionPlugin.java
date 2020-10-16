package com.travelunion.flutter_vision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterView;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.travelunion.flutter_vision.detectors.BarcodeDetector;
import com.travelunion.flutter_vision.detectors.Detector;
import com.travelunion.flutter_vision.detectors.FaceDetector;
import com.travelunion.flutter_vision.detectors.TextRecognizer;

import static android.content.Context.CAMERA_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.view.OrientationEventListener.ORIENTATION_UNKNOWN;

/** FlutterVisionPlugin */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FlutterVisionPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  private static final int CAMERA_REQUEST_ID = 327123094;
  private static CameraManager cameraManager;
  private MLCamera camera;

  private Runnable cameraPermissionContinuation;
  private OrientationEventListener orientationEventListener;
  private int currentOrientation = ORIENTATION_UNKNOWN;

  private FlutterPluginBinding pluginBinding;

  private Activity activity;

  private static byte[] image;

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(PluginRegistry.Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_vision");

    FlutterVisionPlugin plugin = new FlutterVisionPlugin();
    plugin.activity = registrar.activity();
    plugin.channel = channel;

    channel.setMethodCallHandler(plugin);
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_vision");
    channel.setMethodCallHandler(this);
    pluginBinding = flutterPluginBinding;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "availableCameras":
        try {
          String[] cameraNames = cameraManager.getCameraIdList();
          List<Map<String, Object>> cameras = new ArrayList<>();
          for (String cameraName : cameraNames) {
            HashMap<String, Object> details = new HashMap<>();
            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(cameraName);
            details.put("id", cameraName);
            @SuppressWarnings("ConstantConditions")
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            details.put("orientation", sensorOrientation);

            int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            switch (lensFacing) {
              case CameraMetadata.LENS_FACING_FRONT:
                details.put("lensFacing", "front");
                break;
              case CameraMetadata.LENS_FACING_BACK:
                details.put("lensFacing", "back");
                break;
              case CameraMetadata.LENS_FACING_EXTERNAL:
                details.put("lensFacing", "external");
                break;
            }
            cameras.add(details);
          }
          result.success(cameras);
        } catch (Exception e) {
          handleException(e, result);
        }
        break;
      case "initialize":
        String cameraName = call.argument("deviceId");
        String resolutionPreset = call.argument("resolution");
        if (camera != null) {
          camera.close();
        }
        camera = new MLCamera(activity, cameraName, resolutionPreset, result);
        orientationEventListener.enable();
        break;
      case "retrieveLastFrame":
        if(FlutterVisionPlugin.image != null) {
          result.success(FlutterVisionPlugin.image);
        } else {
          result.error("CAMERA", "Last frame is null", null);
        }
        break;
      case "BarcodeDetector#start":
      case "TextRecognizer#start":
      case "FaceDetector#start":
        Detector detector = null;

        if (camera == null) {
          result.success(false);
          return;
        }

        if (camera.currentDetector == null) {
          switch (call.method) {
            case "BarcodeDetector#start":
              detector = new BarcodeDetector(FirebaseVision.getInstance());
              break;
            case "TextRecognizer#start":
              detector = new TextRecognizer(FirebaseVision.getInstance());
              break;
            case "FaceDetector#start":
              detector = new FaceDetector(FirebaseVision.getInstance());
              break;
          }
          addDetector(detector, result);
        }
        break;
      case "BarcodeDetector#close":
      case "TextRecognizer#close":
      case "FaceDetector#close":
        if(camera == null) {
          result.success(false);
        } else {
          closeDetector(camera.currentDetector, result);
        }
        break;
      case "dispose":
        if (camera != null) {
          camera.dispose();
        }
        orientationEventListener.disable();
        result.success(true);
        break;
      default:
        result.notImplemented();
    }
  }

  private void handleException(Exception exception, Result result) {
    if (exception instanceof CameraAccessException) {
      result.error("CameraAccess", exception.getMessage(), null);
    }

    throw (RuntimeException) exception;
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == CAMERA_REQUEST_ID) {
      cameraPermissionContinuation.run();
      return true;
    }
    return false;
  }

  private void closeDetector(final Detector detector, final Result result) {
    try {
      detector.close();
      camera.currentDetector = null;
      result.success(true);
    } catch (IOException e) {
      result.success(false);
    }
  }

  private void addDetector(final Detector detector, final Result result) {
    if(detector != null) {
      camera.currentDetector = detector;
      result.success(true);
      return;
    }

    result.success(false);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();

    cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);

    orientationEventListener =
            new OrientationEventListener(activity.getApplicationContext()) {
              @Override
              public void onOrientationChanged(int i) {
                if (i == ORIENTATION_UNKNOWN) {
                  return;
                }
                // Convert the raw deg angle to the nearest multiple of 90.
                currentOrientation = (int) Math.round(i / 90.0) * 90;
              }
            };
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
    cameraManager = null;
    orientationEventListener = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();

    cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);

    orientationEventListener =
            new OrientationEventListener(activity.getApplicationContext()) {
              @Override
              public void onOrientationChanged(int i) {
                if (i == ORIENTATION_UNKNOWN) {
                  return;
                }
                // Convert the raw deg angle to the nearest multiple of 90.
                currentOrientation = (int) Math.round(i / 90.0) * 90;
              }
            };
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
    cameraManager = null;
    orientationEventListener = null;
  }

  private class MLCamera {
    private final SparseIntArray ORIENTATIONS = new SparseIntArray(4);
    {
      ORIENTATIONS.append(Surface.ROTATION_0, 0);
      ORIENTATIONS.append(Surface.ROTATION_90, 90);
      ORIENTATIONS.append(Surface.ROTATION_180, 180);
      ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    private final FlutterView.SurfaceTextureEntry textureEntry;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private EventChannel.EventSink eventSink;
    private ImageReader imageReader;
    private int sensorOrientation;
    private boolean isFrontFacing;
    private String cameraName;
    private Size previewSize;
    private CaptureRequest.Builder captureRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private WindowManager windowManager;
    private Detector currentDetector;
    private Activity activity;

    MLCamera(
            final Activity activity,
            final String cameraName,
            final String resolutionPreset,
            @NonNull final Result result) {

      this.activity = activity;
      this.cameraName = cameraName;
      textureEntry = pluginBinding.getTextureRegistry().createSurfaceTexture();

      registerEventChannel();

      try {
        Pair resolution;
        switch (resolutionPreset) {
          case "fullhd":
          case "ultrahd":
            resolution = new Pair(1920,1080);
            break;
          case "hd":
            resolution = new Pair(1280,720);
            break;
          case "sd":
            resolution = new Pair(640,480);
            break;
          case "potato":
            resolution = new Pair(352,240);
            break;
          default:
            throw new IllegalArgumentException("Unknown preset: " + resolutionPreset);
        }

        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraName);
        StreamConfigurationMap streamConfigurationMap =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //noinspection ConstantConditions
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        //noinspection ConstantConditions
        isFrontFacing =
                characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraMetadata.LENS_FACING_FRONT;
        computeBestPreviewAndRecordingSize(streamConfigurationMap, resolution);

        if (cameraPermissionContinuation != null) {
          result.error("cameraPermission", "Camera permission request ongoing", null);
        }
        cameraPermissionContinuation =
                new Runnable() {
                  @Override
                  public void run() {
                    cameraPermissionContinuation = null;
                    if (!hasCameraPermission()) {
                      result.error(
                              "cameraPermission", "MediaRecorderCamera permission not granted", null);
                      return;
                    }
                    open(result);
                  }
                };
        if (hasCameraPermission()) {
          cameraPermissionContinuation.run();
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.activity == null) {
              throw new IllegalStateException("No activity available!");
            }

            this.activity.requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_ID);
          }
        }
      } catch (CameraAccessException e) {
        result.error("CameraAccess", e.getMessage(), null);
      } catch (IllegalArgumentException e) {
        result.error("IllegalArgumentException", e.getMessage(), null);
      }
    }

    private void registerEventChannel() {
      new EventChannel(
              pluginBinding.getBinaryMessenger(), "flutter_vision/events")
              .setStreamHandler(
                      new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object arguments, EventChannel.EventSink eventSink) {
                          MLCamera.this.eventSink = eventSink;
                        }

                        @Override
                        public void onCancel(Object arguments) {
                          MLCamera.this.eventSink = null;
                        }
                      });
    }

    private boolean hasCameraPermission() {
      if (activity == null) {
        throw new IllegalStateException("No activity available!");
      }

      return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
              || activity.checkSelfPermission(Manifest.permission.CAMERA)
              == PackageManager.PERMISSION_GRANTED;
    }

    private void computeBestPreviewAndRecordingSize(
            StreamConfigurationMap streamConfigurationMap, Pair<Integer, Integer> resolution) {
      Size[] sizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);

      // Preview size and video size should not be greater than screen resolution or 1080.
      Point screenResolution = new Point();

      if (activity == null) {
        throw new IllegalStateException("No activity available!");
      }

      Display display = activity.getWindowManager().getDefaultDisplay();
      display.getRealSize(screenResolution);

      float resolutionPixels = resolution.first * resolution.second;
      Size size = null;

      List<Size> sizeList = new ArrayList<>();
      for (Size s : sizes) {
        float sizePixels = s.getHeight() * s.getWidth();
        if (resolutionPixels <= sizePixels) {
          if(size == null) {
            size = s;
          } else {
            float currentSizePixes = size.getHeight() * size.getWidth();
            size = sizePixels - resolutionPixels < currentSizePixes - resolutionPixels ? s : size;
          }
        }
        sizeList.add(s);
      }

      if (size == null) {
        previewSize = sizeList.get(0);
      } else {
        previewSize = size;
      }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
      mBackgroundThread = new HandlerThread("CameraBackground");
      mBackgroundThread.start();
      mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /** Stops the background thread and its {@link Handler}. */
    private void stopBackgroundThread() {
      if (mBackgroundThread != null) {
        mBackgroundThread.quitSafely();
        try {
          mBackgroundThread.join();
          mBackgroundThread = null;
          mBackgroundHandler = null;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    private int getRotation() {
      if (windowManager == null) {
        windowManager = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
      }
      int degrees = 0;
      int rotation = windowManager.getDefaultDisplay().getRotation();
      switch (rotation) {
        case Surface.ROTATION_0:
          degrees = 0;
          break;
        case Surface.ROTATION_90:
          degrees = 90;
          break;
        case Surface.ROTATION_180:
          degrees = 180;
          break;
        case Surface.ROTATION_270:
          degrees = 270;
          break;
        default:
          Log.e("ML", "Bad rotation value: $rotation");
      }

      try {
        int angle;
        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(cameraName);
        Integer orientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // back-facing
        angle = (orientation - degrees + 360) % 360;
        int translatedAngle = angle / 90;
        return translatedAngle; // this corresponds to the rotation constants
      } catch (CameraAccessException e) {
        return 0;
      }
    }

    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

    private void processImage(Image image) {
      if (eventSink == null || currentDetector == null) return;
      if (shouldThrottle.get()) {
        return;
      }
      shouldThrottle.set(true);
      FirebaseVisionImageMetadata metadata =
              new FirebaseVisionImageMetadata.Builder()
                      .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                      .setWidth(image.getWidth())
                      .setHeight(image.getHeight())
                      .setRotation(getRotation())
                      .build();
      FirebaseVisionImage firebaseVisionImage =
              FirebaseVisionImage.fromMediaImage(image, metadata.getRotation());


      ByteArrayOutputStream out = new ByteArrayOutputStream();
      firebaseVisionImage.getBitmap().compress(Bitmap.CompressFormat.JPEG, 90, out);
      FlutterVisionPlugin.image = out.toByteArray();

      currentDetector.handleDetection(firebaseVisionImage, eventSink, shouldThrottle);
    }

    private final ImageReader.OnImageAvailableListener imageAvailable =
            new ImageReader.OnImageAvailableListener() {
              @Override
              public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                  processImage(image);
                  image.close();
                }
              }
            };

    private void open(@Nullable final Result result) {
      if (!hasCameraPermission()) {
        if (result != null) result.error("cameraPermission", "Camera permission not granted", null);
      } else {
        try {
          startBackgroundThread();
          imageReader =
                  ImageReader.newInstance(
                          previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
          imageReader.setOnImageAvailableListener(imageAvailable, mBackgroundHandler);
          cameraManager.openCamera(
                  cameraName,
                  new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                      MLCamera.this.cameraDevice = cameraDevice;
                      try {
                        startPreview();
                      } catch (CameraAccessException e) {
                        if (result != null) result.error("CameraAccess", e.getMessage(), null);
                        cameraDevice.close();
                        MLCamera.this.cameraDevice = null;
                        return;
                      }

                      if (result != null) {
                        Map<String, Object> reply = new HashMap<>();
                        reply.put("textureId", textureEntry.id());
                        reply.put("width", previewSize.getWidth());
                        reply.put("height", previewSize.getHeight());
                        result.success(reply);
                      }
                    }

                    @Override
                    public void onClosed(@NonNull CameraDevice camera) {
                      if (eventSink != null) {
                        Map<String, String> event = new HashMap<>();
                        event.put("eventType", "cameraClosing");
                        eventSink.success(event);
                      }
                      super.onClosed(camera);
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                      cameraDevice.close();
                      MLCamera.this.cameraDevice = null;
                      sendErrorEvent("The camera was disconnected.");
                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int errorCode) {
                      cameraDevice.close();
                      MLCamera.this.cameraDevice = null;
                      String errorDescription;
                      switch (errorCode) {
                        case ERROR_CAMERA_IN_USE:
                          errorDescription = "The camera device is in use already.";
                          break;
                        case ERROR_MAX_CAMERAS_IN_USE:
                          errorDescription = "Max cameras in use";
                          break;
                        case ERROR_CAMERA_DISABLED:
                          errorDescription =
                                  "The camera device could not be opened due to a device policy.";
                          break;
                        case ERROR_CAMERA_DEVICE:
                          errorDescription = "The camera device has encountered a fatal error";
                          break;
                        case ERROR_CAMERA_SERVICE:
                          errorDescription = "The camera service has encountered a fatal error.";
                          break;
                        default:
                          errorDescription = "Unknown camera error";
                      }
                      sendErrorEvent(errorDescription);
                    }
                  },
                  null);
        } catch (CameraAccessException e) {
          if (result != null) result.error("cameraAccess", e.getMessage(), null);
        }
      }
    }

    private void startPreview() throws CameraAccessException {
      closeCaptureSession();

      SurfaceTexture surfaceTexture = textureEntry.surfaceTexture();
      surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
      captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

      List<Surface> surfaces = new ArrayList<>();

      Surface previewSurface = new Surface(surfaceTexture);
      surfaces.add(previewSurface);
      captureRequestBuilder.addTarget(previewSurface);

      surfaces.add(imageReader.getSurface());
      captureRequestBuilder.addTarget(imageReader.getSurface());

      cameraDevice.createCaptureSession(
              surfaces,
              new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                  if (cameraDevice == null) {
                    sendErrorEvent("The camera was closed during configuration.");
                    return;
                  }
                  try {
                    cameraCaptureSession = session;
                    captureRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                  } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                    sendErrorEvent(e.getMessage());
                  }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                  sendErrorEvent("Failed to configure the camera for preview.");
                }
              },
              null);
    }

    private void sendErrorEvent(String errorDescription) {
      if (eventSink != null) {
        Map<String, String> event = new HashMap<>();
        event.put("eventType", "error");
        event.put("errorDescription", errorDescription);
        eventSink.success(event);
      }
    }

    private void closeCaptureSession() {
      if (cameraCaptureSession != null) {
        cameraCaptureSession.close();
        cameraCaptureSession = null;
      }
    }

    private void close() {
      closeCaptureSession();

      if (cameraDevice != null) {
        cameraDevice.close();
        cameraDevice = null;
      }
      if (imageReader != null) {
        imageReader.close();
        imageReader = null;
      }
      stopBackgroundThread();
    }

    private void dispose() {
      close();
      textureEntry.release();
    }

    private int getMediaOrientation() {
      final int sensorOrientationOffset =
              (currentOrientation == ORIENTATION_UNKNOWN)
                      ? 0
                      : (isFrontFacing) ? -currentOrientation : currentOrientation;
      return (sensorOrientationOffset + sensorOrientation + 360) % 360;
    }
  }
}
