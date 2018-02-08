package com.firrael.tracker.openCV;

import android.content.Context;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;
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

    private boolean meteringAreaSupported;

    public FocusCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void initializeCamera() {
        Camera.Parameters params = mCamera.getParameters();
        String previewSize = params.get("preview-size");
        Log.i(TAG, "Preview size: " + previewSize);
        //params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    }

    public void switchFlashMode(String mode) {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(mode);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            focusOnTouch(event);
        }
        return super.onTouchEvent(event);
    }

    protected void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {

            mCamera.cancelAutoFocus();
            android.graphics.Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            android.graphics.Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);

            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
                this.meteringAreaSupported = true;
            }

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 1000));
            parameters.setFocusAreas(focusAreas);

            for (Camera.Area area : focusAreas) {
                Log.e(TAG, area.rect.flattenToString());
            }

            if (meteringAreaSupported) {
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(new Camera.Area(meteringRect, 1000));
                parameters.setMeteringAreas(meteringAreas);
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus((b, camera) -> {
            });
        }
    }

    /**
     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
     */
    private android.graphics.Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 72; // TODO update?
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        getMatrix().mapRect(rectF);

        final int l = clamp(Math.round(rectF.left), -1000, 900);
        final int t = clamp(Math.round(rectF.top), -1000, 900);
        final int r = clamp(Math.round(rectF.right), -900, 1000);
        final int b = clamp(Math.round(rectF.bottom), -900, 1000);

        return new android.graphics.Rect(l, t, r, b);
        //return new android.graphics.Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public void setFocusMode(int type) {

        Camera.Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        mCamera.autoFocus((b, camera) -> {
        });

        List<String> focusModes = params.getSupportedFocusModes();

        switch (type) {
            case FOCUS_AUTO:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                else
                    Log.i(TAG, "Auto Mode is not supported");
                break;
            case FOCUS_CONTINUOUS_VIDEO:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else {
                    Log.i(TAG, "Continuous Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_EDOF:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                else {
                    Log.i(TAG, "EDOF Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_FIXED:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                else {
                    Log.i(TAG, "Fixed Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_INFINITY:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                else {
                    Log.i(TAG, "Infinity Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_MACRO:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else {
                    Log.i(TAG, "Macro Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
            case FOCUS_CONTINUOUS_PICTURE:
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                else {
                    Log.i(TAG, "Continuous Picture Mode is not supported");
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                break;
        }

        mCamera.setParameters(params);
    }
}