package com.firrael.tracker.openCV;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.firrael.tracker.App;
import com.firrael.tracker.DriveUtils;
import com.firrael.tracker.OpenCVUtils;
import com.firrael.tracker.R;
import com.firrael.tracker.TestUtils;
import com.firrael.tracker.Utils;
import com.firrael.tracker.tesseract.RecognizedRegion;
import com.firrael.tracker.tesseract.Tesseract;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.MSER;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by railag on 04.01.2018.
 */

public class OpenCVActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String TAG = OpenCVActivity.class.getSimpleName();

    public final static String KEY_SCAN_RESULTS = "scanResults";
    public final static String KEY_TEST = "test";

    private final static Scalar CONTOUR_COLOR = Scalar.all(100);

    private FocusCameraView mOpenCVCameraView;
    private CropImageView mCropImageView;
    private ImageButton mCropAcceptButton;
    private ImageButton mCropBackButton;
    private ImageButton mCropRotateButton;
    private ImageButton mCropSkewButton;
    private ImageButton mCropClearNoiseButton;
    private LinearLayout mCropButtonsLayout;

    private Tesseract mTesseract;
    private MSER mDetector;
    private Mat mGrey, mRgba, mIntermediateMat;
    private DriveResourceClient mDriveResourceClient;
    private int mTesseractCounter;
    private Tesseract.Language mLanguage;
    private CompositeSubscription mSubscription;
    private Bitmap mSavedSource;
    private Kernel mCurrentNoiseKernel;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (mDetector == null) {
                        initDetector();
                    }

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

    private boolean isTested;
    private String folderName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_opencv);

        mSubscription = new CompositeSubscription();

        initializeLanguage();

        Window window = getWindow();

        //FULLSCREEN MODE
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //transparent toolbar & status bar
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        initializeTesseract();

        mDriveResourceClient = App.getDrive();

        mCropImageView = findViewById(R.id.cropImageView);
        mCropImageView.setAutoZoomEnabled(true);
        mCropImageView.setGuidelines(CropImageView.Guidelines.ON);

        mCropAcceptButton = findViewById(R.id.cropImageAcceptButton);
        mCropBackButton = findViewById(R.id.cropImageBackButton);
        mCropRotateButton = findViewById(R.id.cropImageRotateButton);
        mCropSkewButton = findViewById(R.id.cropImageSkewButton);
        mCropClearNoiseButton = findViewById(R.id.cropImageClearNoiseButton);
        mCropButtonsLayout = findViewById(R.id.cropButtonsLayout);

        mOpenCVCameraView = findViewById(R.id.javaCameraView);
        mOpenCVCameraView.setOnLongClickListener(v1 -> {
            if (mTesseract.isAvailable()) {
                detectRegions(true);
            } else {
                Snackbar.make(mOpenCVCameraView, R.string.recognition_in_progress_error, Snackbar.LENGTH_SHORT).show();
            }

            return true;
        });

        mOpenCVCameraView.setVisibility(View.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);

        Intent intent = getIntent();
        if (intent.hasExtra(KEY_TEST)) {
            isTested = true;
        }
    }

    private void test() {
        int resourceId = R.drawable.test_ocr_image;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resourceId);

        Mat sourceMat = OpenCVUtils.createMat(bmp);

        if (Core.countNonZero(sourceMat) != 0) { // non-empty matrix (and not 0x0 matrix)
            mSavedSource = OpenCVUtils.createBitmap(imagePostProcessing(sourceMat));

            sourceMat = clearNoise(sourceMat);

            sourceMat = OpenCVUtils.createMat(textSkew(sourceMat));

            recognizeMat(sourceMat);
        }

    }

    private void initializeLanguage() {
        mLanguage = Utils.getLanguage(this);
    }

    private void initializeTesseract() {
        mTesseract = new Tesseract(this, mLanguage);

        mTesseractCounter = 0;


        mSubscription.add(Observable
                .range(0, Tesseract.WORKER_POOL_SIZE)
                .subscribe(integer -> {
                    Observable<Integer> observable = getInitNewWorkerObservable(integer);
/*                    observable.doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            // TODO handle stop
                        }
                    });*/
                    mSubscription.add(observable.subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::getCreatedObservable, OpenCVActivity.this::onError));
                }));
    }

    private Observable<Integer> getInitNewWorkerObservable(int integer) {
        return Observable.create(emitter -> {
            mTesseract.initNewWorker();
            emitter.onNext(integer);
        }, Emitter.BackpressureMode.LATEST);
    }

    private void getCreatedObservable(int i) {
        Log.i(TAG, "Worker# " + i + " initialized");
        mTesseractCounter++;
        if (mTesseractCounter == Tesseract.WORKER_POOL_SIZE) {
            Log.i(TAG, "Tesseract initialization finished");
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mGrey = inputFrame.gray();
        mRgba = inputFrame.rgba();

        return mRgba;
    }

    private void initDetector() {
        // C++: static Ptr_MSER create(int _delta = 5, int _min_area = 60, int _max_area = 14400, double _max_variation = 0.25, double _min_diversity = .2, int _max_evolution = 200, double _area_threshold = 1.01, double _min_margin = 0.003, int _edge_blur_size = 5)

        mDetector = MSER.create();
        // TODO use all features    mDetector = MSER.create(5, 60, 14400, 0.25,
        //            0.2, 200, 1.01, 0.003, 5);
        //    mDetector.setMinArea(60);
        //    mDetector.setMaxArea(14400);
        //    mDetector.setDelta(5);
        // TODO ???    mDetector.setPass2Only();
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

        if (!Utils.checkCameraPermission(this)) {
            Utils.verifyCameraPermission(this);
            return;
        }

        if (!Utils.checkDiskPermission(this)) {
            Utils.verifyStoragePermissions(this);
            return;
        }

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();

        if (mTesseract != null) {
            if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                mSubscription.unsubscribe();
            }
            mTesseract.onDestroy();
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mOpenCVCameraView.initializeCamera();
        mOpenCVCameraView.setFocusMode(FocusCameraView.FOCUS_CONTINUOUS_PICTURE); // Continuous Picture Mode

        mIntermediateMat = new Mat();
        mGrey = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);

        if (isTested) {
            test();
        }
    }

    public void processBitmapResults(BitmapResults bitmapResults) {
        List<Bitmap> bitmapRegions = bitmapResults.getRegions();

        if (bitmapRegions == null || bitmapRegions.size() == 0) {
            // no recognized regions
            mTesseract.setAvailable(true);
            Snackbar.make(mOpenCVCameraView,
                    R.string.no_text_recognized_error, Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<BitmapRegion> regions = new ArrayList<>();
        for (int i = 0; i < bitmapRegions.size(); i++) {
            regions.add(new BitmapRegion(i, bitmapRegions.get(i)));
        }

        mTesseract.setAvailable(false);

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setTitle(getString(R.string.loading));
        dialog.setMessage(getString(R.string.text_processing));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMax(regions.size()); // last image in 'regions' is full image, not region
        dialog.setProgress(0);
        dialog.show();

        Observable.create((Action1<Emitter<Void>>) emitter -> {
            uploadBitmapsToGoogleDrive(regions, bitmapResults.getSourceBitmaps());
            emitter.onCompleted();
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                        }, this::onError,
                        () -> Log.i(TAG, "Bitmaps uploading completed."));

        List<RecognizedRegion> results = new ArrayList<>();

        Observable
                .from(regions)
                .flatMap(Observable::just)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapRegion -> Observable.create((Action1<Emitter<RecognizedRegion>>) emitter -> {
                    RecognizedRegion region = mTesseract.processImage(bitmapRegion);
                    emitter.onNext(region);
                }, Emitter.BackpressureMode.LATEST)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            Log.i(TAG, "Result " + result);
                            results.add(result);
                            dialog.setProgress(dialog.getProgress() + 1);
                            if (results.size() == regions.size()) {
                                mTesseract.setAvailable(true);
                                dialog.dismiss();
                                onSuccess(results);
                            }
                        }, this::onError));
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private void onSuccess(List<RecognizedRegion> s) {
        Collections.sort(s);
        Log.i(TAG, "Results: " + s);
        Intent data = new Intent();
        ArrayList<RecognizedRegion> results = new ArrayList<>(s);
        data.putParcelableArrayListExtra(KEY_SCAN_RESULTS, results);
        setResult(RESULT_OK, data);
        showResults(results);
    }

    private void showResults(ArrayList<RecognizedRegion> results) {

        if (isTested) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                RecognizedRegion line = results.get(i);
                builder.append(line);
            }

            uploadDiffToGoogleDrive(builder.toString(), folderName, "diff");
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                RecognizedRegion line = results.get(i);
                builder.append("region#");
                builder.append(line.getRegionNumber());
                builder.append(": ");
                builder.append(line);
                builder.append("\n");
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.results)
                    .setMessage(builder.toString())
                    .setPositiveButton(R.string.done, (d, which) -> {
                        d.dismiss();
                        finish();
                    })
                    .setNeutralButton(R.string.reset_image, (d, which) -> {
                        d.dismiss();
                        resetToSourceImage();
                    })
                    .setNegativeButton(R.string.take_another_image, (d, which) -> {
                        d.dismiss();
                        resetToInitial();
                    })
                    .create();
            dialog.show();
        }
    }

    private void uploadDiffToGoogleDrive(String recognizedText, String folderName, String fileName) {
        String sourceText = TestUtils.readTextFile(this, "test_ocr_text");

        Observable.create((Action1<Emitter<String>>) emitter -> {
            try {
                ParallelDotsHelper.findDiff(this, sourceText, recognizedText, fileName, folderName, mDriveResourceClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
            emitter.onCompleted();
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                        }, this::onError,
                        () -> Log.i(TAG, "Similarity uploaded to Google Drive."));
    }

    private void resetToSourceImage() {
        detectRegions(false);
    }

    private void detectRegions(boolean initial) {
        // TODO PERFECT TEST Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.test_ocr_image);
        //      Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.test_ocr_image_2);
//        Mat sourceMat = imagePostProcessing(OpenCVUtils.createMat(source));
        //    mSavedSource = OpenCVUtils.createBitmap(sourceMat);

        Mat sourceMat = TestUtils.loadImage();

        if (Core.countNonZero(sourceMat) != 0) { // non-empty matrix (and not 0x0 matrix)
            mSavedSource = OpenCVUtils.createBitmap(imagePostProcessing(sourceMat));
        } else {
            TestUtils.saveImage(mGrey.clone());

            if (initial) {
                Mat tmp = imagePostProcessing(mGrey.clone());
                mSavedSource = OpenCVUtils.createBitmap(tmp);
            }
        }

        mCurrentNoiseKernel = Kernel.TINY;

        // crop image
        mCropButtonsLayout.setVisibility(View.VISIBLE);

        mCropAcceptButton.setOnClickListener(v -> {
            Bitmap croppedBitmap = mCropImageView.getCroppedImage();
            Mat croppedMat = OpenCVUtils.createMat(croppedBitmap);

            if (isTested) {
                croppedMat = imagePostProcessing(croppedMat);
            }

            recognizeMat(croppedMat);
        });

        mCropBackButton.setOnClickListener(v -> resetToInitial());

        mCropRotateButton.setOnClickListener(v -> mCropImageView.rotateImage(90));

        mCropClearNoiseButton.setOnClickListener(v -> {
            Bitmap croppedBitmap = mCropImageView.getCroppedImage();
            Mat croppedMat = OpenCVUtils.createMat(croppedBitmap);

            croppedMat = clearNoise(croppedMat);
            Bitmap clearBitmap = OpenCVUtils.createBitmap(croppedMat);
            mCropImageView.setImageBitmap(clearBitmap);
        });

        mCropSkewButton.setOnClickListener(v -> {
            Bitmap croppedBitmap = mCropImageView.getCroppedImage();
            Mat croppedMat = OpenCVUtils.createMat(croppedBitmap);

            Bitmap skewedBitmap = textSkew(croppedMat);
            mCropImageView.setImageBitmap(skewedBitmap);
        });

        mCropImageView.setVisibility(View.VISIBLE);
        mCropImageView.setImageBitmap(mSavedSource);
        mCropImageView.resetCropRect();

        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.disableView();
            mOpenCVCameraView.setVisibility(View.GONE);
        }
    }

    private void recognizeMat(Mat mat) {
        List<Bitmap> sourceImages = new ArrayList<>();
        sourceImages.add(mSavedSource); // full image source

        mIntermediateMat = mat;

        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mat.size(), CvType.CV_8UC1);
        int rectanx1, rectany1, rectanx2, rectany2;

        List<MatOfPoint> contours = new ArrayList<>();
        Mat kernel = Kernel.LARGE.generate();
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3;
        //

        if (mDetector == null) {
            initDetector();
        }

        mDetector.detect(mat, keypoint);
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
            if ((rectanx1 + rectanx2) > mat.width())
                rectanx2 = mat.width() - rectanx1;
            if ((rectany1 + rectany2) > mat.height())
                rectany2 = mat.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d(TAG, "mat roi error " + ex.getMessage());
            }
        }

        Bitmap sourceMask = OpenCVUtils.createBitmap(mask);
        sourceImages.add(sourceMask); // mask before dilation filter, but with detected contours

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel); // dilate filter

        Bitmap sourceImage = OpenCVUtils.createBitmap(morbyte);
        sourceImages.add(sourceImage); // mask after dilation filter

        Imgproc.findContours(morbyte, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        Collections.reverse(contours);

        List<Bitmap> bitmapsToRecognize = new ArrayList<>();

        for (int ind = 0; ind < contours.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contours.get(ind));
            Imgproc.rectangle(mat, rectan3.br(), rectan3.tl(),
                    CONTOUR_COLOR);
            Bitmap bmp = null;
            try {
                Mat croppedPart;
                croppedPart = mIntermediateMat.submat(rectan3);
                bmp = OpenCVUtils.createBitmap(croppedPart);
            } catch (Exception e) {
                Log.d(TAG, "cropped part data error " + e.getMessage());
            }
            if (bmp != null) {
                bitmapsToRecognize.add(bmp);
            }
        }

        Bitmap sourceCropped = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceCropped);

        BitmapResults results = new BitmapResults(bitmapsToRecognize, sourceImages);

        processBitmapResults(results);
    }

    private Mat clearNoise(Mat mat) {
        if (mCurrentNoiseKernel == null) {
            mCurrentNoiseKernel = Kernel.TINY;
        }

        Mat kernel = mCurrentNoiseKernel.generate();

        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, kernel);

        mCurrentNoiseKernel.increase();
        return mat;
    }

    private Mat imagePostProcessing(Mat mat) {
/*        float fishVal = 600.0f;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int cX = displayMetrics.widthPixels; // 1920
        int cY = displayMetrics.heightPixels; // 1080

        Mat k = new Mat(3, 3, CvType.CV_32FC1);
        k.put(0, 0, new float[]{fishVal, 0, cX});
        k.put(1, 0, new float[]{0, fishVal, cY});
        k.put(2, 0, new float[]{0, 0, 1});

        Mat d = new Mat(1, 4, CvType.CV_32FC1);
        d.put(0, 0, new float[]{0, 0, 0, 0});

        Mat kNew = k.clone();
        kNew.put(0, 0, new float[]{fishVal * 0.4f, 0.0f, cX});
        kNew.put(1, 0, new float[]{0.0f, fishVal * 0.4f, cY});
        kNew.put(2, 0, new float[]{0.0f, 0.0f, 1.0f});

    //    Imgproc.undistort(mat, mat, k, d, kNew);
        Calib3d.undistortImage(mat, mat, k, d, kNew, mat.size());*/

        List<Bitmap> sourceImages = new ArrayList<>();

        Bitmap sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

        Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0); // TODO gaussian -> median?
        //Imgproc.medianBlur(mat, mat, 3);

        sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

        Imgproc.adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 5, 4);

        sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

   /*     Mat kernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, kernel);

        sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

        // TODO enable?
        Mat erodeKernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_ERODE, erodeKernel);
*/
        sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

        Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_OTSU);

        sourceImage = OpenCVUtils.createBitmap(mat);
        sourceImages.add(sourceImage);

        uploadBitmapsToGoogleDrive(new ArrayList<>(), sourceImages);

        //    Imgproc.adaptiveThreshold(tmp, tmp, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        return mat;
    }


    private Bitmap textSkew(Mat greyscale) {
        List<Bitmap> sourceImages = new ArrayList<>();

        Bitmap sourceImage = OpenCVUtils.createBitmap(greyscale);
        sourceImages.add(sourceImage);

        Mat source = greyscale.clone();

        //Binarize it
        //Use adaptive threshold if necessary
        Imgproc.adaptiveThreshold(greyscale, greyscale, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        //Imgproc.threshold(greyscale, greyscale, 200, 255, Imgproc.THRESH_BINARY);

        Bitmap sourceThreshold = OpenCVUtils.createBitmap(greyscale);
        sourceImages.add(sourceThreshold);

        //Invert the colors (because objects are represented as white pixels, and the background is represented by black pixels)
        Core.bitwise_not(greyscale, greyscale);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        Bitmap sourceElement = OpenCVUtils.createBitmap(element);
        sourceImages.add(sourceElement);

        //We can now perform our erosion, we must declare our rectangle-shaped structuring element and call the erode function
        Imgproc.erode(greyscale, greyscale, element);

        Bitmap sourceErodedGreyscale = OpenCVUtils.createBitmap(greyscale);
        sourceImages.add(sourceErodedGreyscale);

        //Find all white pixels
        Mat wLocMat = Mat.zeros(greyscale.size(), greyscale.type());
        Core.findNonZero(greyscale, wLocMat);

        Bitmap sourceLockMat = OpenCVUtils.createBitmap(greyscale);
        sourceImages.add(sourceLockMat);

        //Create an empty Mat and pass it to the function
        MatOfPoint matOfPoint = new MatOfPoint(wLocMat);

        //Translate MatOfPoint to MatOfPoint2f in order to user at a next step
        MatOfPoint2f mat2f = new MatOfPoint2f();
        matOfPoint.convertTo(mat2f, CvType.CV_32FC2);

        try {
            //Get rotated rect of white pixels
            RotatedRect rotatedRect = Imgproc.minAreaRect(mat2f);

            Point[] vertices = new Point[4];
            rotatedRect.points(vertices);
            List<MatOfPoint> boxContours = new ArrayList<>();
            boxContours.add(new MatOfPoint(vertices));
            Imgproc.drawContours(greyscale, boxContours, 0, new Scalar(128, 128, 128), -1);

            Bitmap sourceGreyscale = OpenCVUtils.createBitmap(greyscale);
            sourceImages.add(sourceGreyscale);

            // always landscape orientation!
            rotatedRect.angle = rotatedRect.angle < -45 ? rotatedRect.angle + 90.f : rotatedRect.angle;
            double resultAngle = rotatedRect.angle;

            Mat result = deskew(source, resultAngle);

            Bitmap bitmap = OpenCVUtils.createBitmap(result);
            sourceImages.add(bitmap);

            uploadBitmapsToGoogleDrive(new ArrayList<>(), sourceImages);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return sourceImage;
        }
    }

    public Mat deskew(Mat src, double angle) {
        Point center = new Point(src.width() / 2, src.height() / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        //1.0 means 100 % scale
        Size size = new Size(src.width(), src.height());
        Imgproc.warpAffine(src, src, rotImage, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
        return src;
    }

    private void resetToInitial() {
        mCropImageView.setVisibility(View.GONE);
        mCropButtonsLayout.setVisibility(View.GONE);

        mOpenCVCameraView.setVisibility(View.VISIBLE);
        mOpenCVCameraView.enableView();

        TestUtils.removeImage();
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped called");
    }

    public void uploadBitmapsToGoogleDrive(List<BitmapRegion> regions, List<Bitmap> sourceBitmaps) {
        String timeStamp = Utils.getCurrentTimestamp();
        folderName = timeStamp + " openCV";

        Task<DriveFolder> folderTask = DriveUtils.createFolder(folderName, mDriveResourceClient);
        folderTask.continueWith(task -> {
            DriveFolder parentFolder = folderTask.getResult();

            for (int i = 0; i < regions.size(); i++) {
                BitmapRegion region = regions.get(i);
                addGoogleDriveImage(region.getBitmap(), "region #" + region.getRegionNumber(), folderName);
            }

            if (sourceBitmaps != null && sourceBitmaps.size() > 0) {
                for (int i = 0; i < sourceBitmaps.size(); i++) {
                    addGoogleDriveImage(sourceBitmaps.get(i), "source image " + i, folderName);
                }
            }

            return null;
        });
    }


    private void addGoogleDriveImage(Bitmap image, String name, String folderName) {
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);

        Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
            MetadataBuffer metadata = folders.getResult();
            DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

            DriveContents contents = createContentsTask.getResult();

            metadata.release();

            return DriveUtils.createImage(contents, image, name, folder, mDriveResourceClient);
        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                });
    }
}