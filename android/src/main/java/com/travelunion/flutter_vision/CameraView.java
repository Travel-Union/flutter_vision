package com.travelunion.flutter_vision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.util.Rational;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.travelunion.flutter_vision.detectors.BarcodeDetectionProcessor;
import com.travelunion.flutter_vision.detectors.FaceContourDetectionProcessor;
import com.travelunion.flutter_vision.detectors.TextDetectionProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;

public class CameraView implements PlatformView, MethodChannel.MethodCallHandler, EventChannel.StreamHandler, TextDetectionProcessor.Result, FaceContourDetectionProcessor.Result, BarcodeDetectionProcessor.Result {

    private final MethodChannel methodChannel;
    private EventChannel.EventSink eventSink;
    PreviewView mPreviewView;
    private Executor executor = Executors.newSingleThreadExecutor();
    Camera camera;
    int flashMode = ImageCapture.FLASH_MODE_AUTO;
    ImageCapture imageCapture;
    int cameraId = 0;
    int lensFacing = CameraSelector.LENS_FACING_BACK;
    FlutterPlugin.FlutterPluginBinding flutterPluginBinding;
    FlutterVisionPlugin plugin;
    Context context;
    Rational aspectRatio = new Rational(16,9);
    ProcessCameraProvider cameraProvider;
    private static final int CAMERA_REQUEST_ID = 327123094;
    boolean torchMode = false;
    ImageAnalysis imageAnalysis;
    OrientationEventListener orientationEventListener;
    FaceContourDetectionProcessor faceDetector;
    TextDetectionProcessor textRecognizer;
    BarcodeDetectionProcessor barcodeDetector;


    CameraView(Context context, BinaryMessenger messenger, int id, FlutterPlugin.FlutterPluginBinding flutterPluginBinding, FlutterVisionPlugin plugin) {
        methodChannel = new MethodChannel(messenger, Constants.methodChannelId + "_0");
        new EventChannel(messenger, Constants.methodChannelId + "/events").setStreamHandler(this);
        this.cameraId = id;
        this.context = context;
        this.plugin = plugin;
        this.flutterPluginBinding = flutterPluginBinding;
        methodChannel.setMethodCallHandler(this);
        mPreviewView = new PreviewView(context);
        mPreviewView.setImportantForAccessibility(0);
        mPreviewView.setMinimumHeight(100);
        mPreviewView.setMinimumWidth(100);
        mPreviewView.setContentDescription("Description Here");
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_START);
    }

    private void startCamera(final Context context, FlutterRequest myRequest, final FlutterVisionPlugin plugin) {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if(cameraProvider != null) {
                        return;
                    }

                    cameraProvider = cameraProviderFuture.get();

                    bindPreview(cameraProvider, myRequest, plugin);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                    myRequest.reportError("initialize", "Failed to initialize camera: " + e.getLocalizedMessage(), null);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider, FlutterRequest myRequest, FlutterVisionPlugin plugin) {
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        Preview.Builder previewBuilder = new Preview.Builder();
        @SuppressLint("RestrictedApi")
        Preview preview = previewBuilder
                .setTargetResolution(new Size(width, (int)(width * 4 / 3)))
                .build();

        final CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(720,960))
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        imageCapture = builder
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(plugin.activityPluginBinding.getActivity().getWindowManager().getDefaultDisplay().getRotation())
                .setTargetRotation(Surface.ROTATION_0)
                .build();

        orientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }

                if(imageCapture != null) {
                    imageCapture.setTargetRotation(rotation);
                }
            }
        };

        orientationEventListener.enable();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        imageCapture.setFlashMode(flashMode);

        if(cameraProvider != null) {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(((LifecycleOwner) plugin.activityPluginBinding.getActivity()), cameraSelector, preview, imageAnalysis, imageCapture);
        }

        camera.getCameraControl().enableTorch(torchMode);

        Map<String, Object> reply = new HashMap<>();
        reply.put("width", mPreviewView.getWidth());
        reply.put("height", mPreviewView.getHeight());

        myRequest.submit(reply);
    }

    void captureImage(final FlutterRequest myRequest){
        imageCapture.setFlashMode(flashMode);

        float x = (float) (mPreviewView.getHeight() * 0.25);
        float y = (float) (mPreviewView.getWidth() * 0.5);

        MeteringPoint meteringPoint = new DisplayOrientedMeteringPointFactory(mPreviewView.getDisplay(), camera.getCameraInfo(), mPreviewView.getWidth(), mPreviewView.getHeight()).createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
        final CameraControl cameraControl = camera.getCameraControl();
        cameraControl.startFocusAndMetering(action);

        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull final ImageProxy image) {
                plugin.activityPluginBinding.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("UnsafeExperimentalUsageError")
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                        Bitmap rotated = rotateImageIfRequired(bitmapImage, image.getImageInfo());

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        rotated.compress(Bitmap.CompressFormat.JPEG, 90, out);

                        myRequest.submit(out.toByteArray());
                        image.close();
                    }
                });
                super.onCaptureSuccess(image);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
            }
        });
    }

    private void setLensFacing(String lensFacing){
        this.lensFacing = Utils.getLensFacingFromString(lensFacing);
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, ImageInfo imageInfo) {
        int orientation = imageInfo.getRotationDegrees();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull MethodChannel.Result result) {
        final FlutterRequest myRequest = new FlutterRequest(call, result);

        switch ((String)(call.method)) {
            case MethodNames.capture:
                captureImage(myRequest);
                break;
            case MethodNames.initialize:
                setLensFacing((String)call.argument("lensFacing"));
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    plugin.activityPluginBinding.getActivity().requestPermissions(
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST_ID);
                    plugin.activityPluginBinding.addRequestPermissionsResultListener(new PluginRegistry.RequestPermissionsResultListener() {
                        @Override
                        public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                            if(requestCode==CAMERA_REQUEST_ID && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                                startCamera(context, myRequest, plugin);
                            } else {
                                myRequest.reportError("initialize", "Failed to initialize camera due to permissions not being granted.", null);
                            }
                            return false;
                        }
                    });
                } else {
                    myRequest.reportError("initialize", "Failed to initialize because Android M is required to operate.", null);
                }
                break;
            case MethodNames.setAspectRatio:
                try {
                    aspectRatio = new Rational((int)(call.argument("num")), (int)(call.argument("denom")));
                    myRequest.submit(true);
                }catch (Exception e){
                    myRequest.reportError("-2","Invalid Aspect Ratio","Invalid Aspect Ratio");
                }
                break;
            case MethodNames.addFaceDetector:
                faceDetector = new FaceContourDetectionProcessor(this);
                imageAnalysis.setAnalyzer(executor, faceDetector);
                myRequest.submit(true);
                break;
            case MethodNames.addTextRegonizer:
                textRecognizer = new TextDetectionProcessor(this);
                imageAnalysis.setAnalyzer(executor, textRecognizer);
                myRequest.submit(true);
                break;
            case MethodNames.addBarcodeDetector:
                barcodeDetector = new BarcodeDetectionProcessor(this);
                imageAnalysis.setAnalyzer(executor, barcodeDetector);
                myRequest.submit(true);
                break;
            case MethodNames.closeFaceDetector:
            case MethodNames.closeTextRegonizer:
            case MethodNames.closeBarcodeDetector:
                myRequest.submit(true);
                break;
            case MethodNames.dispose:
                dispose();
                myRequest.submit(true);
            default:
                myRequest.notImplemented();
        }
    }

    @Override
    public View getView() {
        return mPreviewView;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void dispose() {
        if(imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }

        if(cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider.shutdown();
        }

        if(orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }

        camera = null;
        imageCapture = null;
        imageAnalysis = null;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }

    @Override
    public void onTextResult(Map<String, Object> result) {
        if(this.eventSink != null) {
            this.eventSink.success(result);
        }
    }

    @Override
    public void onTextError(Exception e) {
        if(this.eventSink != null) {
            this.eventSink.error("TextRecognizer", e.getLocalizedMessage(), null);
        }
    }

    @Override
    public void onFaceResult(Map<String, Object> result) {
        if(this.eventSink != null) {
            this.eventSink.success(result);
        }
    }

    @Override
    public void onFaceError(Exception e) {
        if(this.eventSink != null) {
            this.eventSink.error("FaceDetector", e.getLocalizedMessage(), null);
        }
    }

    @Override
    public void onBarcodeResult(Map<String, Object> result) {
        if(this.eventSink != null) {
            this.eventSink.success(result);
        }
    }

    @Override
    public void onBarcodeError(Exception e) {
        if(this.eventSink != null) {
            this.eventSink.error("BarcodeDetector", e.getLocalizedMessage(), null);
        }
    }
}
