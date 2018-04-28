//package cs.ubbcluj.rosana.cleanphoto;
//
//import android.app.Activity;
//import android.hardware.Camera;
//import android.util.Log;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//
//import java.io.IOException;
//
//
//public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
//    private SurfaceHolder mHolder;
//    private Camera mCamera;
//    private int mCameraId;
//    private Activity mActivity;
//    private String TAG = this.getClass().getName();
//
//    public CameraPreview(Activity activity, int cameraId, Camera camera) {
//        super(activity.getApplicationContext());
//        mCamera = camera;
//        mCameraId = cameraId;
//        mActivity = activity;
//
//        // Install a SurfaceHolder.Callback so we get notified when the
//        // underlying surface is created and destroyed.
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//        // deprecated setting, but required on Android versions prior to 3.0
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        if (mCamera == null) {
//            return;
//        }
//        // The Surface has been created, now tell the camera where to draw the
//        // preview.
//        try {
//            mCamera.setPreviewDisplay(holder);
//            mCamera.startPreview();
//        } catch (IOException e) {
//            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
//        }
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        // empty. Take care of releasing the Camera preview in your activity.
//    }
//
//    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        if (mCamera == null) {
//            return;
//        }
//
//        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
//
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//
//                // ***The parameter 'data' holds the frame information***
//				/*
//				 * int width = 0; int height = 0;
//				 *
//				 * Camera.Parameters parameters = camera.getParameters();
//				 *
//				 * height = parameters.getPreviewSize().height;
//				 *
//				 * width = parameters.getPreviewSize().width;
//				 */
//
//                // ****You can change formats, save the data
//                // to file etc.*****
//				/*
//				 * YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
//				 * width, height, null);
//				 *
//				 * Rect rectangle = new Rect(0, 0, width, height);
//				 *
//				 * yuvImage.compressToJpeg(rectangle, 100, stream);
//				 */
//            }
//
//        });
//
//        // If your preview can change or rotate, take care of those events here.
//        // Make sure to stop the preview before resizing or reformatting it.
//
//        if (mHolder.getSurface() == null) {
//            // preview surface does not exist
//            return;
//        }
//
//        // stop preview before making changes
//        try {
//            mCamera.stopPreview();
//        } catch (Exception e) {
//            // ignore: tried to stop a non-existent preview
//        }
//
//        // set preview size and make any resize, rotate or
//        // reformatting changes here
//        CameraOrientationHelper.setCameraDisplayOrientation(mActivity, mCameraId, mCamera);
//
//        // start preview with new settings
//        try {
//            mCamera.setPreviewDisplay(mHolder);
//
//            mCamera.startPreview();		} catch (Exception e) {
//            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
//        }
//    }
//}