package com.firrael.tracker.openCV;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

    public final static Scalar CONTOUR_COLOR = Scalar.all(100);

    private final static int MAX_OPTIMAL_REGIONS = 100;

    private Handler handler = new Handler();

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

                    if (!isTest) {
                        mOpenCVCameraView.setVisibility(View.VISIBLE);
                        mOpenCVCameraView.enableView();
                    } else {
                        mOpenCVCameraView.setVisibility(View.GONE);
                        handler.post(runTest);
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private boolean isTest;
    private int testPhase = 0;
    private String folderName;
    private String mCurrentTimeStamp;
    private ArrayList<String> similarities = new ArrayList<>();

    private ArrayList<SourceBitmap> mCurrentSourceBitmaps = new ArrayList<>();
    private ArrayList<BitmapRegion> mCurrentBitmapRegions = new ArrayList<>();
    private int optimalRegionsNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_opencv);

        mSubscription = new CompositeSubscription();

        mLanguage = Utils.getLanguage(this);

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
            isTest = true;
        }
    }

    private void initializeTesseract() {
        int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK; // default
        //int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;

        mTesseract = new Tesseract(this, mLanguage, pageSegmentationMode);

        mTesseractCounter = 0;

        mSubscription.add(Observable
                .range(0, Tesseract.WORKER_POOL_SIZE)
                .subscribe(integer -> {
                    Observable<Integer> observable = getInitNewWorkerObservable(integer);
                    mSubscription.add(observable.subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::getCreatedObservable, Throwable::printStackTrace));
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

        if (handler != null) {
            handler.removeCallbacks(runTest);
            handler = null;
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mGrey = inputFrame.gray();
        mRgba = inputFrame.rgba();

        return mRgba;
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped called");
    }

    private void initDetector() {
        // C++: static Ptr_MSER create(int _delta = 5, int _min_area = 60, int _max_area = 14400, double _max_variation = 0.25, double _min_diversity = .2, int _max_evolution = 200, double _area_threshold = 1.01, double _min_margin = 0.003, int _edge_blur_size = 5)

        mDetector = MSER.create();
        mDetector.setDelta(5); // TODO default, find better delta?

        // TODO use all features    mDetector = MSER.create(5, 60, 14400, 0.25,
        //            0.2, 200, 1.01, 0.003, 5);
        //    mDetector.setMinArea(60);
        //    mDetector.setMaxArea(14400);
        //    mDetector.setDelta(5);
        // ???    mDetector.setPass2Only();
    }

    public void onCameraViewStarted(int width, int height) {
        mOpenCVCameraView.initializeCamera();
        mOpenCVCameraView.setFocusMode(FocusCameraView.FOCUS_CONTINUOUS_PICTURE); // Continuous Picture Mode

        mIntermediateMat = new Mat();
        mGrey = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    private void detectRegions(boolean initial) {
        // PERFECT TEST
        /*Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.test_ocr_image);
        Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.test_ocr_image_2);
        Mat sourceMat = imagePostProcessing(OpenCVUtils.createMat(source));
        mSavedSource = OpenCVUtils.createBitmap(sourceMat);*/

        Mat sourceMat = TestUtils.loadImage();

        if (Core.countNonZero(sourceMat) != 0) { // non-empty matrix (and not 0x0 matrix)
            mSavedSource = OpenCVUtils.createBitmap(imagePostProcessing(sourceMat));
        } else {
            Mat mat = mGrey.clone();
            TestUtils.saveImage(mat);
            addSourceBitmap(OpenCVUtils.createSourceBitmap("1RGB source", mRgba.clone()));

            if (initial) {
                Mat tmp = imagePostProcessing(mat);
                mSavedSource = OpenCVUtils.createBitmap(tmp);
            }
        }

        mCurrentNoiseKernel = Kernel.TINY;

        initializeCropUI();
    }

    private void initializeCropUI() {
        mCropButtonsLayout.setVisibility(View.VISIBLE);

        mCropAcceptButton.setOnClickListener(v -> {
            Bitmap croppedBitmap = mCropImageView.getCroppedImage();
            Mat croppedMat = OpenCVUtils.createMat(croppedBitmap);

            if (isTest) {
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

            Bitmap skewedBitmap = textDeskew(croppedMat);
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

    private Mat imagePostProcessing(Mat mat) {
        //Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);
        Imgproc.medianBlur(mat, mat, 3);

        addSourceBitmap(OpenCVUtils.createSourceBitmap("1/3 post median blur", mat));

        Imgproc.adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 5, 4);

        addSourceBitmap(OpenCVUtils.createSourceBitmap("2/3 post adaptive threshold", mat));

        Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_OTSU);

        addSourceBitmap(OpenCVUtils.createSourceBitmap("3/3 post otsu threshold", mat));

        //    Imgproc.adaptiveThreshold(tmp, tmp, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        return mat;
    }

    private void recognizeMat(Mat mat) {
        addSourceBitmap(new SourceBitmap("full image source", mSavedSource)); // full image source

        mIntermediateMat = mat;

        if (mDetector == null) {
            initDetector();
        }

        List<MatOfPoint> contours = OpenCVUtils.detectRegions(mDetector, mat);

        if (isTest &&
                ((contours.size() > optimalRegionsNumber && optimalRegionsNumber != 0) || contours.size() > MAX_OPTIMAL_REGIONS)) {
            resetCurrentDriveData();
            testPhase++;
            handler.post(runTest);
            return;
        }

        List<Bitmap> bitmapsToRecognize = new ArrayList<>();
        Rect rectan3;

        for (int ind = 0; ind < contours.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contours.get(ind));
            // TODO some extra space for text
            rectan3.y -= 5;
            rectan3.height += 10;
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

        addSourceBitmap(OpenCVUtils.createSourceBitmap("1regions", mat)); // full image source

        if (isTest && optimalRegionsNumber == 0) {
            optimalRegionsNumber = bitmapsToRecognize.size();
        }

        processRegions(bitmapsToRecognize);
    }

    public void processRegions(List<Bitmap> bitmapRegions) {
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
            addRegionBitmaps(regions);
            uploadAllBitmaps(); // uploads all collected region bitmaps + source bitmaps together
            emitter.onCompleted();
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                        }, Throwable::printStackTrace,
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
                                tesseractRecognitionCompleted(results);
                            }
                        }, Throwable::printStackTrace));
    }

    private void tesseractRecognitionCompleted(List<RecognizedRegion> s) {
        Collections.sort(s);
        Log.i(TAG, "Results: " + s);
        Intent data = new Intent();
        ArrayList<RecognizedRegion> results = new ArrayList<>(s);
        data.putParcelableArrayListExtra(KEY_SCAN_RESULTS, results);
        setResult(RESULT_OK, data);

        resetCurrentDriveData();

        showResultsDialog(results);
    }

    private void resetCurrentDriveData() {
        mCurrentTimeStamp = null;
        mCurrentBitmapRegions = new ArrayList<>();
        mCurrentSourceBitmaps = new ArrayList<>();
    }

    private void showResultsDialog(ArrayList<RecognizedRegion> results) {
        if (isTest) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                RecognizedRegion line = results.get(i);
                builder.append(line);
            }

            uploadDiffToGoogleDrive(builder.toString(), folderName, "diff");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            RecognizedRegion line = results.get(i);
            builder.append("region#");
            builder.append(line.getRegionNumber());
            builder.append(": ");
            builder.append(line);
            builder.append("\n");
        }

        uploadResultsToGoogleDrive(builder.toString(), folderName, "1results");

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

    private void uploadResultsToGoogleDrive(String results, String folderName, String fileName) {
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);

        Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
            MetadataBuffer metadata = folders.getResult();
            DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

            DriveContents contents = createContentsTask.getResult();

            metadata.release();

            return DriveUtils.createText(contents, results, fileName, folder, mDriveResourceClient);
        })
                .addOnFailureListener(this, e -> Log.e(TAG, "Unable to create file", e))
                .addOnCompleteListener(this, task -> Log.i(TAG, "OCR results uploaded."));
    }

    private void resetToSourceImage() {
        detectRegions(false);
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

    private Bitmap textDeskew(Mat mat) {
        Bitmap sourceImage = OpenCVUtils.createBitmap(mat);
        addSourceBitmap(new SourceBitmap("cropped pre-deskew image", sourceImage));

        Mat source = mat.clone();

        //Binarize it
        //Use adaptive threshold if necessary
        Imgproc.adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        //Imgproc.threshold(greyscale, greyscale, 200, 255, Imgproc.THRESH_BINARY);

        addSourceBitmap(OpenCVUtils.createSourceBitmap("deskew 1/5 ", mat));

        //Invert the colors (because objects are represented as white pixels, and the background is represented by black pixels)
        Core.bitwise_not(mat, mat);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        addSourceBitmap(OpenCVUtils.createSourceBitmap("deskew 2/5", mat));

        //We can now perform our erosion, we must declare our rectangle-shaped structuring element and call the erode function
        Imgproc.erode(mat, mat, element);

        addSourceBitmap(OpenCVUtils.createSourceBitmap("deskew 3/5", mat));

        //Find all white pixels
        Mat wLocMat = Mat.zeros(mat.size(), mat.type());
        Core.findNonZero(mat, wLocMat);

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
            Imgproc.drawContours(mat, boxContours, 0, new Scalar(128, 128, 128), -1);

            addSourceBitmap(OpenCVUtils.createSourceBitmap("deskew 4/5", mat));

            // always landscape orientation!
            rotatedRect.angle = rotatedRect.angle < -45 ? rotatedRect.angle + 90.f : rotatedRect.angle;
            double resultAngle = rotatedRect.angle;

            Mat result = OpenCVUtils.deskew(source, resultAngle);

            Bitmap bitmap = OpenCVUtils.createBitmap(result);

            addSourceBitmap(new SourceBitmap("deskew 5/5", bitmap));

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return sourceImage;
        }
    }

    private void resetToInitial() {
        mCropImageView.setVisibility(View.GONE);
        mCropButtonsLayout.setVisibility(View.GONE);

        mOpenCVCameraView.setVisibility(View.VISIBLE);
        mOpenCVCameraView.enableView();

        TestUtils.removeImage();

        resetCurrentDriveData();
    }

    public void addSourceBitmap(SourceBitmap sourceBitmap) {
        if (sourceBitmap != null) {
            mCurrentSourceBitmaps.add(sourceBitmap);
        }
    }

    public void addRegionBitmaps(List<BitmapRegion> regions) {
        if (regions != null && regions.size() > 0) {
            mCurrentBitmapRegions.addAll(regions);
        }
    }

    public void uploadAllBitmaps() {
        if (TextUtils.isEmpty(mCurrentTimeStamp)) {
            mCurrentTimeStamp = Utils.getCurrentTimestamp();

            if (isTest) {
                folderName = getTestFolderPrefix() + " " + mCurrentTimeStamp + " openCV";
            } else {
                folderName = mCurrentTimeStamp + " openCV";
            }

            Task<DriveFolder> folderTask = DriveUtils.createFolder(folderName, mDriveResourceClient);
            folderTask.continueWith(task -> {
                DriveFolder parentFolder = folderTask.getResult();

                for (int i = 0; i < mCurrentBitmapRegions.size(); i++) {
                    BitmapRegion region = mCurrentBitmapRegions.get(i);
                    uploadGoogleDriveImage(region.getBitmap(), "region #" + region.getRegionNumber(), folderName);
                }

                if (mCurrentSourceBitmaps != null && mCurrentSourceBitmaps.size() > 0) {
                    for (int i = 0; i < mCurrentSourceBitmaps.size(); i++) {
                        SourceBitmap source = mCurrentSourceBitmaps.get(i);
                        uploadGoogleDriveImage(source.getBitmap(), source.getName(), folderName);
                    }
                }

                return null;
            });
        }
    }

    private void uploadGoogleDriveImage(Bitmap image, String name, String folderName) {
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);

        Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
            MetadataBuffer metadata = folders.getResult();
            DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

            DriveContents contents = createContentsTask.getResult();

            metadata.release();

            return DriveUtils.createImage(contents, image, name, folder, mDriveResourceClient);
        })
                .addOnFailureListener(this, e -> Log.e(TAG, "Unable to create file", e));
    }

    private void test() {
        int resourceId = R.drawable.ocr_test_3; //R.drawable.test_ocr_image;
        Mat sourceMat = null;
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resourceId);

            sourceMat = OpenCVUtils.createMat(bmp);

            if (Core.countNonZero(sourceMat) != 0) { // non-empty matrix (and not 0x0 matrix)
                mSavedSource = OpenCVUtils.createBitmap(imagePostProcessing(sourceMat));

                sourceMat = clearNoise(sourceMat);

                sourceMat = OpenCVUtils.createMat(textDeskew(sourceMat));
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        switch (testPhase) {
            case 0:
                Snackbar.make(mCropImageView, getMessageForTestStep(0), Snackbar.LENGTH_SHORT).show();
                if (sourceMat != null) {
                    recognizeMat(sourceMat);
                }
                break;
            case 1:
                Snackbar.make(mCropImageView, getMessageForTestStep(1), Snackbar.LENGTH_SHORT).show();
                sourceMat = OpenCVUtils.dilate(sourceMat);
                recognizeMat(sourceMat);
                break;
            case 2:
                Snackbar.make(mCropImageView, getMessageForTestStep(2), Snackbar.LENGTH_SHORT).show();
                sourceMat = OpenCVUtils.erode(sourceMat);
                recognizeMat(sourceMat);
                break;
            case 3:
                Snackbar.make(mCropImageView, getMessageForTestStep(3), Snackbar.LENGTH_SHORT).show();
                sourceMat = OpenCVUtils.close(sourceMat);
                recognizeMat(sourceMat);
                break;
            case 4:
                Snackbar.make(mCropImageView, getMessageForTestStep(4), Snackbar.LENGTH_SHORT).show();
                sourceMat = OpenCVUtils.open(sourceMat);
                recognizeMat(sourceMat);
                break;
            case 5:
                Snackbar.make(mCropImageView, getMessageForTestStep(5), Snackbar.LENGTH_SHORT).show();
                uploadSimilaritiesResults();
                break;
            default:
                break;
        }
    }

    private String getMessageForTestStep(int step) {
        return "Test step " + step + ": " + getTestFolderPrefix();
    }

    private String getTestFolderPrefix() {
        switch (testPhase) {
            case 0:
                return "base";
            case 1:
                return "dilate";
            case 2:
                return "erode";
            case 3:
                return "close";
            case 4:
                return "open";
            case 5:
                return "results";
            default:
                return "error";
        }
    }

    private void uploadSimilaritiesResults() {
        if (TextUtils.isEmpty(mCurrentTimeStamp)) {
            mCurrentTimeStamp = Utils.getCurrentTimestamp();

            folderName = getTestFolderPrefix() + mCurrentTimeStamp + " openCV";

            Task<DriveFolder> folderTask = DriveUtils.createFolder(folderName, mDriveResourceClient);
            folderTask.continueWith(task -> {
                DriveFolder parentFolder = folderTask.getResult();

                final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
                Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);

                Tasks.whenAll(folders, createContentsTask).continueWithTask(task2 -> {
                    MetadataBuffer metadata = folders.getResult();
                    DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

                    DriveContents contents = createContentsTask.getResult();

                    metadata.release();

                    StringBuilder builder = new StringBuilder();
                    for (String s : similarities) {
                        builder.append(s);
                        builder.append("\n");
                    }


                    return DriveUtils.createText(contents, builder.toString(), "results", folder, mDriveResourceClient);
                }).addOnCompleteListener(this, task1 -> {
                    Log.i(TAG, "Finished similarity results upload.");
                    finish();
                }).addOnFailureListener(this, e -> Log.e(TAG, "Unable to create file", e));

                return null;
            });
        }
    }

    private void uploadDiffToGoogleDrive(String recognizedText, String folderName, String fileName) {
        String sourceText = TestUtils.readTextFile(this, "test_ocr_text_2");

        Observable.create((Action1<Emitter<String>>) emitter -> {
            try {
                String similarity = ParallelDotsHelper.findDiff(this, sourceText, recognizedText, fileName, folderName, mDriveResourceClient);
                if (!TextUtils.isEmpty(similarity)) {
                    similarities.add(similarity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            emitter.onCompleted();
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                        }, Throwable::printStackTrace,
                        () -> {
                            Log.i(TAG, "Similarity uploaded to Google Drive.");
                            testPhase++;
                            handler.post(runTest);
                        });
    }

    Runnable runTest = this::test;

}