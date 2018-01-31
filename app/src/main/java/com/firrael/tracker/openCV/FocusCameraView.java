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

    public FocusCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void initializeCamera() {
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    public void setFocusMode(Context item, int type) {

        Camera.Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        mCamera.autoFocus((b, camera) -> {
        });

        List<String> FocusModes = params.getSupportedFocusModes();

        switch (type) {
            case 0:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                else
                    Log.i(TAG, "Auto Mode is not supported");
                break;
            case 1:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else {
                    Log.i(TAG, "Continuous Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 2:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                else {
                    Log.i(TAG, "EDOF Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 3:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                else {
                    Log.i(TAG, "Fixed Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 4:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                else {
                    Log.i(TAG, "Infinity Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 5:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else {
                    Log.i(TAG, "Macro Mode is not supported");
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 6:
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