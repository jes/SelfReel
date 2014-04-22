package xxx.jes.selfreel.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;

public class SelfReel extends Activity {

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_reel);

        Camera c = null;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                c = Camera.open(i);
            }
        }
        if (c == null) {
            Toast.makeText(getApplicationContext(), "You need a front-facing camera.", Toast.LENGTH_SHORT);
            return;
        }

        c.setDisplayOrientation(90);

        CameraPreview preview = new CameraPreview(this, c);
        FrameLayout previewframe = (FrameLayout) findViewById(R.id.cameraPreview);
        previewframe.addView(preview);
    }

    /** A basic Camera preview class */
    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private static final double ASPECT_RATIO = 3.0 / 4.0;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        /**
         * Measure the view and its content to determine the measured width and the
         * measured height.
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);

            if (width < height * ASPECT_RATIO) {
                width = (int) (height * ASPECT_RATIO + .5);
            } else {
                height = (int) (width / ASPECT_RATIO + .5);
            }

            setMeasuredDimension(width, height);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("TAG", "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                Camera.Parameters params = mCamera.getParameters();
                params.setRecordingHint(true);
                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d("TAG", "Error starting camera preview: " + e.getMessage());
            }
        }
    }
}
