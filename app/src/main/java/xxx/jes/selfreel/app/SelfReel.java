package xxx.jes.selfreel.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SelfReel extends Activity {

    public boolean recording = true;
    public static View overlay;

    public long startms = 0;
    public long lastms = 0;
    public long INITIAL_TIME = 1000;
    public long MS_TIMER = 2500;

    public ArrayList<String> filenames = new ArrayList<String>();

    public TextView textView;

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

        overlay = new ImageView(this);
        overlay.setBackgroundColor(0xffffffff);
        overlay.setAlpha(0.0f);
        previewframe.addView(overlay);

        RelativeLayout rl = new RelativeLayout(this);
        previewframe.addView(rl);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        rl.addView(ll);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)ll.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        ll.setLayoutParams(params);

        Button btn = new Button(this);
        btn.setText("Pause");
        ll.addView(btn);
        btn.setWidth(200);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button)v;

                if (recording) {
                    recording = false;
                    btn.setText("Resume");
                } else {
                    recording = true;
                    btn.setText("Pause");
                    startms = System.currentTimeMillis() - (MS_TIMER - INITIAL_TIME);
                    lastms = -1000;
                }
            }
        });
        final Button pauseButton = btn;

        btn = new Button(this);
        btn.setText("Done");
        ll.addView(btn);
        btn.setWidth(200);
        final SelfReel selfreel = this;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording = false;
                pauseButton.setText("Resume");
                Intent intent = new Intent(selfreel, KeepSelfiesActivity.class);
                intent.putExtra("filenames", filenames);
                startActivity(intent);
            }
        });

        textView = new TextView(this);
        textView.setText("0");
        textView.setTextSize(50f);
        ll.addView(textView);
    }

    /** A basic Camera preview class */
    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera = null;
        private static final double ASPECT_RATIO = 3.0 / 4.0;

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                long ms = System.currentTimeMillis() - startms;
                if (recording && ms / MS_TIMER != lastms / MS_TIMER) {
                    SelfReel.overlay.setAlpha(0.25f);
                    mCamera.takePicture(null, null, mPicture);
                    lastms = ms;
                }
                long secs = MS_TIMER - (ms % MS_TIMER);
                secs /= 100;
                if (recording)
                    textView.setText("" + secs);
                else
                    textView.setText("0");
                timerHandler.postDelayed(this, 100);
            }
        };
        Runnable resumePreview = new Runnable() {
            @Override
            public void run() {
                SelfReel.overlay.setAlpha(0.0f);
            }
        };

        Camera.PictureCallback mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.startPreview();

                String pictureFileName = getOutputMediaFileName(MEDIA_TYPE_IMAGE);
                File pictureFile = new File(pictureFileName);
                if (pictureFile == null){
                    Log.d("TAG", "Error creating media file, check storage permissions.");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(pictureFile));
                    sendBroadcast(intent);
                    filenames.add(pictureFileName);
                    timerHandler.postDelayed(resumePreview, 200);
                } catch (FileNotFoundException e) {
                    Log.d("TAG", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("TAG", "Error accessing file: " + e.getMessage());
                }
            }
        };

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
                params.set("rotation", 270);
                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d("TAG", "Error starting camera preview: " + e.getMessage());
            }

            startms = System.currentTimeMillis() - (MS_TIMER - INITIAL_TIME);
            timerHandler.postDelayed(timerRunnable, 0);
        }

        public static final int MEDIA_TYPE_IMAGE = 1;
        public static final int MEDIA_TYPE_VIDEO = 2;
        public final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());
        public int fileidx = 0;

        /** Create a File for saving an image or video */
        private String getOutputMediaFileName(int type){
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "SelfReel");
            Log.d("TAG", "ExternalStoragePublicDirectory = " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    Log.d("SelfReel", "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String mediaFileName;
            if (type == MEDIA_TYPE_IMAGE){
                mediaFileName = mediaStorageDir.getPath() + File.separator +
                        "IMG_"+ timeStamp + fileidx + ".jpg";
            } else if(type == MEDIA_TYPE_VIDEO) {
                mediaFileName = mediaStorageDir.getPath() + File.separator +
                        "VID_"+ timeStamp + fileidx + ".mp4";
            } else {
                return null;
            }

            fileidx++;

            return mediaFileName;
        }
    }
}
