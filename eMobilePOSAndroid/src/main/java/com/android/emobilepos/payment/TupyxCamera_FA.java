package com.android.emobilepos.payment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;

import com.android.emobilepos.R;
import com.android.support.Global;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;
import com.crashlytics.android.Crashlytics;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

public class TupyxCamera_FA extends BaseFragmentActivityActionBar {
    private Camera mCamera;
    private TupyxCameraPreview_SV mPreview;
    private Handler autoFocusHandler;

    private ImageScanner scanner;
    private boolean previewing = true;
    private Global global;
    private boolean hasBeenCreated = false;

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tupyx_camera_layout);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        global = (Global) getApplication();

//        autoFocusHandler = new Handler();
//        mCamera = getCameraInstance();
//        
//
//        /* Instance barcode scanner */
//        scanner = new ImageScanner();
//        scanner.setConfig(0, Config.X_DENSITY, 3);
//        scanner.setConfig(0, Config.Y_DENSITY, 3);
//
//        mPreview = new TupyxCameraPreview_SV(this, mCamera, previewCb, autoFocusCB);
//        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
//        preview.addView(mPreview);
        initCamera();
        hasBeenCreated = true;
    }

//    public void onPause() {
//        super.onPause();
//        releaseCamera();
//    }

    @Override
    protected void onResume() {
        super.onResume();

        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();

        if (hasBeenCreated && !Global.loggedIn) {
            if (global.getGlobalDlog() != null)
                global.getGlobalDlog().dismiss();
            global.promptForMandatoryLogin(this);
        }
//		if(hasBeenCreated)
//			initCamera();
        if (mCamera == null)
            initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

//        PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
//		boolean isScreenOn = powerManager.isScreenOn();
//        MyPreferences myPref = new MyPreferences(this);
//		if(!isScreenOn && myPref.isExpireUserSession())
//			Global.loggedIn = false;
        global.startActivityTransitionTimer();
        if (mCamera != null)
            releaseCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
        return c;
    }

    private void initCamera() {
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new TupyxCameraPreview_SV(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    Bundle resultData = new Bundle();
                    resultData.putString("result", sym.getData());
                    Intent intent = new Intent();
                    intent.putExtras(resultData);
                    setResult(0, intent);
                    finish();
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
}
