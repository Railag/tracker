package com.firrael.tracker.openCV;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.firrael.tracker.R;
import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.tesseract.Tesseract;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.features2d.MSER;

import java.util.ArrayList;
import java.util.List;

import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.opencv.android.Utils.matToBitmap;

/**
 * Created by railag on 26.12.2017.
 */

public class OpenCVFRagment extends SimpleFragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String TAG = OpenCVFRagment.class.getSimpleName();

    private final static Scalar CONTOUR_COLOR = Scalar.all(100);

    public static OpenCVFRagment newInstance() {

        Bundle args = new Bundle();

        OpenCVFRagment fragment = new OpenCVFRagment();
        fragment.setArguments(args);
        return fragment;
    }

    private Tesseract tesseract;
    private MSER detector;
    private JavaCameraView mOpenCVCameraView;
    private Mat mGrey, mRgba, mIntermediateMat;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCVCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FULLSCREEN MODE
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getMainActivity().transparentStatusBar();
        getMainActivity().hideToolbar();

        tesseract = new Tesseract(getActivity());
        detector = MSER.create();
    }

    @Override
    protected void initView(View v) {
        mOpenCVCameraView = v.findViewById(R.id.javaCameraView);
        mOpenCVCameraView.setOnClickListener(v1 -> {
            if (tesseract.isAvailable()) {
                detectTextAsync();
            } else {
                Toast.makeText(getActivity(), "Wait till previous recognition is finished", Toast.LENGTH_SHORT).show();
            }
        });

        mOpenCVCameraView.setVisibility(View.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mGrey = inputFrame.gray();
        mRgba = inputFrame.rgba();
        mIntermediateMat = mGrey;

        //    detectRegions();
        return mRgba;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, getActivity(), mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mGrey = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void detectTextAsync() {
        List<Bitmap> regions = detectRegions();

        Observable<List<String>> observable = tesseract.getOCRResult(regions);

        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccess, this::onError);
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private void onSuccess(List<String> s) {
        Log.i(TAG, "Results: " + s);
    }

    private List<Bitmap> detectRegions() {
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;
        int imgsize = mGrey.height() * mGrey.width();
        Scalar zeros = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3;
        //

        detector.detect(mGrey, keypoint);
        listpoint = keypoint.toList();

        for (int i = 0; i < listpoint.size(); i++) {
            kpoint = listpoint.get(i);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGrey.width())
                rectanx2 = mGrey.width() - rectanx1;
            if ((rectany1 + rectany2) > mGrey.height())
                rectany2 = mGrey.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d(TAG, "mat roi error " + ex.getMessage());
            }
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel); // dilate filter

        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        List<Bitmap> bitmapsToRecognize = new ArrayList<>();

        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(),
                    CONTOUR_COLOR);
            Bitmap bmp = null;
            try {
                Mat croppedPart;
                croppedPart = mIntermediateMat.submat(rectan3); // gain mat from rectan3?
                bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                matToBitmap(croppedPart, bmp);
            } catch (Exception e) {
                Log.d(TAG, "cropped part data error " + e.getMessage());
            }
            if (bmp != null) {
                //    recognize(bmp);
                bitmapsToRecognize.add(bmp);
                //bmp.recycle();
                //Log.i(TAG, "Result: " + result);
            }
        }

        return bitmapsToRecognize;
    }


    @Override
    protected String getTitle() {
        return getString(R.string.app_name);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_opencv;
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped called");
    }
}
