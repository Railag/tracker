package com.firrael.tracker.openCV;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by railag on 04.01.2018.
 */

public class FocusCameraView extends JavaCameraView {
    private final static String TAG = FocusCameraView.class.getSimpleName();

    private final static int PREVIEW_WIDTH = 800;
    private final static int PREVIEW_HEIGHT = 600;

    public final static int FOCUS_AUTO = 0;
    public final static int FOCUS_CONTINUOUS_VIDEO = 1;
    public final static int FOCUS_EDOF = 2;
    public final static int FOCUS_FIXED = 3;
    public final static int FOCUS_INFINITY = 4;
    public final static int FOCUS_MACRO = 5;
    public final static int FOCUS_CONTINUOUS_PICTURE = 6;

    public FocusCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void initializeCamera() {
        Camera.Parameters params = mCamera.getParameters();
        String previewSize = params.get("preview-size");
        Log.i(TAG, "Preview size: " + previewSize);
        //params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    public void setFocusMode(int type) {

        Camera.Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        mCamera.autoFocus((b, camera) -> {
        });

        List<String> FocusModes = params.getSupportedFocusModes();

        switch (type) {
            case FOCUS_AUTO:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                else
                    Log.i(TAG, "Auto Mode is not supported");
                break;
            case FOCUS_CONTINUOUS_VIDEO:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else {
                    Log.i(TAG, "Continuous Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_EDOF:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                else {
                    Log.i(TAG, "EDOF Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_FIXED:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                else {
                    Log.i(TAG, "Fixed Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_INFINITY:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                else {
                    Log.i(TAG, "Infinity Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_MACRO:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else {
                    Log.i(TAG, "Macro Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_CONTINUOUS_PICTURE:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                else {
                    Log.i(TAG, "Continuous Picture Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
        }

        mCamera.setParameters(params);
    }
}