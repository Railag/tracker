package com.firrael.tracker.openCV;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by railag on 04.01.2018.
 */

public class FocusCameraView extends JavaCameraView {

    public FocusCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
                    Toast.makeText(item, "Auto Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else {
                    Toast.makeText(item, "Continuous Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 2:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                else {
                    Toast.makeText(item, "EDOF Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 3:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                else {
                    Toast.makeText(item, "Fixed Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 4:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                else {
                    Toast.makeText(item, "Infinity Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 5:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else {
                    Toast.makeText(item, "Macro Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case 6:
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                else {
                    Toast.makeText(item, "Continuous Picture Mode is not supported", Toast.LENGTH_SHORT).show();
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
        }

        mCamera.setParameters(params);
    }
}